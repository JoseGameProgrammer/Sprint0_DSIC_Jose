plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.jmmarter.btle_definitivo_jose"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.jmmarter.btle_definitivo_jose"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    // -----------------------------------------------------------------------------------------
    // El codigo Java NO vive en app/src/main/java, sino en ../../src/app_android/, porque la
    // entrega exige que el codigo de cada componente viva en src/<componente>/ y este proyecto
    // tiene tres: arduino_emisor, app_android y web.
    //
    // Gradle resuelve estas rutas relativas a la carpeta del modulo, que es app/. Por eso el
    // prefijo es ../../ (app/ -> BTLE_Definitivo_Jose/ -> raiz del repositorio).
    //
    // Los recursos (app/src/main/res) y el AndroidManifest.xml se quedan en su sitio, que es
    // donde el plugin de Android los busca por defecto.
    // -----------------------------------------------------------------------------------------
    sourceSets {
        getByName("main") {
            java.srcDirs("../../src/app_android/main/java")
        }
        getByName("test") {
            java.srcDirs("../../src/app_android/test/java")
        }
        getByName("androidTest") {
            java.srcDirs("../../src/app_android/androidTest/java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}