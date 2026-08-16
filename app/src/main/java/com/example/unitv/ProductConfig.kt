package com.example.unitv

/** Configuração de produção do aplicativo Prestigie. */
object ProductConfig {
    val api: ApiConfig = ApiConfig(
        baseUrl = "https://renciaapp.manus.space",
        updateUrl = "",
        appVersion = "0.4.1-full-catalog-gate",
        deviceType = "prestigie",
        useDemoData = false
    )
}
