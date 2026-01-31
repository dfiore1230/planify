package com.planify.app.util

fun buildAbsoluteUrl(apiBaseUrl: String, path: String): String {
    val trimmed = apiBaseUrl.removeSuffix("/")
    val webBase = if (trimmed.endsWith("/api")) trimmed.removeSuffix("/api") else trimmed
    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return webBase + normalizedPath
}
