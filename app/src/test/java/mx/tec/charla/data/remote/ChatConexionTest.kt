package mx.tec.charla.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mx.tec.charla.domain.EstadoConexion
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ChatConexion contra salas de mentira que corren en la JVM: MockWebServer
 * habla WebSocket de verdad. Sin emulador y sin Docker.
 */
class ChatConexionTest {

    private val salaA = MockWebServer()
    private val salaB = MockWebServer()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val conexion = ChatConexion(OkHttpClient(), alcance)

    @After
    fun cerrar() {
        conexion.desconectar()
        alcance.cancel()
        salaA.shutdown()
        salaB.shutdown()
    }

    /**
     * Encola una sala que acepta el WebSocket. `alAbrir` decide qué hace el servidor
     * cuando alguien entra; `tardaEnDespedirse`, cuánto tarda en contestar un cierre
     * (en localhost es casi nada; por el túnel, no).
     */
    private fun MockWebServer.sala(tardaEnDespedirse: Long = 0, alAbrir: (WebSocket) -> Unit = {}): String {
        enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = alAbrir(webSocket)
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Thread.sleep(tardaEnDespedirse)
                webSocket.close(1000, null)
            }
        }))
        return url("/").toString().removeSuffix("/")
    }

    private fun esperar(estado: EstadoConexion) {
        runBlocking { withTimeout(5_000) { conexion.estado.first { it == estado } } }
    }

    @Test
    fun manda_el_token_y_recibe_la_bienvenida() {
        val url = salaA.sala { it.send("""{"tipo":"bienvenida","yo":"dani","mensajes":[],"solicitudes":[]}""") }
        // UNDISPATCHED: empieza a escuchar YA, antes de conectar. Si no, la bienvenida podría llegar sin nadie oyendo.
        val bienvenida = runBlocking {
            val primero = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(5_000) { conexion.eventos.first() } }
            conexion.conectar(url, "token-de-dani")
            primero.await()
        }

        assertEquals("dani", (bienvenida as EventoServidor.Bienvenida).yo)
        assertEquals("Bearer token-de-dani", salaA.takeRequest().getHeader("Authorization"))
        esperar(EstadoConexion.Conectado)
    }

    @Test
    fun el_cierre_de_la_conexion_vieja_no_apaga_la_nueva() {
        conexion.conectar(salaA.sala(tardaEnDespedirse = 300), "uno")
        esperar(EstadoConexion.Conectado)
        conexion.conectar(salaB.sala(), "dos")
        esperar(EstadoConexion.Conectado)

        // El cierre de la sala A llega 300 ms DESPUÉS de que abrió la B. Que no cambie nada.
        Thread.sleep(1_000)
        assertEquals(EstadoConexion.Conectado, conexion.estado.value)
    }

    @Test
    fun si_la_sala_no_reconoce_el_token_no_reintenta() {
        salaA.enqueue(MockResponse().setResponseCode(403))
        conexion.conectar(salaA.url("/").toString().removeSuffix("/"), "viejo")
        esperar(EstadoConexion.Rechazado)

        Thread.sleep(1_500)
        assertEquals(EstadoConexion.Rechazado, conexion.estado.value)
        assertEquals(1, salaA.requestCount)
    }

    @Test
    fun si_la_sala_se_cae_reintenta_y_vuelve() {
        // La primera conexión el servidor la corta en cuanto abre (como al reiniciarse); la segunda, no.
        val url = salaA.sala { it.close(1012, "me reinicio") }
        salaA.sala()
        conexion.conectar(url, "t")

        esperar(EstadoConexion.Reconectando(1))
        esperar(EstadoConexion.Conectado)
        assertEquals(2, salaA.requestCount)
    }
}