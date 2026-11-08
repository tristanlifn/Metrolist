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

// Same version as the Android app plus a channel suffix: "dev" locally, release builds pass -PdesktopChannel=beta.
val desktopVersion =
    providers.fileContents(rootProject.layout.projectDirectory.file("app/build.gradle.kts")).asText
        .map { requireNotNull(Regex("""versionName = "([^"]+)"""").find(it)) { "versionName not found in app/build.gradle.kts" }.groupValues[1] }
        .zip(providers.gradleProperty("desktopChannel").orElse("dev")) { version, channel -> "$version-$channel" }
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
