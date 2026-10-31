package com.vigia.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.vigia.app.adb.GestorAdb
import com.vigia.app.util.Ayuda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modo avanzado: empareja Vigía con la depuración inalámbrica de este teléfono para poder quitar o desactivar apps del
 * sistema (y devolverlas). Necesita Android 11 o superior y una red wifi.
 */
class ModoAdbActivity : AppCompatActivity() {

    private lateinit var estado: TextView
    private lateinit var registro: TextView
    private lateinit var etPuerto: EditText
    private lateinit var etCodigo: EditText
    private lateinit var etPuertoCon: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modo_adb)
        Ayuda.instalar(this, R.string.ayuda_modo_adb_titulo, R.string.ayuda_modo_adb)
        estado = findViewById(R.id.adbEstado)
        registro = findViewById(R.id.adbRegistro)
        etPuerto = findViewById(R.id.etPuertoPar)
        etCodigo = findViewById(R.id.etCodigoPar)
        etPuertoCon = findViewById(R.id.etPuertoCon)

        findViewById<MaterialButton>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnAbrirDesarrollo).setOnClickListener { abrirDesarrollo() }
        findViewById<MaterialButton>(R.id.btnEmparejar).setOnClickListener { emparejar() }
        findViewById<MaterialButton>(R.id.btnConectarAdb).setOnClickListener { conectar() }
        findViewById<MaterialButton>(R.id.btnRestaurar).setOnClickListener { listarRestaurables() }
        findViewById<MaterialButton>(R.id.btnOlvidarAdb).setOnClickListener { olvidar() }
    }

    override fun onResume() {
        super.onResume()
        if (!GestorAdb.disponible()) {
            estado.setText(R.string.adb_no_disponible)
            return
        }
        estado.setText(R.string.adb_comprobando)
        lifecycleScope.launch {
            val vivo = GestorAdb.verificar()
            estado.setText(if (vivo) R.string.adb_conectado else R.string.adb_sin_conexion)
        }
    }

    private fun abrirDesarrollo() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_se_pudo_abrir, Toast.LENGTH_LONG).show()
        }
    }

    private fun emparejar() {
        if (!GestorAdb.disponible()) {
            Toast.makeText(this, R.string.adb_no_disponible, Toast.LENGTH_LONG).show()
            return
        }
        val puerto = etPuerto.text.toString().trim().toIntOrNull()
        val codigo = etCodigo.text.toString().trim()
        if (puerto == null || puerto < 1 || puerto > 65535 || codigo.length != 6) {
            Toast.makeText(this, R.string.adb_datos_invalidos, Toast.LENGTH_LONG).show()
            return
        }
        estado.setText(R.string.adb_emparejando)
        lifecycleScope.launch {
            val resultado = try {
                if (GestorAdb.emparejar(applicationContext, puerto, codigo)) "emparejado" else "rechazado"
            } catch (e: Exception) {
                "error: " + e.javaClass.simpleName + " " + (e.message ?: "")
            }
            registro.text = "Emparejar → " + resultado
            if (resultado == "emparejado") {
                etCodigo.setText("")
                estado.setText(R.string.adb_emparejado_ok)
            } else {
                estado.setText(R.string.adb_emparejar_fallo)
            }
        }
    }

    private fun conectar() {
        if (!GestorAdb.disponible()) {
            Toast.makeText(this, R.string.adb_no_disponible, Toast.LENGTH_LONG).show()
            return
        }
        val textoPuerto = etPuertoCon.text.toString().trim()
        val puertoManual = if (textoPuerto.isEmpty()) null else textoPuerto.toIntOrNull()
        if (textoPuerto.isNotEmpty() && (puertoManual == null || puertoManual < 1 || puertoManual > 65535)) {
            Toast.makeText(this, R.string.adb_datos_invalidos, Toast.LENGTH_LONG).show()
            return
        }
        estado.setText(R.string.adb_conectando)
        lifecycleScope.launch {
            val resultado = try {
                if (GestorAdb.conectar(applicationContext, puertoManual)) "conectado" else "sin conexión"
            } catch (e: Exception) {
                "error: " + e.javaClass.simpleName + " " + (e.message ?: "")
            }
            registro.text = "Conectar → " + resultado
            estado.setText(if (resultado == "conectado") R.string.adb_conectado else R.string.adb_conectar_fallo)
        }
    }

    private class Restaurable(val paquete: String, val etiqueta: String, val quitada: Boolean)

    @Suppress("DEPRECATION")
    private fun listarRestaurables() {
        lifecycleScope.launch {
            val lista = withContext(Dispatchers.IO) {
                val pm = packageManager
                val res = ArrayList<Restaurable>()
                for (info in pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)) {
                    val quitada = (info.flags and ApplicationInfo.FLAG_INSTALLED) == 0
                    val desactivada = try {
                        pm.getApplicationEnabledSetting(info.packageName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    } catch (e: Exception) {
                        false
                    }
                    if (quitada || desactivada) res.add(Restaurable(info.packageName, info.loadLabel(pm).toString(), quitada))
                }
                res.sortBy { it.etiqueta.lowercase() }
                res
            }
            if (lista.isEmpty()) {
                Toast.makeText(this@ModoAdbActivity, R.string.nada_que_restaurar, Toast.LENGTH_LONG).show()
                return@launch
            }
            val nombres = lista.map { it.etiqueta + (if (it.quitada) " · quitada" else " · desactivada") + "\n" + it.paquete }
            AlertDialog.Builder(this@ModoAdbActivity)
                .setTitle(R.string.restaurar_apps)
                .setItems(nombres.toTypedArray()) { _, cual -> confirmarRestaurar(lista[cual]) }
                .setNegativeButton(R.string.cerrar, null)
                .show()
        }
    }

    private fun confirmarRestaurar(app: Restaurable) {
        if (!GestorAdb.conectado) {
            Toast.makeText(this, R.string.adb_sin_conexion, Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(app.etiqueta)
            .setMessage(getString(R.string.confirmar_restaurar, app.paquete))
            .setPositiveButton(R.string.restaurar) { _, _ ->
                val orden = if (app.quitada) "cmd package install-existing " + app.paquete else "pm enable " + app.paquete
                lifecycleScope.launch {
                    val salida = try {
                        GestorAdb.ejecutar(orden)
                    } catch (e: Exception) {
                        "error: " + e.javaClass.simpleName + " " + (e.message ?: "")
                    }
                    registro.text = orden + "\n" + salida
                    Toast.makeText(this@ModoAdbActivity, R.string.orden_enviada, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun olvidar() {
        AlertDialog.Builder(this)
            .setTitle(R.string.olvidar_emparejamiento)
            .setMessage(R.string.confirmar_olvidar)
            .setPositiveButton(R.string.olvidar) { _, _ ->
                GestorAdb.olvidar(applicationContext)
                estado.setText(R.string.adb_sin_conexion)
                registro.text = ""
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
