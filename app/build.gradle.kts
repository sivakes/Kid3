import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.0"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "MainKt"

                nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ChromaCut"
            packageVersion = "1.0.0"
            description = "Photo editing and color grading"

            // Java modules the app needs at runtime (this fixes "Failed to launch JVM")
            modules("java.net.http", "java.sql", "jdk.unsupported", "java.naming", "java.management")

            // The packaged Python engine is copied into app/resources/<os>/ by the build
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            windows { menuGroup = "ChromaCut"; shortcut = true }
            linux { shortcut = true }
        
}
}
}
