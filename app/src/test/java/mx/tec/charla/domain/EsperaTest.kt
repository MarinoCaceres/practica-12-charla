package mx.tec.charla.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EsperaTest {

    @Test
    fun la_espera_se_duplica_en_cada_intento() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L), (1..5).map(::esperaAntesDeReintentar))
    }

    @Test
    fun nunca_pasa_de_30_segundos() {
        assertEquals(30_000L, esperaAntesDeReintentar(6))
        assertEquals(30_000L, esperaAntesDeReintentar(500))
    }
}