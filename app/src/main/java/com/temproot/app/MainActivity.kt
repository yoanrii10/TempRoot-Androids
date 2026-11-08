package com.temproot.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rootStatus: TextView
    private lateinit var details: TextView
    private lateinit var checkButton: Button

    private val bg = 0xFF0B1510.toInt()
    private val panel = 0xFF14251B.toInt()
    private val green = 0xFF55E895.toInt()
    private val white = 0xFFF2F7F3.toInt()
    private val muted = 0xFFB3C7BA.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        refreshDeviceInfo()
        checkRoot()
    }

    private fun buildScreen() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "TEMP ROOT"
            textSize = 29f
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
        }
        page.addView(title)
        page.addView(label("ADMINISTRADOR DE ACCESO ROOT", 13f, muted, true))
        page.addView(space(18))

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(panel)
        }
        statusCard.addView(label("ESTADO DEL SUPERUSUARIO", 12f, muted, true))
        rootStatus = label("Comprobando…", 22f, white, true)
        statusCard.addView(rootStatus)
        details = label("La comprobación solicitará permiso a la app de superusuario si está instalada.", 13f, muted)
        statusCard.addView(details)
        page.addView(statusCard)
        page.addView(space(14))

        checkButton = Button(this).apply {
            text = "COMPROBAR / SOLICITAR ROOT"
            setOnClickListener { checkRoot() }
        }
        page.addView(checkButton, matchWidth())
        val magiskButton = Button(this).apply {
            text = "ABRIR MAGISK"
            setOnClickListener { openMagisk() }
        }
        page.addView(magiskButton, matchWidth())
        page.addView(space(18))
        page.addView(label("INFORMACIÓN DEL DISPOSITIVO", 14f, green, true))
        page.addView(space(6))
        page.addView(label(deviceSummary(), 14f, white))
        page.addView(space(18))
        page.addView(label("CÓMO FUNCIONA", 14f, green, true))
        page.addView(label(
            "TempRoot no explota vulnerabilidades ni desbloquea el teléfono por sí sola. " +
            "Comprueba si ya existe un gestor ROOT autorizado (por ejemplo, Magisk) y puede pedirle una verificación. " +
            "Si el dispositivo no está rooteado, primero debes seguir el procedimiento oficial y específico de tu modelo.",
            14f, muted
        ))
        page.addView(space(12))
        page.addView(label(
            "Compatibilidad: Android 10 o posterior. La comprobación funciona en distintas marcas; " +
            "la posibilidad de obtener ROOT depende del modelo, firmware, bootloader y método disponible.",
            13f, 0xFFFFD18A.toInt()
        ))

        val scroll = ScrollView(this).apply { addView(page) }
        setContentView(scroll)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 5, 0, 5)
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, (height * resources.displayMetrics.density).toInt())
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun deviceSummary(): String = buildString {
        appendLine("Marca: ${Build.MANUFACTURER}")
        appendLine("Modelo: ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Arquitectura: ${Build.SUPPORTED_ABIS.joinToString()}")
        append("Compilación: ${Build.DISPLAY}")
    }

    private fun refreshDeviceInfo() {
        // Device information is displayed directly in the screen; no data is uploaded.
    }

    private fun checkRoot() {
        checkButton.isEnabled = false
        rootStatus.text = "Solicitando comprobación…"
        details.text = "Si tienes Magisk u otro gestor compatible, acepta la solicitud que aparezca en pantalla."
        executor.execute {
            val result = runSuCheck()
            runOnUiThread {
                checkButton.isEnabled = true
                when (result.state) {
                    RootState.GRANTED -> {
                        rootStatus.text = "ROOT ACTIVO"
                        rootStatus.setTextColor(green)
                        details.text = "Confirmado por el sistema: ${result.output}\nEste permiso puede revocarse desde tu gestor ROOT."
                    }
                    RootState.DENIED -> {
                        rootStatus.text = "ROOT DENEGADO"
                        rootStatus.setTextColor(0xFFFFC56B.toInt())
                        details.text = result.output.ifBlank { "El gestor ROOT rechazó o no concedió el permiso." }
                    }
                    RootState.UNAVAILABLE -> {
                        rootStatus.text = "ROOT NO DISPONIBLE"
                        rootStatus.setTextColor(0xFFFF8B8B.toInt())
                        details.text = result.output.ifBlank { "No se encontró un comando su funcional. El teléfono puede no estar rooteado." }
                    }
                    RootState.ERROR -> {
                        rootStatus.text = "NO SE PUDO VERIFICAR"
                        rootStatus.setTextColor(0xFFFFC56B.toInt())
                        details.text = result.output
                    }
                }
            }
        }
    }

    private fun runSuCheck(): SuResult {
        return try {
            val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val finished = process.waitFor(20, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return SuResult(RootState.ERROR, "La comprobación tardó demasiado. Vuelve a intentarlo y revisa el aviso del gestor ROOT.")
            }
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
            if (process.exitValue() == 0 && output.contains("uid=0")) {
                SuResult(RootState.GRANTED, output)
            } else if (output.isNotBlank()) {
                SuResult(RootState.DENIED, output)
            } else {
                SuResult(RootState.UNAVAILABLE, "No se obtuvo respuesta de su.")
            }
        } catch (_: java.io.IOException) {
            SuResult(RootState.UNAVAILABLE, "No se encontró su. Este dispositivo probablemente no tiene ROOT configurado.")
        } catch (e: Exception) {
            SuResult(RootState.ERROR, "Error de comprobación: ${e.javaClass.simpleName}")
        }
    }

    private fun openMagisk() {
        val intent = packageManager.getLaunchIntentForPackage("com.topjohnwu.magisk")
        if (intent != null) {
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "No se pudo abrir Magisk.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Magisk no está instalado. TempRoot no lo instala ni rootea el teléfono.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}

private enum class RootState { GRANTED, DENIED, UNAVAILABLE, ERROR }
private data class SuResult(val state: RootState, val output: String)
