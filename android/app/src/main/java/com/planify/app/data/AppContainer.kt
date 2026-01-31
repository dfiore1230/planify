package com.planify.app.data

import android.content.Context
import com.planify.app.data.network.HttpClient
import com.planify.app.data.storage.ApiKeyStore
import com.planify.app.data.storage.InstanceStore
import com.planify.app.data.storage.SettingsStore
import com.planify.app.ui.theme.ThemeState
import com.planify.app.util.AppContext

class AppContainer {
    private val context: Context = AppContext.context

    val instanceStore: InstanceStore = InstanceStore(context)
    val appSettings: AppSettings = AppSettings(SettingsStore(context))
    val apiKeyStore: ApiKeyStore = ApiKeyStore(context)
    val httpClient: HttpClient = HttpClient(apiKeyStore)
    val repositories: Repositories = Repositories(httpClient, appSettings, apiKeyStore)
    val themeState: ThemeState = ThemeState(instanceStore)
}
