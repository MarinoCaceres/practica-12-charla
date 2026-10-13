package mx.tec.charla.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.tec.charla.BuildConfig
import mx.tec.charla.data.remote.ChatConexion
import mx.tec.charla.data.remote.EventoServidor
import mx.tec.charla.data.remote.Orden
import mx.tec.charla.domain.ANFITRION
import mx.tec.charla.domain.Destino
import mx.tec.charla.domain.Solicitud

@HiltViewModel
class SalaViewModel @Inject constructor(private val conexion: ChatConexion) : ViewModel() {

    // Por ahora solo existe tu sala, y su dirección sale de local.properties. En el C2 la guarda SalaStore.
    private val destino = Destino(BuildConfig.SERVIDOR_PROPIO, BuildConfig.CLAVE_ANFITRION, ANFITRION, esPropio = true)

    private val _ui = MutableStateFlow(SalaUiState(destino = destino))
    val ui: StateFlow<SalaUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            conexion.estado.collect { estado -> _ui.update { it.copy(conexion = estado) } }
        }
        viewModelScope.launch {
            conexion.eventos.collect(::recibir)
        }
    }

    /** La pantalla se ve: a conectarse. Lo llama LifecycleStartEffect. */
    fun alAparecer() = conexion.conectar(destino.servidor, destino.token)

    /** La app se fue al fondo: se cierra. Un socket abierto sin nadie viendo gasta batería. */
    fun alDesaparecer() = conexion.desconectar()

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

    // La invitación y Reiniciar necesitan la API de la sala y SalaStore: llegan en el C2.
    fun invitar() {}

    fun cerrarInvitacion() {}

    fun reiniciar() {}

    override fun onCleared() = conexion.desconectar()
}