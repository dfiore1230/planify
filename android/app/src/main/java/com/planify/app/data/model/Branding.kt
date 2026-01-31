package com.planify.app.data.model

import com.planify.app.util.objectOrNull
import com.planify.app.util.string
import com.planify.app.util.stringAny
import kotlinx.serialization.json.JsonObject


data class BrandingResponse(
    val logoUrl: String?,
    val wordmarkUrl: String?,
    val primaryHex: String?,
    val secondaryHex: String?,
    val accentHex: String?,
    val textHex: String?,
    val bgHex: String?,
    val buttonRadius: Float?,
    val legalFooter: String?,
    val appIconUrl: String?
) {
    companion object {
        fun fromJson(obj: JsonObject): BrandingResponse {
            val legacyPrimary = obj.string("primary_hex")
            val legacySecondary = obj.string("secondary_hex")
            val legacyAccent = obj.string("accent_hex")

            val colors = obj.objectOrNull("colors")
            val primary = legacyPrimary ?: colors?.string("primary")
            val secondary = legacySecondary ?: colors?.string("secondary") ?: primary
            val accent = legacyAccent ?: colors?.string("tertiary") ?: secondary
            val text = obj.string("text_hex") ?: colors?.string("text") ?: colors?.string("onPrimary")
            val background = obj.string("bg_hex") ?: colors?.string("background")

            val buttonRadius = obj["button_radius"]?.jsonPrimitive?.floatOrNull
                ?: obj["buttonRadius"]?.jsonPrimitive?.floatOrNull

            return BrandingResponse(
                logoUrl = obj.stringAny(listOf("logo_url", "logoUrl")),
                wordmarkUrl = obj.stringAny(listOf("wordmark_url", "wordmarkUrl")),
                primaryHex = primary,
                secondaryHex = secondary,
                accentHex = accent,
                textHex = text,
                bgHex = background,
                buttonRadius = buttonRadius,
                legalFooter = obj.string("legal_footer") ?: obj.string("legalFooter"),
                appIconUrl = obj.stringAny(listOf("app_icon_url", "appIconUrl"))
            )
        }
    }
}

fun themeFromBranding(branding: BrandingResponse): ThemeDTO {
    val defaultTheme = ThemeDTO.default()
    return ThemeDTO(
        primaryHex = branding.primaryHex ?: defaultTheme.primaryHex,
        secondaryHex = branding.secondaryHex ?: branding.primaryHex ?: defaultTheme.secondaryHex,
        accentHex = branding.accentHex ?: branding.secondaryHex ?: defaultTheme.accentHex,
        textHex = branding.textHex ?: defaultTheme.textHex,
        backgroundHex = branding.bgHex ?: defaultTheme.backgroundHex,
        buttonRadius = branding.buttonRadius ?: defaultTheme.buttonRadius,
        legalFooter = branding.legalFooter
    )
}
