package com.vigia.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.vigia.app.adb.GestorAdb
import com.vigia.app.integridad.PlanLimpieza
import com.vigia.app.util.Ayuda
import com.vigia.app.util.Formato
import com.vigia.app.util.Portapapeles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Plan de limpieza: las apps elegidas y los comandos de ADB para desactivarlas o quitarlas, con su «deshacer». */
class PlanLimpiezaActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var vacio: TextView
    private lateinit var guion: TextView
    private var texto: String = ""

    private val lanzadorGuardar = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        guardar(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_limpieza)
        Ayuda.instalar(this, R.string.ayuda_plan_titulo, R.string.ayuda_plan)
        contenedor = findViewById(R.id.contenedorPlan)
        vacio = findViewById(R.id.planVacio)
        guion = findViewById(R.id.planGuion)

        findViewById<MaterialButton>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnCopiarPlan).setOnClickListener {
            if (texto.isNotEmpty()) {
                Portapapeles.copiar(this, texto)
                Toast.makeText(this, R.string.copiado, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<MaterialButton>(R.id.btnGuardarPlan).setOnClickListener {
            if (texto.isNotEmpty()) lanzadorGuardar.launch("vigia-plan-limpieza.sh")
        }
        findViewById<MaterialButton>(R.id.btnCompartirPlan).setOnClickListener {
            if (texto.isNotEmpty()) {
                val envio = Intent(Intent.ACTION_SEND)
                envio.type = "text/plain"
                envio.putExtra(Intent.EXTRA_TEXT, texto)
                startActivity(Intent.createChooser(envio, getString(R.string.compartir_plan)))
            }
        }
        findViewById<MaterialButton>(R.id.btnEjecutarAdb).setOnClickListener { ejecutarConAdb() }
        findViewById<MaterialButton>(R.id.btnModoAvanzado).setOnClickListener {
            startActivity(Intent(this, ModoAdbActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnVaciarPlan).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.vaciar_plan)
                .setMessage(R.string.confirmar_vaciar_plan)
                .setPositiveButton(R.string.vaciar_plan) { _, _ ->
                    PlanLimpieza.vaciar(this)
                    refrescar()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        refrescar()
    }

    private fun ejecutarConAdb() {
        val lista = PlanLimpieza.leer(this)
        if (lista.isEmpty()) {
            Toast.makeText(this, R.string.plan_vacio, Toast.LENGTH_LONG).show()
            return
        }
        if (!GestorAdb.disponible()) {
            Toast.makeText(this, R.string.adb_no_disponible, Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            val vivo = GestorAdb.verificar()
            if (!vivo) {
                AlertDialog.Builder(this@PlanLimpiezaActivity)
                    .setTitle(R.string.adb_sin_conexion)
                    .setMessage(R.string.adb_reconectar)
                    .setPositiveButton(R.string.abrir_modo_avanzado) { _, _ ->
                        startActivity(Intent(this@PlanLimpiezaActivity, ModoAdbActivity::class.java))
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                return@launch
            }
            val resumen = lista.joinToString("\n") { e ->
                "• " + e.etiqueta + " → " + (if (e.accion == PlanLimpieza.DESACTIVAR) "desactivar" else "quitar para este usuario")
            }
            AlertDialog.Builder(this@PlanLimpiezaActivity)
                .setTitle(R.string.ejecutar_plan_adb)
                .setMessage(getString(R.string.confirmar_ejecutar_plan, lista.size, resumen))
                .setPositiveButton(R.string.ejecutar) { _, _ -> lanzarPlan(lista) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun lanzarPlan(lista: List<PlanLimpieza.Entrada>) {
        lifecycleScope.launch {
            val informe = StringBuilder()
            for (e in lista) {
                val motivo = PlanLimpieza.motivoCritica(this@PlanLimpiezaActivity, e.paquete)
                if (motivo != null) {
                    informe.append("✗ ").append(e.etiqueta).append(": omitida (").append(motivo).append(")\n")
                    continue
                }
                val orden = if (e.accion == PlanLimpieza.DESACTIVAR) {
                    "pm disable-user --user 0 " + e.paquete
                } else {
                    "pm uninstall --user 0 " + e.paquete
                }
                var salida = ""
                var bien = false
                for (intento in 1..2) {
                    salida = try {
                        GestorAdb.ejecutar(orden)
                    } catch (ex: Exception) {
                        "error: " + ex.javaClass.simpleName + (if (ex.message.isNullOrEmpty()) "" else " (" + ex.message + ")")
                    }
                    delay(500)
                    // El resultado se comprueba en el propio sistema, no solo en lo que imprime la orden.
                    bien = aplicado(e) || salida.contains("Success") || salida.contains("disabled-user")
                    if (bien) break
                }
                if (bien) PlanLimpieza.quitar(this@PlanLimpiezaActivity, e.paquete)
                val detalle = if (salida.isEmpty()) "sin respuesta" else salida.take(160)
                informe.append(if (bien) "✓ " else "✗ ").append(e.etiqueta).append(": ")
                    .append(if (bien) "hecho (" + detalle + ")" else detalle).append('\n')
            }
            refrescar()
            AlertDialog.Builder(this@PlanLimpiezaActivity)
                .setTitle(R.string.resultado_plan)
                .setMessage(informe.toString() + "\n" + getString(R.string.resultado_plan_nota))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    @Suppress("DEPRECATION")
    private fun aplicado(e: PlanLimpieza.Entrada): Boolean {
        val pm = packageManager
        return try {
            if (e.accion == PlanLimpieza.DESACTIVAR) {
                val s = pm.getApplicationEnabledSetting(e.paquete)
                s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER || s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                val info = pm.getApplicationInfo(e.paquete, PackageManager.MATCH_UNINSTALLED_PACKAGES)
                (info.flags and ApplicationInfo.FLAG_INSTALLED) == 0
            }
        } catch (ex: PackageManager.NameNotFoundException) {
            e.accion == PlanLimpieza.DESINSTALAR
        } catch (ex: Exception) {
            false
        }
    }

    private fun refrescar() {
        val lista = PlanLimpieza.leer(this)
        contenedor.removeAllViews()
        vacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        val margen = (8 * resources.displayMetrics.density).toInt()
        for (e in lista) {
            val fila = TextView(this)
            val accion = if (e.accion == PlanLimpieza.DESACTIVAR) "desactivar" else "quitar para este usuario"
            fila.text = e.etiqueta + "\n" + e.paquete + " · " + accion
            fila.textSize = 14f
            fila.setPadding(0, margen, 0, margen)
            fila.setOnClickListener { opciones(e) }
            contenedor.addView(fila)
        }
        texto = if (lista.isEmpty()) "" else PlanLimpieza.guion(lista, Formato.fecha(System.currentTimeMillis()))
        guion.text = texto
    }

    private fun opciones(e: PlanLimpieza.Entrada) {
        val cambio = if (e.accion == PlanLimpieza.DESACTIVAR) "Cambiar a «quitar para este usuario»" else "Cambiar a «desactivar»"
        AlertDialog.Builder(this)
            .setTitle(e.etiqueta)
            .setItems(arrayOf(cambio, "Quitar del plan")) { _, cual ->
                if (cual == 0) {
                    val nueva = if (e.accion == PlanLimpieza.DESACTIVAR) PlanLimpieza.DESINSTALAR else PlanLimpieza.DESACTIVAR
                    PlanLimpieza.anadir(this, e.paquete, e.etiqueta, nueva)
                } else {
                    PlanLimpieza.quitar(this, e.paquete)
                }
                refrescar()
            }
            .show()
    }

    private fun guardar(uri: Uri?) {
        if (uri == null) return
        val bytes = texto.toByteArray(Charsets.UTF_8)
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            Toast.makeText(this@PlanLimpiezaActivity, if (ok) R.string.informe_guardado else R.string.informe_no_guardado, Toast.LENGTH_LONG).show()
        }
    }
}
