package mx.tec.charla.domain

/**
 * La dirección de una sala, venga de donde venga: un QR, una captura, o un
 * texto que alguien pegó («Entra a mi sala de Charla: https://…»).
 *
 * Se queda con lo primero que parezca `http(s)://host[:puerto]` y tira el
 * resto (una ruta, una diagonal al final, el texto alrededor). Si no hay nada
 * así, regresa null: mejor decir «eso no es un enlace» que conectarse a basura.
 */
object Enlace {
    private val DIRECCION = Regex("""https?://[A-Za-z0-9.-]+(:\d{1,5})?""")

    fun servidorDesde(texto: String): String? = DIRECCION.find(texto)?.value
}