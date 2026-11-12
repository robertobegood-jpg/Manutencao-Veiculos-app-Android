import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    // Prints automáticos das telas nos testes (revisão visual antes de cada versão)
    id("app.cash.paparazzi")
}

android {
    namespace = "com.roberto.manutencao"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.roberto.manutencao"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.2"
    }

    // Assinatura oficial do app. A chave NÃO fica no repositório:
    // - no GitHub Actions vem dos secrets KEYSTORE_BASE64 e KEYSTORE_PASSWORD;
    // - no computador, coloque o arquivo em app/assinatura/manutencao.jks
    //   e a senha em keystore.properties (senha=...), ambos ignorados pelo git.
    // Sem a chave (ex.: quem clonar o projeto), o build usa a chave de debug padrão.
    val chave = file("assinatura/manutencao.jks")
    val senhaChave: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: rootProject.file("keystore.properties").takeIf { it.exists() }?.let { arq ->
            Properties().apply { arq.inputStream().use { load(it) } }.getProperty("senha")
        }
    val temAssinatura = chave.exists() && senhaChave != null

    signingConfigs {
        if (temAssinatura) {
            create("oficial") {
                storeFile = chave
                storePassword = senhaChave
                keyAlias = "manutencao"
                keyPassword = senhaChave
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (temAssinatura) signingConfig = signingConfigs.getByName("oficial")
        }
        debug {
            if (temAssinatura) signingConfig = signingConfigs.getByName("oficial")
        }
    }

    lint {
        // Avisos de lint não devem impedir a geração do APK de release.
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Banco de dados local
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Verificação diária em segundo plano
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // QR Code do Pix (cafezinho)
    implementation("com.google.zxing:core:3.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
