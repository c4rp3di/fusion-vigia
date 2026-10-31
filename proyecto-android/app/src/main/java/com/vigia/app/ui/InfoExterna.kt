package com.vigia.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vigia.app.R
import com.vigia.app.util.Ayuda
import com.vigia.app.util.Diccionario
import com.vigia.app.util.Portapapeles

/**
 * Ayuda contextual sobre un evento: «¿Qué es esto?» (diccionario local, sin salir de la app) y una hoja con enlaces para
 * investigar el dominio o la IP en servicios externos. Al pulsar un enlace se abre el navegador y la consulta sale de Vigía.
 */
object InfoExterna {

    private class Enlace(val nombre: String, val descripcion: String, val url: String)

    fun queEs(actividad: Activity, categoria: String, tipo: String, app: String, dominio: String, ip: String) {
        val sb = StringBuilder()
        if (dominio.isNotEmpty()) {
            sb.append("Dominio: ").append(dominio).append('\n')
            val d = Diccionario.buscar(dominio)
            if (d != null) {
                sb.append(d).append("\n\n")
            } else {
                sb.append("No está en el diccionario de Vigía. Eso no significa que sea sospechoso: hay millones de dominios. ")
                    .append("Usa «Analizar información externa» para investigarlo.\n\n")
            }
        }
        if (ip.isNotEmpty()) sb.append("IP: ").append(ip).append("\n\n")
        sb.append("Tipo de evento (").append(tipo).append("): ").append(Diccionario.explicarTipo(categoria, tipo)).append('\n')
        if (app.isNotEmpty() && app != "desconocida") sb.append("\nApp que lo generó: ").append(app).append('\n')
        sb.append("\nEl diccionario es orientativo: el nombre de un dominio no prueba para qué se está usando.")
        AlertDialog.Builder(actividad)
            .setTitle(R.string.que_es_titulo)
            .setMessage(sb.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    fun hoja(actividad: Activity, dominio: String, ip: String) {
        if (dominio.isEmpty() && ip.isEmpty()) {
            Toast.makeText(actividad, R.string.nada_que_analizar, Toast.LENGTH_SHORT).show()
            return
        }
        val hoja = BottomSheetDialog(actividad)
        val margen = (16 * actividad.resources.displayMetrics.density).toInt()
        val raiz = LinearLayout(actividad)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setPadding(margen, margen, margen, margen * 2)

        val titulo = TextView(actividad)
        titulo.setText(R.string.analizar_externa_titulo)
        titulo.textSize = 18f
        titulo.setTypeface(titulo.typeface, android.graphics.Typeface.BOLD)
        raiz.addView(titulo)

        val aviso = TextView(actividad)
        aviso.setText(R.string.aviso_externo)
        aviso.textSize = 13f
        aviso.setPadding(0, margen / 2, 0, margen / 2)
        raiz.addView(aviso)

        if (dominio.isNotEmpty()) {
            anadirSeccion(actividad, raiz, hoja, "Dominio: " + dominio, dominio, enlacesDominio(dominio))
        }
        if (ip.isNotEmpty()) {
            anadirSeccion(actividad, raiz, hoja, "IP: " + ip, ip, enlacesIp(ip))
        }
        hoja.setContentView(raiz)
        hoja.show()
    }

    private fun enlacesDominio(dominio: String): List<Enlace> {
        val d = Uri.encode(dominio)
        return listOf(
            Enlace("VirusTotal", "reputación según decenas de antivirus", "https://www.virustotal.com/gui/domain/" + d),
            Enlace("urlscan.io", "análisis y capturas de la web", "https://urlscan.io/domain/" + d),
            Enlace("RDAP", "quién registró el dominio", "https://rdap.org/domain/" + d),
            Enlace("ICANN Lookup", "datos oficiales del registro", "https://lookup.icann.org/en/lookup?name=" + d)
        )
    }

    private fun enlacesIp(ip: String): List<Enlace> {
        val i = Uri.encode(ip)
        return listOf(
            Enlace("VirusTotal", "reputación según decenas de antivirus", "https://www.virustotal.com/gui/ip-address/" + i),
            Enlace("AbuseIPDB", "denuncias de abuso de esta IP", "https://www.abuseipdb.com/check/" + i),
            Enlace("urlscan.io", "webs analizadas en esta IP", "https://urlscan.io/ip/" + i),
            Enlace("RDAP", "a quién pertenece la IP", "https://rdap.org/ip/" + i)
        )
    }

    private fun anadirSeccion(actividad: Activity, raiz: LinearLayout, hoja: BottomSheetDialog, cabecera: String, valor: String, enlaces: List<Enlace>) {
        val t = TextView(actividad)
        t.text = cabecera
        t.textSize = 14f
        t.setTypeface(t.typeface, android.graphics.Typeface.BOLD)
        t.setPadding(0, 24, 0, 0)
        raiz.addView(t)
        for (e in enlaces) {
            raiz.addView(Ayuda.enlace(actividad, e.nombre + " — " + e.descripcion + " ›") { abrir(actividad, hoja, e.url) })
        }
        raiz.addView(Ayuda.enlace(actividad, "Copiar " + valor) {
            Portapapeles.copiar(actividad, valor)
            Toast.makeText(actividad, R.string.copiado, Toast.LENGTH_SHORT).show()
        })
    }

    private fun abrir(actividad: Activity, hoja: BottomSheetDialog, url: String) {
        try {
            actividad.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            hoja.dismiss()
        } catch (e: Exception) {
            Toast.makeText(actividad, R.string.no_se_pudo_abrir, Toast.LENGTH_LONG).show()
        }
    }
}
