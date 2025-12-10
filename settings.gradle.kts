rootProject.name = "reconciler"

// Configure toolchain management to download Java 21 if not found
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

