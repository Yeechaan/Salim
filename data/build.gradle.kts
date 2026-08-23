plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.chanbro.salim.data"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    // 온보딩 완료 플래그 등 로컬 전용 데이터 (CLAUDE.md 2번 data/local)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
}
