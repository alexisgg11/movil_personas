plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val startLocalServices = tasks.register<Exec>("startLocalServices") {
    group = "application"
    description = "Prepara PostgreSQL, compila la API y conecta dispositivos USB."
    val script = rootProject.layout.projectDirectory.file("scripts/start-local-services.ps1")
    commandLine("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script.asFile.absolutePath)
    val enabled = providers.gradleProperty("START_LOCAL_SERVICES").orNull != "false"
    onlyIf { enabled }
    notCompatibleWithConfigurationCache("Arranque idempotente de servicios locales fuera de Gradle.")
}
tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(startLocalServices) }

android {
    namespace = "com.example.personas"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.personas"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        val apiUrl = providers.gradleProperty("PERSONAS_API_URL").orElse("").get().trim().trimEnd('/')
        buildConfigField("String", "API_BASE_URL", "\"${apiUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}


tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    jvmArgs("-Djdk.net.unixdomain.tmpdir=${rootProject.projectDir.absolutePath}")
}
