package com.vigia.app.util

/**
 * Sanitización de la exportación a CSV: oculta lo que más delata sin quitarle utilidad al archivo para pedir ayuda.
 *  - IPs de redes locales → «[Red Local]».
 *  - IPs públicas que no son de las grandes CDN (rangos orientativos y pocos a propósito) → «[IP Oculta]».
 *  - Dominios, nombres de apps y paquetes con palabras sensibles → «[Dominio Sensible]» o «[App Sensible]».
 *  - La hora se redondea a la hora o al minuto.
 */
object SanitizadorCsv {

    const val HORA = 0
    const val MINUTO = 1

    private val PALABRAS = listOf(
        "bank", "banco", "gov", "salud", "dating", "tinder",
        "paypal", "bizum", "hacienda", "clinic", "hospital", "grindr", "bumble", "okcupid", "health", "medic"
    )

    /** Rangos de las grandes redes de distribución y nubes (Cloudflare, Google, Microsoft, CloudFront). Orientativos. */
    private val CDN: List<Pair<Int, Int>> = listOf(
        // Cloudflare (lista publicada por la propia empresa)
        "173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22", "141.101.64.0/18", "108.162.192.0/18",
        "190.93.240.0/20", "188.114.96.0/20", "197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
        "104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22",
        // Google
        "8.8.8.0/24", "8.8.4.0/24", "74.125.0.0/16", "172.217.0.0/16", "142.250.0.0/15", "216.58.192.0/19", "173.194.0.0/16",
        "64.233.160.0/19", "209.85.128.0/17", "108.177.0.0/17", "172.253.0.0/16", "216.239.32.0/19",
        // Microsoft
        "13.107.0.0/16", "20.190.128.0/18", "40.126.0.0/18", "52.96.0.0/14", "52.112.0.0/14", "204.79.195.0/24",
        // Amazon CloudFront
        "13.32.0.0/15", "13.224.0.0/14", "99.84.0.0/16", "143.204.0.0/16", "54.230.0.0/16", "52.84.0.0/15", "54.239.128.0/18"
    ).map { cidr(it) }

    private val LOCALES: List<Pair<Int, Int>> = listOf(
        "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8", "169.254.0.0/16", "100.64.0.0/10", "0.0.0.0/8"
    ).map { cidr(it) }

    private val IPV4 = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")
    private val CANDIDATO_V6 = Regex("[0-9a-fA-F:]{3,}")
    private val NOMBRE = Regex("[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+")

    private fun cidr(texto: String): Pair<Int, Int> {
        val partes = texto.split('/')
        val red = parseIp(partes[0]) ?: 0
        val bits = partes[1].toInt()
        val mascara = if (bits == 0) 0 else (-1 shl (32 - bits))
        return Pair(red and mascara, mascara)
    }

    private fun parseIp(texto: String): Int? {
        val p = texto.split('.')
        if (p.size != 4) return null
        var r = 0
        for (x in p) {
            val n = x.toIntOrNull() ?: return null
            if (n < 0 || n > 255) return null
            r = (r shl 8) or n
        }
        return r
    }

    private fun dentro(ip: Int, rangos: List<Pair<Int, Int>>): Boolean {
        for (r in rangos) {
            if ((ip and r.second) == r.first) return true
        }
        return false
    }

    /** Etiqueta para una IP: se deja tal cual solo si es de una CDN conocida. */
    fun ip(texto: String): String {
        if (texto.isEmpty()) return texto
        val ip = parseIp(texto) ?: return if (texto.contains(':')) "[IP Oculta]" else texto
        if (dentro(ip, LOCALES)) return "[Red Local]"
        if (dentro(ip, CDN)) return texto
        return "[IP Oculta]"
    }

    fun esSensible(nombre: String): Boolean {
        val n = nombre.lowercase()
        for (p in PALABRAS) {
            if (n.contains(p)) return true
        }
        return false
    }

    fun dominio(texto: String): String = if (texto.isNotEmpty() && esSensible(texto)) "[Dominio Sensible]" else texto

    fun app(texto: String): String = if (texto.isNotEmpty() && esSensible(texto)) "[App Sensible]" else texto

    /** Texto libre (detalles, respuestas DNS...): cambia IPs y nombres sensibles dentro de la frase. */
    fun textoLibre(texto: String): String {
        // IPv6: un trozo de dígitos hexadecimales y «:» con «::» o al menos cuatro «:» (así no se confunden las horas 12:30:45).
        var t = CANDIDATO_V6.replace(texto) { m ->
            val v = m.value
            val separadores = v.count { it == ':' }
            if (separadores >= 2 && (v.contains("::") || separadores >= 4)) "[IP Oculta]" else v
        }
        t = IPV4.replace(t) { m -> ip(m.value) }
        t = NOMBRE.replace(t) { m -> if (esSensible(m.value)) "[Dominio Sensible]" else m.value }
        return t
    }

    /** La hora redondeada a la hora o al minuto, como texto. */
    fun fecha(ms: Long, redondeo: Int): String {
        val completa = Formato.fechaHoraCompleta(ms)
        return if (redondeo == HORA) completa.substring(0, 13) + ":00" else completa.substring(0, 16)
    }
}
