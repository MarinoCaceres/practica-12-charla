package mx.tec.charla.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mx.tec.charla.domain.Mensaje
import mx.tec.charla.domain.Solicitud

/**
 * Lo que viaja por el WebSocket. Cada mensaje es un JSON con un campo `tipo`
 * que dice qué es; el resto de los campos depende del tipo.
 *
 * Una interfaz sellada es exactamente eso: un conjunto cerrado de variantes.
 * kotlinx.serialization lee `tipo` y elige la clase por su `@SerialName`.
 */
@Serializable
sealed interface EventoServidor {

    /** Lo primero que llega al conectarse: quién eres, la historia, y (si eres el anfitrión) quién espera. */
    @Serializable
    @SerialName("bienvenida")
    data class Bienvenida(
        val yo: String,
        val mensajes: List<Mensaje>,
        val solicitudes: List<Solicitud>
    ) : EventoServidor

    @Serializable
    @SerialName("mensaje")
    data class Nuevo(val id: Long, val de: String, val texto: String, val en: Double) : EventoServidor {
        fun aMensaje() = Mensaje(id, de, texto, en)
    }

    @Serializable
    @SerialName("solicitud")
    data class NuevaSolicitud(val id: String, val nickname: String) : EventoServidor {
        fun aSolicitud() = Solicitud(id, nickname)
    }
}

/** Lo que la app le manda al servidor. */
@Serializable
sealed interface Orden {
    @Serializable
    @SerialName("mensaje")
    data class Enviar(val texto: String) : Orden

    @Serializable
    @SerialName("aprobar")
    data class Aprobar(val id: String) : Orden

    @Serializable
    @SerialName("rechazar")
    data class Rechazar(val id: String) : Orden
}

object Protocolo {
    private val json = Json {
        classDiscriminator = "tipo"
        ignoreUnknownKeys = true
    }

    /** null si el servidor manda algo que esta versión de la app no conoce: se ignora, no truena. */
    fun leer(texto: String): EventoServidor? =
        runCatching { json.decodeFromString<EventoServidor>(texto) }.getOrNull()

    fun escribir(orden: Orden): String = json.encodeToString(orden)
}