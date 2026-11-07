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
    implementation(libs.dbus.java.core)
    implementation(libs.dbus.java.transport)
}

compose.desktop {
    application {
        mainClass = "com.metrolist.desktop.MainKt"
    }
}
