package mx.tec.charla.data.remote

import mx.tec.charla.domain.Mensaje
import mx.tec.charla.domain.Solicitud
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Los JSON de estas pruebas son copia de lo que manda el servidor de la Parte A. */
class ProtocoloTest {

    @Test
    fun la_bienvenida_trae_historia_y_solicitudes() {
        val evento = Protocolo.leer(
            """{"tipo":"bienvenida","yo":"anfitrión",
               "mensajes":[{"id":1,"de":"sala","texto":"dani entró a la sala","en":1791427512.5}],
               "solicitudes":[{"id":"IS_cy","nickname":"eli"}]}"""
        )
        assertEquals(
            EventoServidor.Bienvenida(
                yo = "anfitrión",
                mensajes = listOf(Mensaje(1, "sala", "dani entró a la sala", 1791427512.5)),
                solicitudes = listOf(Solicitud("IS_cy", "eli"))
            ),
            evento
        )
    }

    @Test
    fun un_mensaje_nuevo_se_convierte_en_mensaje() {
        val evento = Protocolo.leer("""{"tipo":"mensaje","id":7,"de":"dani","texto":"hola","en":1.0}""")
        assertEquals(Mensaje(7, "dani", "hola", 1.0), (evento as EventoServidor.Nuevo).aMensaje())
    }

    @Test
    fun lo_que_esta_version_no_conoce_se_ignora() {
        assertNull(Protocolo.leer("""{"tipo":"escribiendo","de":"dani"}"""))
        assertNull(Protocolo.leer("no soy json"))
    }

    @Test
    fun cada_orden_lleva_su_tipo() {
        assertEquals("""{"tipo":"mensaje","texto":"hola"}""", Protocolo.escribir(Orden.Enviar("hola")))
        assertEquals("""{"tipo":"aprobar","id":"IS_cy"}""", Protocolo.escribir(Orden.Aprobar("IS_cy")))
    }
}