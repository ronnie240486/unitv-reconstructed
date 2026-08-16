package com.example.unitv

/** Configuração de produção do aplicativo Prestigie. */
object ProductConfig {
    val api: ApiConfig = ApiConfig(
        baseUrl = "https://renciaapp.manus.space",
        updateUrl = "",
        appVersion = "0.3.1-full-m3u",
        deviceType = "prestigie",
        useDemoData = false
    )
}
