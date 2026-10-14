package mx.tec.charla.ui.state

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

@HiltViewModel
class SalaViewModel @Inject constructor(
    private val store: SalaStore,
    private val conexion: ChatConexion,
    private val api: SalaApi,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _ui = MutableStateFlow(SalaUiState())
    val ui: StateFlow<SalaUiState> = _ui.asStateFlow()

    private var visible = false

    init {
        viewModelScope.launch {
            store.destino.distinctUntilChanged().collect { destino ->
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

    fun alAparecer() {
        visible = true
        _ui.value.destino?.let { conexion.conectar(it.servidor, it.token) }
    }

    fun alDesaparecer() {
        visible = false
        conexion.desconectar()
    }

    private fun recibir(evento: EventoServidor) {
        when (evento) {
            is EventoServidor.Bienvenida -> _ui.update {
                it.copy(mensajes = evento.mensajes, solicitudes = evento.solicitudes)
            }
            is EventoServidor.Nuevo -> _ui.update { it.copy(mensajes = it.mensajes + evento.aMensaje()) }
            is EventoServidor.NuevaSolicitud -> _ui.update { it.copy(solicitudes = it.solicitudes + evento.aSolicitud()) }
        }
    }

    fun enviar(texto: String): Boolean = texto.isNotBlank() && conexion.enviar(Orden.Enviar(texto.trim()))

    fun enviarImagenDesdeUri(uri: Uri) {
        val destino = _ui.value.destino ?: return
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: return@launch
                subirYEnviar(bytes, destino.servidor, destino.token)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun enviarImagenDesdeBitmap(bitmap: Bitmap) {
        val destino = _ui.value.destino ?: return
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    stream.toByteArray()
                }
                subirYEnviar(bytes, destino.servidor, destino.token)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private suspend fun subirYEnviar(bytes: ByteArray, servidor: String, token: String) {
        try {
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
            val respuesta = withContext(Dispatchers.IO) {
                api.uploadImage("$servidor/image/", "Bearer $token", part)
            }
            val imageId = respuesta.id
            if (imageId.isNotBlank()) {
                conexion.enviar(Orden.Enviar("/image:$imageId"))
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun aprobar(solicitud: Solicitud) = responder(solicitud, Orden.Aprobar(solicitud.id))

    fun rechazar(solicitud: Solicitud) = responder(solicitud, Orden.Rechazar(solicitud.id))

    private fun responder(solicitud: Solicitud, orden: Orden) {
        if (conexion.enviar(orden)) _ui.update { it.copy(solicitudes = it.solicitudes - solicitud) }
    }

    fun invitar() {
        _ui.update { it.copy(invitacion = InvitacionUi.Cargando) }
        viewModelScope.launch {
            val invitacion = try {
                val url = api.invitacion("${SalaStore.propio.servidor}/invitacion").url
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

    fun reiniciar() {
        viewModelScope.launch { store.reiniciar() }
    }

    override fun onCleared() = conexion.desconectar()
}
