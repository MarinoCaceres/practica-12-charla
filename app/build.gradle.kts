import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Tu sala sale de local.properties, que no se sube al repo:
//   charla.servidor=http://10.0.2.2:8000     el emulador, a tu computadora (el valor por omisión)
//   charla.clave=la-misma-del-.env           con ella, la app entra a tu sala como anfitrión
val local = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val servidorPropio: String = local.getProperty("charla.servidor") ?: "http://10.0.2.2:8000"
val claveAnfitrion: String = local.getProperty("charla.clave") ?: ""

android {
    namespace = "mx.tec.charla"
    compileSdk = 37

    defaultConfig {
        applicationId = "mx.tec.charla"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SERVIDOR_PROPIO", "\"$servidorPropio\"")
        buildConfigField("String", "CLAVE_ANFITRION", "\"$claveAnfitrion\"")
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // Hilt (Práctica 9): la librería, el generador de código, y su pieza para ViewModel
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // El QR: ZXing lo dibuja; el escáner de Google lo lee con la cámara;
    // ML Kit lo lee de una imagen (viene dentro de la app: funciona sin descargar nada)
    implementation(libs.zxing.core)
    implementation(libs.play.services.code.scanner)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.ui.tooling)
}
