package com.planify.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.planify.app.data.model.ThemeDTO
import com.planify.app.data.storage.InstanceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThemeState(private val instanceStore: InstanceStore) {
    var theme by mutableStateOf(ThemeColors.default())
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            instanceStore.instances.collect { _ ->
                updateTheme()
            }
        }
        scope.launch {
            instanceStore.activeInstanceId.collect { _ ->
                updateTheme()
            }
        }
        updateTheme()
    }

    private fun updateTheme() {
        val dto = instanceStore.activeInstance()?.theme
        theme = ThemeColors.fromDto(dto)
    }
}

data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val text: Color,
    val background: Color,
    val buttonRadius: Float,
    val legalFooter: String?
) {
    companion object {
        fun default(): ThemeColors = fromDto(null)

        fun fromDto(dto: ThemeDTO?): ThemeColors {
            val primary = parseColor(dto?.primaryHex) ?: Color(0xFF007AFF)
            val secondary = parseColor(dto?.secondaryHex) ?: Color(0xFF8E8E93)
            val accent = parseColor(dto?.accentHex) ?: Color(0xFF34C759)
            val text = parseColor(dto?.textHex) ?: Color(0xFF000000)
            val background = parseColor(dto?.backgroundHex) ?: Color(0xFFFFFFFF)
            return ThemeColors(
                primary = primary,
                secondary = secondary,
                accent = accent,
                text = text,
                background = background,
                buttonRadius = dto?.buttonRadius ?: 10f,
                legalFooter = dto?.legalFooter
            )
        }

        private fun parseColor(hex: String?): Color? {
            if (hex.isNullOrBlank()) return null
            val cleaned = hex.removePrefix("#")
            val value = cleaned.toLongOrNull(16) ?: return null
            return when (cleaned.length) {
                6 -> Color((0xFF000000 or value).toInt())
                8 -> Color(value.toInt())
                else -> null
            }
        }
    }
}
