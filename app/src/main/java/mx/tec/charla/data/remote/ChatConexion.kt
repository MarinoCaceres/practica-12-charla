package mx.tec.charla.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.tec.charla.domain.EstadoConexion
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * El WebSocket de la sala. Una sola conexión a la vez para toda la app.
 *
 * Hacia afuera son dos Flows: `estado` (cómo está la conexión) y `eventos`
 * (lo que dice el servidor). Hacia adentro, OkHttp avisa con callbacks en su
 * propio hilo; aquí se traducen a esos Flows.
 */
@Singleton
class ChatConexion @Inject constructor(private val cliente: OkHttpClient) {

    private val _estado = MutableStateFlow<EstadoConexion>(EstadoConexion.Desconectado)
    val estado: StateFlow<EstadoConexion> = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoServidor>(extraBufferCapacity = 64)
    val eventos: SharedFlow<EventoServidor> = _eventos.asSharedFlow()

    // @Volatile: OkHttp llama a los callbacks desde su propio hilo, y tienen que ver el valor más reciente.
    @Volatile private var actual: WebSocket? = null

    fun conectar(servidor: String, token: String) {
        desconectar()
        // OkHttp acepta la dirección http(s): un WebSocket empieza como una
        // petición HTTP normal que pide «Upgrade: websocket».
        val peticion = Request.Builder()
            .url("$servidor/chat")
            .header("Authorization", "Bearer $token")
            .build()
        _estado.value = EstadoConexion.Conectando
        actual = cliente.newWebSocket(peticion, Escucha())
    }

    fun desconectar() {
        actual?.close(1000, null)
        actual = null
        _estado.value = EstadoConexion.Desconectado
    }

    /** false si no hay conexión: el mensaje no salió. */
    fun enviar(orden: Orden): Boolean = actual?.send(Protocolo.escribir(orden)) ?: false

    private inner class Escucha : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            _estado.value = EstadoConexion.Conectado
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Protocolo.leer(text)?.let { _eventos.tryEmit(it) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            // El servidor se despide (por ejemplo, porque se reinicia): se le contesta igual.
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _estado.value = EstadoConexion.Desconectado
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // 403: el servidor no reconoce el token. Cualquier otra cosa: se cayó la red o el servidor.
            _estado.value = if (response?.code == 403) EstadoConexion.Rechazado else EstadoConexion.Desconectado
        }
    }
}