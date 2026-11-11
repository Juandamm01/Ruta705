package com.example.ruta705.ui.components

import androidx.compose.ui.graphics.Color
import com.example.ruta705.ui.theme.TealDark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val colombia = ZoneId.of("America/Bogota")
private val spanish = Locale.forLanguageTag("es-CO")

fun minutesAgoText(minutes: Int): String = when {
    minutes < 1 -> "hace un momento"
    minutes == 1 -> "hace 1 min"
    minutes < 60 -> "hace $minutes min"
    minutes < 120 -> "hace 1 h"
    minutes < 24 * 60 -> "hace ${minutes / 60} h"
    else -> "hace ${minutes / (24 * 60)} d"
}

fun minutesUntil(iso: String): Int? = runCatching {
    ((Instant.parse(iso).toEpochMilli() - System.currentTimeMillis()) / 60_000).toInt()
}.getOrNull()

fun minutesSince(iso: String): Int = runCatching {
    ((System.currentTimeMillis() - Instant.parse(iso).toEpochMilli()) / 60_000).toInt().coerceAtLeast(0)
}.getOrDefault(0)

fun clockText(iso: String): String = runCatching {
    DateTimeFormatter.ofPattern("h:mm a", spanish).withZone(colombia).format(Instant.parse(iso))
        .replace("a. m.", "a.m.").replace("p. m.", "p.m.")
}.getOrDefault("")

fun dateText(iso: String): String = runCatching {
    DateTimeFormatter.ofPattern("d 'de' MMMM, h:mm a", spanish).withZone(colombia).format(Instant.parse(iso))
}.getOrDefault(iso)

fun distanceText(meters: Int): String = when {
    meters < 1000 -> "${(meters / 10.0).roundToInt() * 10} m"
    else -> String.format(spanish, "%.1f km", meters / 1000.0)
}

fun durationText(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

fun copText(value: Int): String = "$" + String.format(spanish, "%,d", value).replace(',', '.')

fun parseHexColor(hex: String?, fallback: Color = TealDark): Color = runCatching {
    if (hex == null) fallback else Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(fallback)

/** Elige texto claro u oscuro según la luminancia del color de fondo. */
fun contentColorFor(background: Color): Color {
    val luminance = 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue
    return if (luminance > 0.6) Color(0xFF16211F) else Color(0xFFF7F4EC)
}

fun greeting(): String {
    val hour = java.time.LocalTime.now(colombia).hour
    return when (hour) {
        in 5..11 -> "Buenos días"
        in 12..17 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}
