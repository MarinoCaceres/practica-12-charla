package mx.tec.charla.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Dibuja un QR con ZXing: primero una matriz de sí/no (módulo negro o
 * blanco), y después un Bitmap con un pixel por casilla.
 *
 * Se pasa el arreglo completo de una vez (`setPixels`): pixel por pixel con
 * `setPixel` serían 600 000 llamadas.
 */
object GeneradorQr {
    fun bitmap(texto: String, lado: Int = 768): Bitmap {
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, lado, lado, mapOf(EncodeHintType.MARGIN to 1))
        val pixeles = IntArray(lado * lado) { i ->
            if (matriz[i % lado, i / lado]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(lado, lado, Bitmap.Config.RGB_565).apply {
            setPixels(pixeles, 0, lado, 0, 0, lado, lado)
        }
    }
}