plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

// app/src/main/res stays the single source for strings and icons (Crowdin and Android-only code use it);
// this copies what the shared UI needs into Compose resources.
val syncAppResources = tasks.register<Sync>("syncAppResources") {
    val appRes = rootProject.layout.projectDirectory.dir("app/src/main/res")
    from(appRes) {
        include("values*/strings.xml", "values*/metrolist_strings.xml", "drawable/*.png", "drawable/*.xml")
        // Compose resources only load vector XML; shapes and layer lists are Android widget backgrounds.
        eachFile { if (name.endsWith(".xml") && path.startsWith("drawable/") && "<vector" !in file.readText()) exclude() }
        // Compose resources have no script qualifier.
        eachFile { path = path.replace("values-b+sr+Latn/", "values-sr/") }
    }
    // Android still reports Hebrew/Indonesian as iw/in, but the JVM uses he/id since JDK 17.
    from(appRes) {
        include("values-iw/*strings.xml", "values-in/*strings.xml")
        eachFile { path = path.replace("values-iw/", "values-he/").replace("values-in/", "values-id/") }
    }
    // Compose resources keep Android's \' \" \\ escapes literally and only substitute numbered arguments.
    // Gradle doesn't track filter lambdas; bump this when the conversion below changes.
    inputs.property("conversionVersion", 2)
    filesMatching("values*/*strings.xml") {
        filter { line ->
            line
                .replace(Regex("""\\([\\'"@?])"""), "\$1")
                // Android formats via String.format, so "%%" means "%" and a lone %s/%d is the first argument.
                .replace(Regex("%(%|[sd])")) { if (it.groupValues[1] == "%") "%" else "%1\$" + it.groupValues[1] }
        }
    }
    into(layout.buildDirectory.dir("appComposeResources"))
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.metrolist.music.resources"
    customDirectory("commonMain", layout.dir(syncAppResources.map { it.destinationDir }))
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
        androidResources { enable = true }
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(project(":innertube"))
            api(libs.room.runtime)
            api(libs.compose.multiplatform.runtime)
            api(libs.compose.components.resources)
            api(libs.datastore.preferences.core)
            api(libs.kermit)
            api(libs.lifecycle.viewmodel)
            api(libs.lifecycle.viewmodel.savedstate)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
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
            implementation(compose.desktop.currentOs)
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
