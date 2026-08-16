package com.example.unitv

/** Configuração de produção do aplicativo Prestigie. */
object ProductConfig {
    val api: ApiConfig = ApiConfig(
        baseUrl = "https://renciaapp.manus.space",
        updateUrl = "",
        appVersion = "0.4.2-kids-channels-parental",
        deviceType = "prestigie",
        useDemoData = false
    )
}
