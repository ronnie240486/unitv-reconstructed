package com.example.unitv

/** Configuração de produção do aplicativo Prestigie. */
object ProductConfig {
    val api: ApiConfig = ApiConfig(
        baseUrl = "https://renciaapp-production.up.railway.app",
        fallbackBaseUrl = "https://renciaapp.manus.space",
        appId = "prestige",
        updateUrl = "",
        appVersion = "0.4.9-backend-guide",
        deviceType = "prestigie",
        useDemoData = false
    )
}
