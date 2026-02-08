# Development Setup Guide

## Getting Started

This guide will help you set up the Pokemon Guess Who project for development.

## Prerequisites

- **Android Studio**: Version Giraffe (2022.3.1) or newer
  - Download from: https://developer.android.com/studio
- **Android SDK**: API level 26 (API 34 recommended for latest features)
- **Java Development Kit (JDK)**: Java 8 or higher (OpenJDK 11+ recommended)
- **Gradle**: 8.0 or higher (included with Android Studio)

## Installation Steps

### 1. Clone/Download the Project

```bash
cd /path/to/Pokemon_Guess_Who
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Click "File" > "Open"
3. Navigate to the project directory
4. Click "Open"

### 3. Initial Gradle Sync

- Android Studio will automatically detect and sync the Gradle project
- If manual sync is needed: Click "File" > "Sync Now"
- Wait for the process to complete (may take 2-5 minutes)

### 4. Download Dependencies

All dependencies are automatically downloaded during Gradle sync:
- Jetpack Compose libraries
- Retrofit (networking)
- Coil (image loading)
- DataStore (preferences)
- Coroutines

### 5. Configure SDK

1. Go to "File" > "Project Structure"
2. Under "Project":
   - Set "Gradle JDK" to your installed JDK
   - Ensure "SDK Location" points to your Android SDK
3. Click "Apply" > "OK"

## Running the App

### On Emulator

1. Create/Select Android Virtual Device (AVD):
   - Click "Tools" > "Device Manager"
   - Create a device with Android 8.0 (API 26) or higher
   - Recommended: Pixel 4 or Pixel 5

2. Start the emulator:
   - Select your AVD and click the "Play" button

3. Run the app:
   - Click "Run" > "Run 'app'" (or Shift+F10)
   - Select your running emulator

### On Physical Device

1. Enable Developer Mode:
   - Go to Settings > About phone
   - Tap "Build number" 7 times
   - Go to Settings > Developer options
   - Enable "USB Debugging"

2. Connect via USB:
   - Plug in your Android device
   - Authorize USB debugging on the device

3. Run the app:
   - Click "Run" > "Run 'app'" (or Shift+F10)
   - Select your connected device

## Project Structure

### Source Code

```
app/src/main/java/com/example/pokemonguesswho/
├── MainActivity.kt              # Entry point
├── audio/
│   └── SoundManager.kt          # Audio handling
├── data/
│   ├── GamePokemon.kt           # Game data models
│   ├── PokemonData.kt           # API response models
│   ├── PokemonViewModel.kt      # MVVM ViewModel
│   └── PreferencesManager.kt    # Local storage
├── game/
│   └── GameManager.kt           # Game logic
├── network/
│   ├── PokemonApiClient.kt      # Retrofit setup
│   ├── PokemonApiService.kt     # API endpoints
│   └── bluetooth/
│       └── BluetoothConnectionManager.kt
└── ui/
    ├── Navigation.kt            # Screen navigation
    ├── Theme.kt                 # Material Design theme
    ├── components/              # Reusable UI components
    │   ├── PokemonCard.kt
    │   └── PokemonCardComponent.kt
    └── screens/                 # Full screens
        ├── GameScreenUpdated.kt
        └── MainMenuScreen.kt
```

### Resources

```
app/src/main/res/
├── values/
│   ├── strings.xml              # String resources
│   ├── colors.xml               # Color definitions
│   └── dimens.xml               # Dimension constants
├── xml/
│   ├── network_security_config.xml
│   ├── backup_rules.xml
│   └── data_extraction_rules.xml
├── mipmap-*/                    # App icons (add your own)
└── raw/                         # Sound files (optional)
```

## Build Configuration

### build.gradle.kts (App)

Key dependencies:
- `androidx.compose.ui:ui` - Jetpack Compose UI
- `androidx.lifecycle:lifecycle-viewmodel-compose` - MVVM
- `com.squareup.retrofit2:retrofit` - HTTP client
- `io.coil-kt:coil-compose` - Image loading
- `androidx.navigation:navigation-compose` - Navigation

### AndroidManifest.xml

Permissions configured:
- `INTERNET` - For API calls
- `BLUETOOTH*` - For multiplayer (Android 12+)
- `ACCESS_FINE_LOCATION` - For Bluetooth discovery
- `CHANGE_NETWORK_STATE` - For WiFi connectivity

## Building the Project

### Debug Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```

Note: Requires signing configuration

## Cleaning and Rebuilding

If you encounter build issues:

```bash
# Clean build cache
./gradlew clean

# Invalidate Android Studio cache
File > Invalidate Caches > Invalidate and Restart

# Rebuild project
./gradlew build
```

## Debugging

### Using Android Studio Debugger

1. Set breakpoints by clicking on line numbers
2. Click "Run" > "Debug 'app'" (or Shift+F9)
3. App will pause at breakpoints
4. Use Step Over/Into to navigate

### Viewing Logs

1. Open Logcat: Click "View" > "Tool Windows" > "Logcat"
2. Filter by app name: Search for "pokemonguesswho"
3. Use different log levels: Verbose, Debug, Info, Warn, Error

### Common Issues

**Build fails with "Cannot find symbol"**
- Run Gradle sync: File > Sync Now
- Invalidate cache: File > Invalidate Caches

**Emulator is slow**
- Enable Hardware Acceleration in AVD settings
- Use Pixel 4 or smaller device
- Allocate more RAM in AVD settings

**App crashes on startup**
- Check Logcat for stack trace
- Ensure internet connection for API calls
- Verify SDK version (min API 26)

## Hot Reload

Jetpack Compose supports compose-compatible hot reload:
1. Make code changes
2. Press Ctrl+F10 (Windows/Linux) or Cmd+\ (Mac)
3. Changes preview instantly (when possible)

## Performance Monitoring

### Profiler

Click "View" > "Tool Windows" > "Profiler"

Monitor:
- CPU usage
- Memory allocation
- Network activity
- Battery drain

## Dependencies and Versions

See `app/build.gradle.kts` for:
- Compile SDK version (34)
- Target SDK version (34)
- Minimum SDK version (26)
- All dependency versions

To update dependencies:
1. Edit `build.gradle.kts`
2. Sync Gradle
3. Test thoroughly

## Testing

### Unit Tests

Tests go in `app/src/test/java/`

Run:
```bash
./gradlew testDebug
```

### Instrumented Tests

Tests go in `app/src/androidTest/java/`

Run:
```bash
./gradlew connectedAndroidTest
```

## Signing APK

For release builds, create a keystore:

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

Add to `build.gradle.kts`:
```kotlin
signingConfigs {
    release {
        storeFile = file("my-release-key.jks")
        storePassword = "password"
        keyAlias = "my-key-alias"
        keyPassword = "password"
    }
}
```

## Next Steps

1. Familiarize yourself with Jetpack Compose
2. Understand the ViewModel architecture
3. Explore the API integration with Retrofit
4. Run the app and test gameplay
5. Add sound effects (see SOUND_ASSETS_GUIDE.txt)
6. Implement Bluetooth multiplayer

## Resources

- **Android Development**: https://developer.android.com
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Kotlin**: https://kotlinlang.org
- **PokeAPI**: https://pokeapi.co/docs/v2
- **Material Design 3**: https://m3.material.io

## Support

For issues:
1. Check the README.md
2. Search Android Studio documentation
3. Review error logs in Logcat
4. Check PokeAPI status: https://pokeapi.co
