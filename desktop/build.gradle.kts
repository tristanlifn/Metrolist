plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.coroutines.swing)
    implementation(libs.coil.network.okhttp)
    implementation(libs.jna)
    implementation(libs.sqlite.bundled)
    implementation(libs.dbus.java.core)
    implementation(libs.dbus.java.transport)
}

// Release builds pass -PdesktopVersion; local builds report a dev version.
val desktopVersion = providers.gradleProperty("desktopVersion").orElse("0.1.0-dev")
val generateVersionResource by tasks.registering {
    val version = desktopVersion
    val outputDir = layout.buildDirectory.dir("generated/version")
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().file("metrolist-desktop.properties").asFile.writeText("version=${version.get()}\n")
    }
}
sourceSets.main {
    resources.srcDir(generateVersionResource)
}

compose.desktop {
    application {
        mainClass = "com.metrolist.desktop.MainKt"
    }
}
