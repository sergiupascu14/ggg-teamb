plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "com.example.teamb"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.teamb"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

// --- Coverage (JaCoCo) -------------------------------------------------------
// Targets testable logic; UI composables, generated code and DI glue are excluded
// so the 90% gate reflects real logic coverage. Run `./gradlew jacocoCoverageVerification`.
private val coverageExclusions = listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*Test*.*", "**/databinding/**", "**/generated/**",
    // Room generated implementations
    "**/*_Impl*.*", "**/*_Factory*.*",
    // Pure Compose UI + theme + navigation scaffolding
    "**/ui/**/*Screen*.*", "**/ui/**/*ScreenKt*.*", "**/ui/theme/**",
    "**/ui/components/**", "**/ui/navigation/**",
    "**/*ComposableSingletons*.*", "**/MainActivity*.*",
    // DI / Application wiring
    "**/AppContainer*.*", "**/TeamBApp*.*",
    // Android-framework-bound integration (DataStore, EncryptedSharedPreferences, WorkManager,
    // notifications, Firebase, asset loaders) — exercised on-device, not in JVM unit tests.
    // PasswordHasher (pure) stays IN scope; only the Android store impls are excluded.
    "**/data/datastore/DataStoreProfileStore*.*", "**/data/datastore/ProfileStoreKt*.*",
    "**/data/datastore/EncryptedCredentialStore*.*",
    "**/data/db/AppDatabase*.*",
    "**/data/community/FirebaseCommunityRepository*.*",
    "**/data/desk/DeskAllocationRepository\$Companion*.*",
    "**/data/integration/MockGarminAdDirectoryService*.*",
    "**/notification/**",
    "**/data/util/SystemClock*.*"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(coverageExclusions)
    }
    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("**/testDebugUnitTest.exec", "**/jacoco/testDebugUnitTest.exec")
        }
    )
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jacocoTestReport")
    group = "verification"
    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(coverageExclusions)
    }
    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("**/testDebugUnitTest.exec", "**/jacoco/testDebugUnitTest.exec")
        }
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

// Enforce the 90% gate as part of `check` so it fails the build when coverage regresses.
tasks.named("check") { dependsOn("jacocoCoverageVerification") }
