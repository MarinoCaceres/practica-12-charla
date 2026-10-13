package mx.tec.charla.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Lo que NO va por el WebSocket: preguntar la invitación y pedir entrar.
 *
 * Cada sala tiene su dirección, así que no hay una `baseUrl` fija: con `@Url`
 * cada llamada recibe la dirección completa. Retrofit ignora su `baseUrl`
 * cuando la de la llamada ya trae `http://`.
 */
interface SalaApi {
    @GET
    suspend fun invitacion(@Url url: String): Invitacion

    @POST
    suspend fun solicitar(@Url url: String, @Body cuerpo: NuevaSolicitud): EstadoSolicitud

    @GET
    suspend fun consultar(@Url url: String): EstadoSolicitud
}

@Serializable
data class Invitacion(val url: String)

@Serializable
data class NuevaSolicitud(val nickname: String)

/** `estado` es pendiente, aprobada o rechazada. El token solo llega cuando la aprueban. */
@Serializable
data class EstadoSolicitud(val id: String, val nickname: String, val estado: String, val token: String? = null)