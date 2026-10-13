package mx.tec.charla.domain

/**
 * Cuánto esperar antes del intento número `intento` de reconectar:
 * 1 s, 2 s, 4 s, 8 s, 16 s, y de ahí 30 s siempre.
 *
 * Duplicar la espera (backoff exponencial) es cortesía con el servidor: si se
 * cayó, treinta teléfonos tocándole la puerta cada segundo no lo ayudan a
 * levantarse. El tope evita que, tras una caída larga, la app tarde minutos en
 * darse cuenta de que ya volvió.
 */
fun esperaAntesDeReintentar(intento: Int): Long =
    (1_000L shl (intento - 1).coerceIn(0, 5)).coerceAtMost(30_000L)