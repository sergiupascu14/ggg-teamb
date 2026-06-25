# TeamB

Proiect Android de bază, scris în Kotlin, folosind Jetpack Compose și Material 3.

## Stack

- **Limbaj:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Build:** Gradle (Kotlin DSL) cu version catalog (`gradle/libs.versions.toml`)
- **minSdk:** 24 · **targetSdk / compileSdk:** 35
- **JDK necesar:** 17–21 (AGP 8.7 **nu** suportă JDK 25 — vezi nota de mai jos)

## Structură

```
app/
  src/main/
    java/com/example/teamb/
      MainActivity.kt          # Activity-ul de pornire (Compose)
      ui/theme/                # Theme, culori, tipografie
    res/                       # Resurse (strings, icons, themes)
    AndroidManifest.xml
  src/test/                    # Unit tests
  src/androidTest/             # Instrumented tests
  build.gradle.kts
build.gradle.kts               # Config rădăcină
settings.gradle.kts
gradle/libs.versions.toml      # Versiuni dependențe
```

## Firebase Setup

Communitatea (newsfeed, voturi, leaderboard) folosește **Firebase Realtime Database** (proiect `clooj-ggg`).
Consultă **[README-firebase.md](README-firebase.md)** pentru:
- Cum obții `google-services.json` și îl plasezi local
- Cum aplici regulile de securitate RTDB din `database.rules.json`
- Cum pornești Firebase Emulator și încarci date de demo cu `scripts/seed.json`

---

## Rulare

1. Deschide proiectul în **Android Studio** (Hedgehog sau mai nou). Studio va
   genera automat `local.properties` cu calea către Android SDK.
2. Sau, din linie de comandă. **Important:** `java` implicit pe această mașină
   este JDK 25, incompatibil cu AGP 8.7 — folosește JDK 17–21:

   ```sh
   export ANDROID_HOME="$HOME/Library/Android/sdk"
   export JAVA_HOME="$(/usr/libexec/java_home -v 21)"

   ./gradlew assembleDebug      # build APK debug
   ./gradlew installDebug       # instalează pe device/emulator conectat
   ./gradlew testDebugUnitTest  # unit tests
   ```

## Status verificat

Pe această mașină au fost instalate și testate:

- Android SDK la `~/Library/Android/sdk` (platform 35, build-tools 35.0.0, platform-tools)
- `local.properties` generat (gitignored)
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**, APK: `app/build/outputs/apk/debug/app-debug.apk`
- `./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL**

### Ca să rulezi efectiv aplicația

Nu există încă device fizic conectat sau emulator (AVD). Alege una:

- **Device fizic:** activează USB debugging, conectează-l, apoi `./gradlew installDebug`.
- **Emulator:** instalează un system image și creează un AVD:

  ```sh
  sdkmanager "system-images;android-35;google_apis;arm64-v8a" "emulator"
  avdmanager create avd -n Pixel35 -k "system-images;android-35;google_apis;arm64-v8a" -d pixel
  emulator -avd Pixel35 &        # pornește emulatorul
  ./gradlew installDebug         # instalează aplicația
  ```
