package mx.tec.charla.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.tec.charla.domain.EstadoConexion
import mx.tec.charla.domain.esperaAntesDeReintentar
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
class ChatConexion(
    private val cliente: OkHttpClient,
    private val alcance: CoroutineScope
) {
    @Inject
    constructor(cliente: OkHttpClient) : this(cliente, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _estado = MutableStateFlow<EstadoConexion>(EstadoConexion.Desconectado)
    val estado: StateFlow<EstadoConexion> = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoServidor>(extraBufferCapacity = 64)
    val eventos: SharedFlow<EventoServidor> = _eventos.asSharedFlow()

    // @Volatile: OkHttp llama a los callbacks desde su propio hilo, y tienen que ver el valor más reciente.
    @Volatile private var actual: WebSocket? = null
    private var peticion: Request? = null
    private var reintento: Job? = null
    private var intentos = 0

    fun conectar(servidor: String, token: String) {
        desconectar()
        // OkHttp acepta la dirección http(s): un WebSocket empieza como una
        // petición HTTP normal que pide «Upgrade: websocket».
        peticion = Request.Builder()
            .url("$servidor/chat")
            .header("Authorization", "Bearer $token")
            .build()
        abrir()
    }

    fun desconectar() {
        reintento?.cancel()
        peticion = null
        intentos = 0
        actual?.close(1000, null)
        actual = null
        _estado.value = EstadoConexion.Desconectado
    }

    /** false si no hay conexión: el mensaje no salió. */
    fun enviar(orden: Orden): Boolean = actual?.send(Protocolo.escribir(orden)) ?: false

    private fun abrir() {
        val p = peticion ?: return
        _estado.value = EstadoConexion.Conectando
        actual = cliente.newWebSocket(p, Escucha())
    }

    private fun reintentar() {
        intentos++
        val espera = esperaAntesDeReintentar(intentos)
        _estado.value = EstadoConexion.Reconectando(espera / 1_000)
        reintento = alcance.launch {
            delay(espera)
            abrir()
        }
    }

    private inner class Escucha : WebSocketListener() {
        // Cada callback empieza igual: si este socket ya no es el actual, lo
        // que diga ya no importa. Sin esa línea, el cierre de una conexión
        // vieja llega DESPUÉS de que abrió la nueva y la marca «desconectada».

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (webSocket !== actual) return
            intentos = 0
            _estado.value = EstadoConexion.Conectado
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== actual) return
            Protocolo.leer(text)?.let { _eventos.tryEmit(it) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            // El servidor se despide (por ejemplo, porque se reinicia): se le contesta igual.
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            reintentar()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== actual) return
            if (response?.code == 403) {
                // El servidor no reconoce el token. Reintentar no lo va a cambiar.
                actual = null
                _estado.value = EstadoConexion.Rechazado
            } else {
                reintentar()
            }
        }
    }
}