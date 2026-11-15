# Boccia Android App

Kotlin + Jetpack Compose app for the BadiBoccia league; the Android counterpart of the iOS app in
`../badiboccia-ios`, using the same website API (`../badiboccia`, `/api/v1`,
`src/Controllers/ApiController.php`). Screens, rules and wording follow the iOS app; when in doubt,
read the matching Swift file there.

## Build & run

- Gradle wrapper, AGP 9.5, Kotlin 2.4 (AGP's built-in Kotlin), Compose BOM, compileSdk/targetSdk 37, minSdk 29. Versions in `gradle/libs.versions.toml`.
- Gradle runs on Android Studio's JDK:

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest
./gradlew installDebug   # onto the running emulator / phone
```

- Emulator: AVD `Medium_Phone_API_37.0` (`~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_37.0`).
- App language for testing: `adb shell cmd locale set-app-locales swiss.reutener.boccia --locales de-CH` (or `en-GB`).
- Debug builds default to DEV and can switch server with the gear icon on the overview; release builds only use production. `LOCAL` is the website's `./dev/serve.sh` as the emulator sees the Mac (`http://10.0.2.2:8090`, cleartext allowed in debug only).

## Architecture

- Same shape as iOS: no per-screen view models. One `AppModel` (`app/AppModel.kt`, an `AndroidViewModel` with Compose state) holds server, seasons, loaded `SeasonDetail`s and does all API calls; screens read it via `LocalAppModel`.
- The team login (`net/SessionStore.kt`) is stored per server, encrypted with an Android Keystore key (the counterpart of the iOS Keychain).
- **Demo season**: like on iOS, the website's "Demo Cup" isn't in `/seasons`; `/api/v1` gives its id and public PIN. "Demo ausprobieren" under My Team lists its teams; while a demo team is logged in, `AppModel.isDemoSession` puts the demo season into `seasons` as the `currentSeason`, and `ServerIndicator` shows the mint "DEMO" marker.
- `LocalNavigator` opens the game and team pages from anywhere (`app/BocciaApp.kt`, Navigation Compose with type-safe routes in `app/Navigation.kt`).
- `model/ApiModels.kt` mirrors the API JSON (snake_case via `JsonNamingStrategy.SnakeCase`); **keep it in sync with `ApiController.php` and the iOS `APIModels.swift`**.
- `model/Domain.kt`: derived logic (Swiss time, game rules, sort orders); `ui/Labels.kt`: texts derived from the model (game titles, placeholders, dates, error messages). Dates and times are Swiss local time.
- `ui/bracket/BracketLayout.kt`: pure bracket geometry (unit-tested), like iOS's `BracketLayout.swift`.
- Look: dark blue gradient and dark cards (`ui/theme/Theme.kt`), dark only, accent `Theme.qualifier`.

## Conventions

- **Localization**: German is the source language (Swiss German, never "ß") in `res/values/strings.xml`, English in `res/values-en/strings.xml`. Every new string needs both. Use the same German and English wording as the iOS app (`Localizable.xcstrings`) and the website (`../badiboccia/lang/en.php`, `lang/glossary.json`).
- Business rules must match the website and the iOS app.
- Tests: JUnit in `app/src/test`, fixtures = real API responses (same files as the iOS tests).
- Commit messages: gitmoji format without env tag (`✨: Add …`); commit only when asked.
