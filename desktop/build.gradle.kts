plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.metrolist.desktop.MainKt")
}

dependencies {
    implementation(project(":innertube"))
}
