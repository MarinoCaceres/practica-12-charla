package mx.tec.charla.data.qr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

/**
 * Dos formas de leer un QR, las dos de Google:
 *
 *  - con la cámara: el escáner de Play Services abre SU pantalla, lee el
 *    código y regresa el texto. La app no pide permiso de cámara: la cámara
 *    la usa Google, no tú.
 *  - de una imagen: ML Kit busca códigos en una foto o captura que ya existe.
 */
class LectorQr @Inject constructor(@ApplicationContext private val context: Context) {

    private val soloQr = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()

    /**
     * null si el usuario cerró el escáner sin leer nada. Se escribe con los tres
     * callbacks y no con `await()`: cerrar el escáner CANCELA la tarea, y `await()`
     * lo convertiría en una excepción que cancela también a quien llamó.
     */
    suspend fun escanear(): String? = suspendCancellableCoroutine { continuacion ->
        val opciones = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(context, opciones).startScan()
            .addOnSuccessListener { continuacion.resume(it.rawValue) }
            .addOnCanceledListener { continuacion.resume(null) }
            .addOnFailureListener { continuacion.resumeWithException(it) }
    }

    /** El texto del primer QR de la imagen, o null si no tiene ninguno. */
    suspend fun leerDeImagen(imagen: Uri): String? {
        val entrada = InputImage.fromFilePath(context, imagen)
        val codigos = BarcodeScanning.getClient(soloQr).process(entrada).await()
        return codigos.firstNotNullOfOrNull { it.rawValue }
    }
}