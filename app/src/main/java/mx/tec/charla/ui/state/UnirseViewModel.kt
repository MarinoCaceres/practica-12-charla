package mx.tec.charla.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.tec.charla.data.local.SalaStore
import mx.tec.charla.data.qr.LectorQr
import mx.tec.charla.data.remote.NuevaSolicitud
import mx.tec.charla.data.remote.SalaApi
import mx.tec.charla.domain.Enlace
import retrofit2.HttpException

@HiltViewModel
class UnirseViewModel @Inject constructor(
    private val api: SalaApi,
    private val store: SalaStore,
    private val lector: LectorQr
) : ViewModel() {

    private val _ui = MutableStateFlow(UnirseUiState())
    val ui: StateFlow<UnirseUiState> = _ui.asStateFlow()

    private var pidiendo: Job? = null

    /** Las tres vías terminan aquí: un texto que debería traer la dirección de una sala. */
    fun usarEnlace(texto: String) {
        val servidor = Enlace.servidorDesde(texto)
        _ui.update {
            if (servidor == null) it.copy(etapa = Etapa.Error("Eso no parece el enlace de una sala"))
            else it.copy(servidor = servidor, etapa = Etapa.Eligiendo)
        }
    }

    fun escanear() {
        viewModelScope.launch {
            try {
                lector.escanear()?.let(::usarEnlace)
            } catch (e: Exception) {
                _ui.update { it.copy(etapa = Etapa.Error("No se pudo abrir el escáner: ${e.message}")) }
            }
        }
    }

    fun leerImagen(imagen: Uri) {
        viewModelScope.launch {
            val texto = try {
                lector.leerDeImagen(imagen)
            } catch (e: IOException) {
                null
            }
            if (texto == null) _ui.update { it.copy(etapa = Etapa.Error("No encontré un QR en esa imagen")) }
            else usarEnlace(texto)
        }
    }

    fun cambiarNickname(nickname: String) = _ui.update { it.copy(nickname = nickname) }

    fun pedirEntrar() {
        val servidor = _ui.value.servidor ?: return
        val nickname = _ui.value.nickname.trim()
        pidiendo = viewModelScope.launch {
            _ui.update { it.copy(etapa = Etapa.Enviando) }
            try {
                var solicitud = api.solicitar("$servidor/solicitudes", NuevaSolicitud(nickname))
                _ui.update { it.copy(etapa = Etapa.Esperando) }
                // Todavía no hay token para el WebSocket: se pregunta por HTTP cada 2 s.
                // Si sales de la pantalla, viewModelScope cancela este ciclo.
                while (solicitud.estado == "pendiente") {
                    delay(2_000)
                    solicitud = api.consultar("$servidor/solicitudes/${solicitud.id}")
                }
                val token = solicitud.token
                if (solicitud.estado == "aprobada" && token != null) {
                    store.entrar(servidor, token, solicitud.nickname)
                    _ui.update { it.copy(etapa = Etapa.Dentro) }
                } else {
                    _ui.update { it.copy(etapa = Etapa.Error("El anfitrión no te dejó entrar")) }
                }
            } catch (e: IOException) {
                _ui.update { it.copy(etapa = Etapa.Error(mensajeDe(e))) }
            } catch (e: HttpException) {
                _ui.update { it.copy(etapa = Etapa.Error(mensajeDe(e))) }
            }
        }
    }

    fun cancelar() {
        pidiendo?.cancel()
        _ui.update { it.copy(etapa = Etapa.Eligiendo) }
    }

    /** Elegir otra sala: se olvida la que estaba elegida. */
    fun otraSala() {
        pidiendo?.cancel()
        _ui.update { UnirseUiState(nickname = it.nickname) }
    }
}