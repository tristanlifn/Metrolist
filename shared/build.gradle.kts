plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.metrolist.shared"
        compileSdk = 37
        minSdk = 26
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(project(":innertube"))
            api(libs.room.runtime)
            implementation(libs.compose.runtime.annotation)
        }
        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.timber)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.sqlite.bundled)
        }
        getByName("desktopTest").dependencies {
            implementation(libs.junit)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
