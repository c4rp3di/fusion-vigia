package com.vigia.app.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.vigia.app.R
import com.vigia.app.analyze.Severidad
import com.vigia.app.store.FindingEntity
import com.vigia.app.util.Formato

class HallazgosAdapter(private val contexto: Context) :
    ArrayAdapter<FindingEntity>(contexto, android.R.layout.simple_list_item_2, android.R.id.text1) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vista = super.getView(position, convertView, parent)
        val h = getItem(position)
        val titulo = vista.findViewById<TextView>(android.R.id.text1)
        val detalle = vista.findViewById<TextView>(android.R.id.text2)
        if (h != null) {
            val esNota = h.regla == "NOTA"
            val esInformativo = h.severidad == Severidad.INFO
            val resumen = if (h.descripcion.length > 150) h.descripcion.substring(0, 150) + "…" else h.descripcion
            if (esNota) {
                // Nota del análisis: explica lo que no se pudo comprobar. No es una alerta.
                titulo.text = "ℹ Nota del análisis · " + h.titulo
                detalle.text = resumen
            } else if (esInformativo) {
                titulo.text = "ℹ Informativo · " + h.titulo
                val app = if (h.app.isEmpty()) "" else h.app + "\n"
                detalle.text = app + resumen
            } else {
                titulo.text = "[" + Severidad.etiqueta(h.severidad) + " · " + h.regla + " · " + h.confianza + "%] " + h.titulo
                val app = if (h.app.isEmpty()) "" else " · " + h.app
                detalle.text = Formato.fecha(h.ts) + app + "\n" + resumen
            }
            val color = when {
                esInformativo -> contexto.getColor(R.color.vigia_gris)
                h.severidad == Severidad.CRITICA || h.severidad == Severidad.ALTA -> contexto.getColor(R.color.vigia_rojo)
                h.severidad == Severidad.MEDIA -> contexto.getColor(R.color.vigia_ambar)
                else -> colorTexto()
            }
            titulo.setTextColor(color)
        }
        return vista
    }

    private fun colorTexto(): Int {
        val v = android.util.TypedValue()
        contexto.theme.resolveAttribute(android.R.attr.textColorPrimary, v, true)
        return if (v.resourceId != 0) contexto.getColor(v.resourceId) else v.data
    }

    fun actualizar(lista: List<FindingEntity>) {
        setNotifyOnChange(false)
        clear()
        addAll(lista)
        notifyDataSetChanged()
    }
}
