package com.planify.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.planify.app.data.AppContainer
import com.planify.app.ui.PlanifyNav
import com.planify.app.ui.theme.PlanifyTheme

@Composable
fun PlanifyRoot() {
    val appContainer = remember { AppContainer() }

    CompositionLocalProvider(
        LocalInstanceStore provides appContainer.instanceStore,
        LocalAppSettings provides appContainer.appSettings,
        LocalHttpClient provides appContainer.httpClient,
        LocalRepositories provides appContainer.repositories,
        LocalApiKeyStore provides appContainer.apiKeyStore,
        LocalThemeState provides appContainer.themeState
    ) {
        PlanifyTheme(themeState = appContainer.themeState) {
            PlanifyNav()
        }
    }
}
