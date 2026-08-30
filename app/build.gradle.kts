import com.google.firebase.appdistribution.gradle.firebaseAppDistribution

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")
}

android {
    namespace = "com.chanbro.salim"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.chanbro.salim"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // 테스터 배포용. 디버그 키스토어의 SHA-1이 Firebase에 등록된 것과 같아서
            // 디버그 빌드로 배포해도 구글 로그인이 그대로 동작한다. 릴리스 키로 바꾸려면
            // 그 키의 SHA-1을 Firebase에 먼저 등록해야 로그인이 깨지지 않는다.
            firebaseAppDistribution {
                artifactType = "APK"
                // 상대 경로는 어느 프로젝트 기준인지 모호해서 루트 기준 절대 경로로 넘긴다.
                releaseNotesFile = rootProject.file("distribution/release-notes.txt").absolutePath
                // 기본값은 빈 값 — 아무에게도 알림이 가지 않는 업로드다.
                // 알릴 대상이 있을 때만 -PappDistributionGroups=<그룹> 으로 켠다.
                groups = providers.gradleProperty("appDistributionGroups").getOrElse("")
            }
        }
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
        compose = true
    }
}

dependencies {
    // 모듈
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core-ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    // 구글 로그인 (Credential Manager — 레거시 GoogleSignIn API는 deprecated이라 쓰지 않는다)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    // 상대방 연결 QR (PRD 9) — 코드 스캐너는 카메라 권한 없이 GMS가 UI를 제공한다
    implementation(libs.zxing.core)
    implementation(libs.play.services.code.scanner)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
