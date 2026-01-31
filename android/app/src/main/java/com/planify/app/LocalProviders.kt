package com.planify.app

import androidx.compose.runtime.compositionLocalOf
import com.planify.app.data.AppSettings
import com.planify.app.data.storage.InstanceStore
import com.planify.app.data.Repositories
import com.planify.app.data.network.HttpClient
import com.planify.app.data.storage.ApiKeyStore
import com.planify.app.ui.theme.ThemeState

val LocalInstanceStore = compositionLocalOf<InstanceStore> {
    error("InstanceStore not provided")
}

val LocalAppSettings = compositionLocalOf<AppSettings> {
    error("AppSettings not provided")
}

val LocalHttpClient = compositionLocalOf<HttpClient> {
    error("HttpClient not provided")
}

val LocalRepositories = compositionLocalOf<Repositories> {
    error("Repositories not provided")
}

val LocalApiKeyStore = compositionLocalOf<ApiKeyStore> {
    error("ApiKeyStore not provided")
}

val LocalThemeState = compositionLocalOf<ThemeState> {
    error("ThemeState not provided")
}
