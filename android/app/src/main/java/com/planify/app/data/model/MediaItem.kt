package com.planify.app.data.model


data class MediaUsage(
    val id: Int,
    val context: String? = null,
    val contextLabel: String? = null,
    val type: String? = null,
    val displayName: String? = null,
    val usableId: Int? = null,
    val variantId: Int? = null
)

data class MediaTag(
    val id: Int,
    val name: String,
    val slug: String
)

data class MediaVariant(
    val id: Int,
    val label: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null
)

data class MediaItem(
    val id: Int,
    val uuid: String,
    val url: String,
    val originalFilename: String,
    val width: Int? = null,
    val height: Int? = null,
    val folder: String? = null,
    val usageCount: Int = 0,
    val usages: List<MediaUsage> = emptyList(),
    val tags: List<MediaTag> = emptyList(),
    val variants: List<MediaVariant> = emptyList()
) {
    val thumbnailUrl: String
        get() = url

    val displayName: String
        get() {
            return originalFilename
                .replace(Regex("^(flyer_|banner_|profile_)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\.[^.]+$"), "")
                .ifBlank { originalFilename }
        }
}

data class MediaLibraryResponse(
    val data: List<MediaItem>,
    val pagination: PaginationMeta
) {
    data class PaginationMeta(
        val currentPage: Int,
        val lastPage: Int,
        val perPage: Int,
        val total: Int
    )
}
