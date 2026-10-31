package com.vigia.app.util

/**
 * Diccionario local de dominios conocidos y de tipos de evento: sirve para explicar «qué es esto» sin salir de la app.
 * Es orientativo: el nombre de un dominio no prueba para qué se está usando. Se busca por el nombre completo y, si no
 * está, por sus dominios padre (la entrada más específica gana).
 */
object Diccionario {

    private val DOMINIOS: Map<String, String> = hashMapOf(
        // Google y Android
        "google.com" to "Google: búsqueda y servicios",
        "googleapis.com" to "Google: APIs de servicios que usan muchísimas apps",
        "gstatic.com" to "Google: recursos estáticos (imágenes, scripts, fuentes)",
        "connectivitycheck.gstatic.com" to "Android: comprobación de conectividad (¿hay internet?)",
        "connectivitycheck.android.com" to "Android: comprobación de conectividad (¿hay internet?)",
        "clients3.google.com" to "Android: comprobación de conectividad y servicios de Google",
        "googleusercontent.com" to "Google: contenido de usuarios (fotos, documentos, avatares)",
        "ggpht.com" to "Google: imágenes (fotos, avatares)",
        "doubleclick.net" to "Google: red de publicidad",
        "googlesyndication.com" to "Google: publicidad (AdSense)",
        "googleadservices.com" to "Google: publicidad y medición de conversiones",
        "google-analytics.com" to "Google: estadísticas de uso (Analytics)",
        "googletagmanager.com" to "Google: gestor de etiquetas de seguimiento",
        "app-measurement.com" to "Google Firebase: medición de uso de apps",
        "firebaseio.com" to "Google Firebase: base de datos en tiempo real",
        "firebaseinstallations.googleapis.com" to "Google Firebase: identificador de instalación de una app",
        "crashlytics.com" to "Google Firebase: informes de fallos de apps",
        "mtalk.google.com" to "Google: conexión de notificaciones push (FCM)",
        "fcm.googleapis.com" to "Google: notificaciones push (FCM)",
        "android.clients.google.com" to "Android: servicios de Google Play y registro del dispositivo",
        "play.googleapis.com" to "Google Play: servicios de la tienda",
        "youtube.com" to "YouTube: vídeo",
        "ytimg.com" to "YouTube: imágenes y miniaturas",
        "googlevideo.com" to "YouTube: transmisión de vídeo",
        "gvt1.com" to "Google: descargas de actualizaciones (Chrome, Play)",
        "gvt2.com" to "Google: descargas de actualizaciones (Chrome, Play)",
        "time.android.com" to "Android: sincronización de la hora",
        "dns.google" to "Google: DNS público cifrado (DoH)",
        // Meta
        "facebook.com" to "Meta: Facebook",
        "graph.facebook.com" to "Meta: API de inicio de sesión y telemetría de Facebook",
        "facebook.net" to "Meta: SDK y píxel de seguimiento de Facebook",
        "fbcdn.net" to "Meta: contenido (fotos, vídeos) de Facebook e Instagram",
        "fb.com" to "Meta: Facebook",
        "instagram.com" to "Meta: Instagram",
        "cdninstagram.com" to "Meta: contenido de Instagram",
        "whatsapp.com" to "Meta: WhatsApp",
        "whatsapp.net" to "Meta: servidores de mensajería de WhatsApp",
        // Microsoft
        "microsoft.com" to "Microsoft: servicios y software",
        "live.com" to "Microsoft: cuentas y servicios (Outlook, OneDrive)",
        "office.com" to "Microsoft: Office",
        "office365.com" to "Microsoft: Office 365",
        "outlook.com" to "Microsoft: correo Outlook",
        "microsoftonline.com" to "Microsoft: inicio de sesión de cuentas",
        "windowsupdate.com" to "Microsoft: actualizaciones de Windows",
        "msftconnecttest.com" to "Microsoft: comprobación de conectividad",
        "msftncsi.com" to "Microsoft: comprobación de conectividad",
        "appcenter.ms" to "Microsoft: telemetría y reporte de fallos de apps (App Center)",
        "azureedge.net" to "Microsoft Azure: red de distribución de contenido (CDN)",
        "azure.com" to "Microsoft Azure: nube",
        "bing.com" to "Microsoft: buscador Bing",
        "skype.com" to "Microsoft: Skype",
        "linkedin.com" to "LinkedIn (Microsoft): red profesional",
        "licdn.com" to "LinkedIn (Microsoft): contenido",
        // Apple
        "apple.com" to "Apple: servicios",
        "icloud.com" to "Apple: iCloud",
        "mzstatic.com" to "Apple: contenido de tiendas y servicios",
        "cdn-apple.com" to "Apple: red de distribución de contenido",
        "push.apple.com" to "Apple: notificaciones push",
        // Amazon y nubes
        "amazon.com" to "Amazon: tienda y servicios",
        "amazonaws.com" to "Amazon Web Services: alojamiento que usan muchísimas apps y webs",
        "cloudfront.net" to "Amazon CloudFront: red de distribución de contenido (CDN)",
        "amazon-adsystem.com" to "Amazon: publicidad",
        "media-amazon.com" to "Amazon: imágenes y contenido",
        "primevideo.com" to "Amazon: Prime Video",
        "cloudflare.com" to "Cloudflare: red de distribución y protección web",
        "cloudflare-dns.com" to "Cloudflare: DNS público cifrado (DoH)",
        "workers.dev" to "Cloudflare: aplicaciones alojadas en Cloudflare Workers",
        "akamaihd.net" to "Akamai: red de distribución de contenido (CDN)",
        "akamai.net" to "Akamai: red de distribución de contenido (CDN)",
        "akamaiedge.net" to "Akamai: red de distribución de contenido (CDN)",
        "edgekey.net" to "Akamai: red de distribución de contenido (CDN)",
        "fastly.net" to "Fastly: red de distribución de contenido (CDN)",
        // Publicidad, atribución y analítica
        "adjust.com" to "Adjust: medición y atribución de publicidad móvil",
        "appsflyer.com" to "AppsFlyer: atribución de publicidad móvil",
        "branch.io" to "Branch: enlaces profundos y atribución de apps",
        "applovin.com" to "AppLovin: publicidad en apps y juegos",
        "unity3d.com" to "Unity: motor de juegos, publicidad y estadísticas",
        "unityads.unity3d.com" to "Unity: publicidad en juegos",
        "ironsrc.com" to "ironSource: publicidad en apps y juegos",
        "vungle.com" to "Vungle: publicidad en vídeo en apps",
        "chartboost.com" to "Chartboost: publicidad en juegos",
        "inmobi.com" to "InMobi: publicidad móvil",
        "criteo.com" to "Criteo: publicidad personalizada",
        "taboola.com" to "Taboola: recomendaciones y publicidad",
        "outbrain.com" to "Outbrain: recomendaciones y publicidad",
        "scorecardresearch.com" to "Comscore: medición de audiencias",
        "flurry.com" to "Flurry: estadísticas de uso de apps",
        "mixpanel.com" to "Mixpanel: analítica de uso",
        "amplitude.com" to "Amplitude: analítica de uso",
        "segment.io" to "Segment: recogida y reenvío de datos de uso",
        "sentry.io" to "Sentry: informes de fallos",
        "bugsnag.com" to "Bugsnag: informes de fallos",
        "newrelic.com" to "New Relic: monitorización de rendimiento",
        "datadoghq.com" to "Datadog: monitorización de rendimiento",
        "hotjar.com" to "Hotjar: analítica de comportamiento en webs",
        // Redes sociales y mensajería
        "tiktok.com" to "TikTok",
        "tiktokv.com" to "TikTok: servicios de vídeo",
        "byteoversea.com" to "ByteDance (TikTok): servicios y telemetría",
        "snapchat.com" to "Snapchat",
        "sc-cdn.net" to "Snapchat: contenido",
        "twitter.com" to "X (Twitter)",
        "twimg.com" to "X (Twitter): imágenes y contenido",
        "x.com" to "X (Twitter)",
        "reddit.com" to "Reddit",
        "redditstatic.com" to "Reddit: contenido estático",
        "pinterest.com" to "Pinterest",
        "telegram.org" to "Telegram: mensajería",
        "t.me" to "Telegram: enlaces",
        "signal.org" to "Signal: mensajería cifrada",
        "whispersystems.org" to "Signal: servicios de mensajería cifrada",
        "discord.com" to "Discord",
        "discordapp.com" to "Discord",
        // Entretenimiento
        "spotify.com" to "Spotify: música",
        "scdn.co" to "Spotify: contenido y audio",
        "netflix.com" to "Netflix",
        "nflxvideo.net" to "Netflix: transmisión de vídeo",
        "nflximg.net" to "Netflix: imágenes",
        "twitch.tv" to "Twitch",
        "ttvnw.net" to "Twitch: transmisión de vídeo",
        // Fabricantes
        "samsung.com" to "Samsung: servicios",
        "samsungcloud.com" to "Samsung: nube",
        "samsungapps.com" to "Samsung: tienda Galaxy Store",
        "samsungdm.com" to "Samsung: gestión de dispositivos y actualizaciones",
        "xiaomi.com" to "Xiaomi: servicios",
        "mi.com" to "Xiaomi: servicios",
        "huawei.com" to "Huawei: servicios",
        "hicloud.com" to "Huawei: nube",
        "heytapmobi.com" to "OPPO/OnePlus: servicios del sistema",
        // DNS cifrado, hora y red local
        "quad9.net" to "Quad9: DNS público (puede ser cifrado)",
        "nextdns.io" to "NextDNS: DNS filtrado cifrado",
        "one.one.one.one" to "Cloudflare: DNS público 1.1.1.1",
        "ntp.org" to "Servidores de hora de red (NTP)",
        "pool.ntp.org" to "Servidores de hora de red (NTP)",
        "local" to "Nombre de la red local (por ejemplo impresoras o la tele)",
        "lan" to "Nombre de la red local",
        "home.arpa" to "Nombre de la red local"
    )

    private val TIPOS: Map<String, String> = hashMapOf(
        "A" to "Consulta DNS: pregunta qué dirección IPv4 tiene un nombre. Es el primer paso de casi toda conexión.",
        "AAAA" to "Consulta DNS: pregunta qué dirección IPv6 tiene un nombre.",
        "CNAME" to "Consulta DNS: pregunta si un nombre es un alias de otro.",
        "HTTPS" to "Consulta DNS: pide los datos de conexión segura (HTTPS/QUIC) de un dominio.",
        "SVCB" to "Consulta DNS: pide cómo conectarse a un servicio.",
        "TXT" to "Consulta DNS: pide un texto asociado al dominio (se usa para verificaciones y, a veces, para esconder datos).",
        "MX" to "Consulta DNS: pregunta qué servidor recibe el correo de un dominio.",
        "PTR" to "Consulta DNS inversa: pregunta qué nombre tiene una IP.",
        "SRV" to "Consulta DNS: busca el servidor de un servicio concreto.",
        "NS" to "Consulta DNS: pregunta quién gestiona un dominio.",
        "TCP" to "Conexión TCP: la mayor parte del tráfico (web, apps, mensajería). Se ve el destino y los bytes, no el contenido.",
        "UDP" to "Conexión UDP: DNS, vídeo en directo, llamadas, juegos y QUIC (HTTP/3).",
        "TCP-rechazada" to "Una conexión TCP que el destino no aceptó.",
        "TCP-bloqueada" to "Una conexión que cortó el Escudo porque el destino estaba bloqueado.",
        "camara" to "Cámara: pasó a estar en uso o se liberó.",
        "microfono" to "Micrófono: pasó a estar en uso o se liberó.",
        "ventana" to "Pantalla: una app pasó a estar delante.",
        "clic" to "Pantalla: se tocó algo en una app (no se registra qué).",
        "pantalla_encendida" to "Sistema: se encendió la pantalla.",
        "pantalla_apagada" to "Sistema: se apagó la pantalla.",
        "desbloqueo" to "Sistema: se desbloqueó el teléfono.",
        "red_cambio" to "Sistema: cambió la red por la que sale el tráfico (wifi, datos...).",
        "red_perdida" to "Sistema: se perdió la conexión a internet.",
        "paquete_instalado" to "Sistema: se instaló una app.",
        "paquete_eliminado" to "Sistema: se borró una app.",
        "paquete_actualizado" to "Sistema: se actualizó una app.",
        "idioma_cambiado" to "Sistema: cambió el idioma del teléfono.",
        "hora_cambiada" to "Sistema: se cambió la hora del teléfono.",
        "zona_horaria_cambiada" to "Sistema: cambió la zona horaria.",
        "certificados_cambiaron" to "Sistema: cambió el almacén de certificados de confianza."
    )

    /** Descripción del dominio (o del dominio padre más específico que esté en el diccionario), o null si no se conoce. */
    fun buscar(dominio: String): String? {
        var actual = dominio.lowercase().trim('.')
        while (true) {
            val d = DOMINIOS[actual]
            if (d != null) return d
            val punto = actual.indexOf('.')
            if (punto < 0) return null
            actual = actual.substring(punto + 1)
        }
    }

    fun explicarTipo(categoria: String, tipo: String): String {
        val t = TIPOS[tipo]
        if (t != null) return t
        if (tipo.startsWith("inventario:")) return "Inventario: un cambio entre dos fotos del teléfono (" + tipo.removePrefix("inventario:").replace('_', ' ') + ")."
        return "Evento de tipo «" + tipo + "» (categoría " + categoria + ")."
    }
}
