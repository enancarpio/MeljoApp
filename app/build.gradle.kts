import java.util.Properties

// 🔑 Carga las claves desde local.properties al inicio
val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { localProps.load(it) }
}

// Extraemos las variables
val groqKey: String = localProps.getProperty("GROQ_API_KEY") ?: ""
val witKey: String = localProps.getProperty("WIT_API_KEY") ?: ""
val gmapsKey: String = localProps.getProperty("GMAPS_API_KEY") ?: ""
val supabaseUrl: String = localProps.getProperty("SUPABASE_URL") ?: ""
val supabaseKey: String = localProps.getProperty("SUPABASE_API_KEY") ?: ""

// println("🔑 SUPABASE_API_KEY cargada: $supabaseKey")

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.meljo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.meljo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🔹 BuildConfig para usar en código
        buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")
        buildConfigField("String", "WIT_API_KEY", "\"$witKey\"")
        buildConfigField("String", "GMAPS_API_KEY", "\"$gmapsKey\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_API_KEY", "\"$supabaseKey\"")

        // 🔹 Placeholder para el AndroidManifest
        manifestPlaceholders["GMAPS_API_KEY"] = gmapsKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 🔹 Agrega estas dependencias:
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")

    // logs HTTP
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON con Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Google Play Services (ubicación y mapas)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")


}
