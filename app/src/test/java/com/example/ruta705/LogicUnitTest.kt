package com.example.ruta705

import com.example.ruta705.core.network.AppError
import com.example.ruta705.core.network.parseErrorBody
import com.example.ruta705.core.notifications.NotificationSync
import com.example.ruta705.feature.auth.normalizePhone
import com.example.ruta705.feature.auth.passwordProblems
import com.example.ruta705.ui.components.distanceText
import com.example.ruta705.ui.components.durationText
import com.example.ruta705.ui.components.minutesAgoText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** Pruebas de lógica pura de la app (se ejecutan en el PC, sin emulador). */
class LogicUnitTest {
    @Test
    fun normalizaCelularesColombianos() {
        assertEquals("3105550101", normalizePhone("+57 310 555 0101"))
        assertEquals("3105550101", normalizePhone("310-555-0101"))
        assertEquals("3105550101", normalizePhone("573105550101"))
    }

    @Test
    fun validaContrasenas() {
        assertTrue(passwordProblems("Llanero2026").isEmpty())
        assertEquals(listOf("Al menos 8 caracteres", "Al menos un número"), passwordProblems("abc"))
    }

    @Test
    fun formateaTextosDeTiempoYDistancia() {
        assertEquals("hace un momento", minutesAgoText(0))
        assertEquals("hace 12 min", minutesAgoText(12))
        assertEquals("hace 2 h", minutesAgoText(130))
        assertEquals("350 m", distanceText(347))
        assertEquals("1 h 5 min", durationText(65))
        assertTrue(distanceText(1500).startsWith("1"))
    }

    @Test
    fun interpretaErroresDelBackend() {
        val error = parseErrorBody(
            400,
            """{"error":{"code":"VALIDATION_ERROR","message":"Revisa los datos enviados.","details":[{"path":"email","message":"Correo inválido"}]}}""",
        )
        assertEquals(AppError.Kind.Validation, error.kind)
        assertEquals("Correo inválido", error.fieldErrors["email"])
        assertEquals(AppError.Kind.Unauthorized, parseErrorBody(401, null).kind)
        assertEquals("Inicia sesión para continuar.", parseErrorBody(401, "no-json").message)
    }

    @Test
    fun respetaHorasDeSilencio() {
        val quiet = "22:00" to "06:00"
        assertTrue(NotificationSync.isQuiet(quiet, LocalTime.of(23, 30)))
        assertTrue(NotificationSync.isQuiet(quiet, LocalTime.of(5, 0)))
        assertFalse(NotificationSync.isQuiet(quiet, LocalTime.of(12, 0)))
        assertFalse(NotificationSync.isQuiet(null, LocalTime.of(23, 0)))
    }
}
