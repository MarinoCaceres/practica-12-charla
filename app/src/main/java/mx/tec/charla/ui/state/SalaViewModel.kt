package mx.tec.charla.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.tec.charla.data.local.SalaStore
import mx.tec.charla.data.qr.GeneradorQr
import mx.tec.charla.data.remote.ChatConexion
import mx.tec.charla.data.remote.EventoServidor
import mx.tec.charla.data.remote.Orden
import mx.tec.charla.data.remote.SalaApi
import mx.tec.charla.domain.Solicitud
import retrofit2.HttpException

@HiltViewModel
class SalaViewModel @Inject constructor(
    private val store: SalaStore,
    private val conexion: ChatConexion,
    private val api: SalaApi
) : ViewModel() {

    private val _ui = MutableStateFlow(SalaUiState())
    val ui: StateFlow<SalaUiState> = _ui.asStateFlow()

    private var visible = false

    init {
        viewModelScope.launch {
            // Cada vez que cambia la sala (entraste a otra, o Reiniciar), se conecta a la nueva.
            store.destino.distinctUntilChanged().collect { destino ->
                // Sala nueva, pantalla en blanco: la historia llega con la bienvenida.
                _ui.update { SalaUiState(destino = destino, conexion = it.conexion) }
                if (visible) conexion.conectar(destino.servidor, destino.token)
            }
        }
        viewModelScope.launch {
            conexion.estado.collect { estado -> _ui.update { it.copy(conexion = estado) } }
        }
        viewModelScope.launch {
            conexion.eventos.collect(::recibir)
        }
    }

    /** La pantalla se ve: a conectarse. Lo llama LifecycleStartEffect. */
    fun alAparecer() {
        visible = true
        _ui.value.destino?.let { conexion.conectar(it.servidor, it.token) }
    }

    /** La app se fue al fondo: se cierra. Un socket abierto sin nadie viendo gasta batería. */
    fun alDesaparecer() {
        visible = false
        conexion.desconectar()
    }

    private fun recibir(evento: EventoServidor) {
        when (evento) {
            // Al conectar (y al reconectar) llega la historia completa: reemplaza, no suma.
            is EventoServidor.Bienvenida -> _ui.update {
                it.copy(mensajes = evento.mensajes, solicitudes = evento.solicitudes)
            }
            is EventoServidor.Nuevo -> _ui.update { it.copy(mensajes = it.mensajes + evento.aMensaje()) }
            is EventoServidor.NuevaSolicitud -> _ui.update { it.copy(solicitudes = it.solicitudes + evento.aSolicitud()) }
        }
    }

    /** true si salió. El mensaje no se pinta aquí: se pinta cuando el servidor lo reparte, a todos igual. */
    fun enviar(texto: String): Boolean = texto.isNotBlank() && conexion.enviar(Orden.Enviar(texto.trim()))

    fun aprobar(solicitud: Solicitud) = responder(solicitud, Orden.Aprobar(solicitud.id))

    fun rechazar(solicitud: Solicitud) = responder(solicitud, Orden.Rechazar(solicitud.id))

    private fun responder(solicitud: Solicitud, orden: Orden) {
        if (conexion.enviar(orden)) _ui.update { it.copy(solicitudes = it.solicitudes - solicitud) }
    }

    /** Pregunta a TU servidor su dirección pública y la convierte en QR. */
    fun invitar() {
        _ui.update { it.copy(invitacion = InvitacionUi.Cargando) }
        viewModelScope.launch {
            val invitacion = try {
                val url = api.invitacion("${SalaStore.propio.servidor}/invitacion").url
                // Dibujar el QR son medio millón de pixeles: fuera del hilo principal.
                InvitacionUi.Lista(url, withContext(Dispatchers.Default) { GeneradorQr.bitmap(url) })
            } catch (e: IOException) {
                InvitacionUi.Error(mensajeDe(e))
            } catch (e: HttpException) {
                InvitacionUi.Error(mensajeDe(e))
            }
            _ui.update { it.copy(invitacion = invitacion) }
        }
    }

    fun cerrarInvitacion() = _ui.update { it.copy(invitacion = InvitacionUi.Cerrada) }

    /** Olvida la sala ajena. `store.destino` cambia, y el `collect` de arriba reconecta solo. */
    fun reiniciar() {
        viewModelScope.launch { store.reiniciar() }
    }

    override fun onCleared() = conexion.desconectar()
}