plugins {
    id("com.android.application") version "8.13.2" apply false

    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false

    // Required for Compose with Kotlin 2.x
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

