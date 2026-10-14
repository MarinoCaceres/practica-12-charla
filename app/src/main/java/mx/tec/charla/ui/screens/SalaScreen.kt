package mx.tec.charla.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import mx.tec.charla.R
import mx.tec.charla.domain.ANFITRION
import mx.tec.charla.domain.Destino
import mx.tec.charla.domain.EstadoConexion
import mx.tec.charla.domain.Mensaje
import mx.tec.charla.domain.SALA
import mx.tec.charla.domain.Solicitud
import mx.tec.charla.ui.components.AvisoDeSala
import mx.tec.charla.ui.components.BarraMensaje
import mx.tec.charla.ui.components.Burbuja
import mx.tec.charla.ui.components.ChipConexion
import mx.tec.charla.ui.components.TarjetaSolicitud
import mx.tec.charla.ui.components.sinEsquema
import mx.tec.charla.ui.state.InvitacionUi
import mx.tec.charla.ui.state.SalaUiState
import mx.tec.charla.ui.theme.CharlaTema
import mx.tec.charla.ui.theme.CharlaTheme

/** La sala: encabezado, quién toca la puerta, los mensajes y dónde escribir. No tiene ViewModel. */
@Composable
fun SalaScreen(
    estado: SalaUiState,
    onEnviar: (String) -> Boolean,
    onTomarFoto: () -> Unit,
    onElegirGaleria: () -> Unit,
    onAprobar: (Solicitud) -> Unit,
    onRechazar: (Solicitud) -> Unit,
    onInvitar: () -> Unit,
    onCerrarInvitacion: () -> Unit,
    onUnirme: () -> Unit,
    onReiniciar: () -> Unit
) {
    var borrador by rememberSaveable { mutableStateOf("") }
    val e = CharlaTema.espaciado
    IconosClarosEnLaBarraDeEstado()
    Scaffold(
        topBar = { Encabezado(estado, onInvitar, onUnirme, onReiniciar) },
        bottomBar = {
            BarraMensaje(
                texto = borrador,
                onTexto = { borrador = it },
                onEnviar = { if (onEnviar(borrador)) borrador = "" },
                onTomarFoto = onTomarFoto,
                onElegirGaleria = onElegirGaleria,
                habilitada = estado.puedeEscribir
            )
        }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            AnimatedVisibility(estado.solicitudes.isNotEmpty()) {
                Column(
                    Modifier.padding(horizontal = e.lg, vertical = e.sm),
                    verticalArrangement = Arrangement.spacedBy(e.sm)
                ) {
                    estado.solicitudes.forEach { s ->
                        TarjetaSolicitud(s, onAceptar = { onAprobar(s) }, onRechazar = { onRechazar(s) })
                    }
                }
            }
            if (estado.conexion == EstadoConexion.Rechazado) AvisoRechazado(onReiniciar)
            if (estado.mensajes.isEmpty()) {
                SalaVacia(esPropio = estado.destino?.esPropio != false, modifier = Modifier.weight(1f))
            } else {
                Mensajes(
                    mensajes = estado.mensajes,
                    yo = estado.destino?.yo.orEmpty(),
                    servidor = estado.destino?.servidor.orEmpty(),
                    token = estado.destino?.token.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    if (estado.invitacion != InvitacionUi.Cerrada) {
        InvitarHoja(estado.invitacion, onReintentar = onInvitar, onCerrar = onCerrarInvitacion)
    }
}

/** El encabezado es verde oscuro: mientras se ve esta pantalla, la hora y la batería van en blanco. */
@Composable
private fun IconosClarosEnLaBarraDeEstado() {
    val vista = LocalView.current
    if (vista.isInEditMode) return
    DisposableEffect(Unit) {
        val barras = WindowCompat.getInsetsController((vista.context as Activity).window, vista)
        val antes = barras.isAppearanceLightStatusBars
        barras.isAppearanceLightStatusBars = false
        onDispose { barras.isAppearanceLightStatusBars = antes }
    }
}

@Composable
private fun Encabezado(estado: SalaUiState, onInvitar: () -> Unit, onUnirme: () -> Unit, onReiniciar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val e = CharlaTema.espaciado
    val destino = estado.destino
    val propio = destino?.esPropio != false
    val botones = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = c.onPrimary.copy(alpha = 0.16f),
        contentColor = c.onPrimary
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(c.primary, c.onPrimaryContainer)),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .statusBarsPadding()
            .padding(start = e.xl, end = e.lg, top = e.md, bottom = e.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Charla", style = MaterialTheme.typography.headlineLarge, color = c.onPrimary)
                Text(
                    text = if (propio) "Tu sala" else "En la sala de ${destino?.let { sinEsquema(it.servidor) }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onPrimary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (propio) {
                FilledTonalIconButton(onClick = onInvitar, colors = botones) {
                    Icon(painterResource(R.drawable.ic_qr), contentDescription = "Invitar a tu sala")
                }
            } else {
                FilledTonalIconButton(onClick = onReiniciar, colors = botones) {
                    Icon(painterResource(R.drawable.ic_casa), contentDescription = "Volver a mi sala")
                }
            }
            IconButton(onClick = onUnirme) {
                Icon(painterResource(R.drawable.ic_puerta), contentDescription = "Entrar a otra sala", tint = c.onPrimary)
            }
        }
        Spacer(Modifier.height(e.md))
        Row(horizontalArrangement = Arrangement.spacedBy(e.sm), verticalAlignment = Alignment.CenterVertically) {
            ChipConexion(estado.conexion)
            if (destino != null && destino.yo != ANFITRION) {
                Text("Eres «${destino.yo}»", style = MaterialTheme.typography.labelMedium, color = c.onPrimary)
            }
        }
    }
}

@Composable
private fun Mensajes(mensajes: List<Mensaje>, yo: String, servidor: String, token: String, modifier: Modifier = Modifier) {
    val lista = rememberLazyListState()
    val e = CharlaTema.espaciado
    // Llega un mensaje: a la vista, abajo.
    LaunchedEffect(mensajes.size) { lista.animateScrollToItem(mensajes.lastIndex) }
    LazyColumn(
        state = lista,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = e.lg, vertical = e.md),
        verticalArrangement = Arrangement.spacedBy(e.xs)
    ) {
        itemsIndexed(mensajes, key = { _, m -> m.id }) { i, m ->
            when (m.de) {
                SALA -> AvisoDeSala(m.texto)
                // El nombre solo en el primero de una racha: tres seguidos de dani son UNA voz.
                else -> Burbuja(
                    mensaje = m,
                    mia = m.de == yo,
                    conNombre = i == 0 || mensajes[i - 1].de != m.de,
                    servidor = servidor,
                    token = token
                )
            }
        }
    }
}

@Composable
private fun SalaVacia(esPropio: Boolean, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val e = CharlaTema.espaciado
    Column(
        modifier = modifier.fillMaxWidth().padding(e.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(104.dp).background(c.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_globos), contentDescription = null, tint = c.onPrimaryContainer, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(e.xl))
        Text("La sala está vacía", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(e.sm))
        Text(
            text = if (esPropio) "Toca el QR de arriba e invita a alguien." else "Saluda: eres el primero en escribir.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AvisoRechazado(onReiniciar: () -> Unit) {
    val e = CharlaTema.espaciado
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = e.lg, vertical = e.sm),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(e.lg), verticalArrangement = Arrangement.spacedBy(e.sm)) {
            Text("Esta sala ya no te reconoce", style = MaterialTheme.typography.titleMedium)
            Text(
                "El anfitrión borró su sala o te sacó. Vuelve a la tuya y pide entrar otra vez.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onReiniciar) { Text("Volver a mi sala") }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun SalaPreview() {
    CharlaTheme {
        SalaScreen(
            estado = SalaUiState(
                destino = Destino("http://10.0.2.2:8000", "", ANFITRION, esPropio = true),
                conexion = EstadoConexion.Conectado,
                mensajes = listOf(
                    Mensaje(1, SALA, "dani entró a la sala", 1_791_400_000.0),
                    Mensaje(2, "dani", "¡Hola! Ya entré", 1_791_400_060.0),
                    Mensaje(3, ANFITRION, "Bienvenida a mi sala", 1_791_400_090.0)
                ),
                solicitudes = listOf(Solicitud("x", "eli"))
            ),
            onEnviar = { true }, onTomarFoto = {}, onElegirGaleria = {}, onAprobar = {}, onRechazar = {}, onInvitar = {},
            onCerrarInvitacion = {}, onUnirme = {}, onReiniciar = {}
        )
    }
}
