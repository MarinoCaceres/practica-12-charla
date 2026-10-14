package mx.tec.charla.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import mx.tec.charla.domain.Mensaje
import mx.tec.charla.ui.theme.CharlaTema

/**
 * Un mensaje. Los tuyos a la derecha y en el color de la marca; los de los
 * demás a la izquierda, con su avatar.
 *
 * La esquina pequeña apunta hacia quien habla: es lo que hace que un
 * rectángulo redondeado se lea como globo de diálogo.
 */
@Composable
fun Burbuja(mensaje: Mensaje, mia: Boolean, conNombre: Boolean, servidor: String, token: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val e = CharlaTema.espaciado
    val forma = if (mia) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (mia) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!mia) {
            if (conNombre) Avatar(mensaje.de) else Spacer(Modifier.width(32.dp))
            Spacer(Modifier.width(e.sm))
        }
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(if (mia) c.primary else c.surfaceContainerHigh, forma)
                .padding(horizontal = e.md + 2.dp, vertical = e.sm + 2.dp)
        ) {
            if (!mia && conNombre) {
                Text(mensaje.de, style = MaterialTheme.typography.labelMedium, color = c.primary)
            }
            if (mensaje.texto.startsWith("/image:")) {
                val imageId = mensaje.texto.substringAfter("/image:")
                val imageUrl = "$servidor/image/$imageId"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .httpHeaders(NetworkHeaders.Builder().set("Authorization", "Bearer $token").build())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Downloaded JPEG from FastAPI",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = mensaje.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (mia) c.onPrimary else c.onSurface
                )
            }
            Text(
                text = horaDe(mensaje.en),
                style = MaterialTheme.typography.labelSmall,
                color = if (mia) c.onPrimary.copy(alpha = 0.75f) else c.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/** Lo que dice la sala misma («dani entró a la sala»): al centro, sin globo. */
@Composable
fun AvisoDeSala(texto: String, modifier: Modifier = Modifier) {
    val e = CharlaTema.espaciado
    Box(modifier.fillMaxWidth().padding(vertical = e.xs), contentAlignment = Alignment.Center) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .padding(horizontal = e.md, vertical = e.xs + 2.dp)
        )
    }
}
