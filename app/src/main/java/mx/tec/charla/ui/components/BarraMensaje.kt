package mx.tec.charla.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import mx.tec.charla.R
import mx.tec.charla.ui.theme.CharlaTema

/** Donde se escribe. Sin conexión no se puede enviar: el campo lo dice en vez de tragarse el mensaje. */
@Composable
fun BarraMensaje(
    texto: String,
    onTexto: (String) -> Unit,
    onEnviar: () -> Unit,
    onTomarFoto: () -> Unit,
    onElegirGaleria: () -> Unit,
    habilitada: Boolean,
    modifier: Modifier = Modifier
) {
    val e = CharlaTema.espaciado
    val puedeEnviar = habilitada && texto.isNotBlank()
    var menuExpandido by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = e.md, vertical = e.sm),
            horizontalArrangement = Arrangement.spacedBy(e.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = texto,
                onValueChange = onTexto,
                modifier = Modifier.weight(1f),
                enabled = habilitada,
                placeholder = { Text(if (habilitada) "Escribe un mensaje" else "Esperando conexión…") },
                shape = CircleShape,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (puedeEnviar) onEnviar() }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
            Box {
                FilledIconButton(
                    onClick = { menuExpandido = true },
                    enabled = habilitada
                ) {
                    Icon(painterResource(R.drawable.ic_imagen), contentDescription = "Enviar imagen")
                }
                DropdownMenu(
                    expanded = menuExpandido,
                    onDismissRequest = { menuExpandido = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tomar foto con la cámara") },
                        onClick = {
                            menuExpandido = false
                            onTomarFoto()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_imagen), contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Enviar imagen de la galería") },
                        onClick = {
                            menuExpandido = false
                            onElegirGaleria()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_imagen), contentDescription = null)
                        }
                    )
                }
            }
            FilledIconButton(onClick = onEnviar, enabled = puedeEnviar) {
                Icon(painterResource(R.drawable.ic_enviar), contentDescription = "Enviar")
            }
        }
    }
}
