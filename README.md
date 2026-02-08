# Pokemon Guess Who - Android Game

A modern Android game implementation of Guess Who featuring Pokemon. Players can battle locally via Bluetooth/WiFi, with a beautiful UI, smooth animations, and immersive sound effects.

## Features

- **Single Player**: Play against the board with 24 random Pokemon
- **Multiplayer**: Connect with other players via Bluetooth or WiFi (coming soon)
- **Dynamic Card Layout**: Switch between 1-wide, 2-wide, 3-wide, up to 6-wide grid views
- **Pokemon Stats**: View type, HP, Attack, Defense, and other base stats
- **Card Elimination**: Tap cards to mark them with red X when eliminated
- **Toggle View**: Hide/show eliminated cards to reduce clutter
- **Responsive Design**: Modern Material Design 3 UI with Jetpack Compose
- **Smooth Animations**: Elegant transitions and visual feedback
- **Real Pokemon Data**: Fetches data from PokéAPI

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with StateFlow
- **Networking**: Retrofit 2 + OkHttp
- **Image Loading**: Coil
- **Bluetooth**: Android Bluetooth API
- **Local Storage**: DataStore Preferences
- **API**: PokeAPI v2

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/pokemonguesswho/
│   │   │   ├── MainActivity.kt              # App entry point
│   │   │   ├── audio/
│   │   │   │   └── SoundManager.kt          # Sound effects
│   │   │   ├── data/
│   │   │   │   ├── GamePokemon.kt           # Game Pokemon model
│   │   │   │   ├── PokemonData.kt           # API data models
│   │   │   │   ├── PokemonViewModel.kt      # MVVM ViewModel
│   │   │   │   └── PreferencesManager.kt    # Local storage
│   │   │   ├── game/
│   │   │   │   └── GameManager.kt           # Game logic
│   │   │   ├── network/
│   │   │   │   ├── PokemonApiClient.kt      # Retrofit client
│   │   │   │   ├── PokemonApiService.kt     # API interface
│   │   │   │   └── bluetooth/
│   │   │   │       └── BluetoothConnectionManager.kt
│   │   │   └── ui/
│   │   │       ├── Navigation.kt            # Compose navigation
│   │   │       ├── components/
│   │   │       │   ├── PokemonCard.kt       # Card components
│   │   │       │   └── PokemonCardComponent.kt
│   │   │       └── screens/
│   │   │           ├── GameScreen.kt        # Main game screen
│   │   │           └── MainMenuScreen.kt    # Menu screen
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── dimens.xml
│   │   │   ├── xml/
│   │   │   │   ├── network_security_config.xml
│   │   │   │   ├── backup_rules.xml
│   │   │   │   └── data_extraction_rules.xml
│   │   │   └── mipmap-*/
│   │   │       └── ic_launcher.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── proguard-rules.pro
└── build.gradle.kts
```

## Setup Instructions

### Prerequisites

- Android Studio Giraffe (2022.3.1) or newer
- Android SDK 26 (API level 26) or higher
- Java 8 or higher
- Gradle 8.0+

### Installation

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd Pokemon_Guess_Who
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the project directory and click "Open"

3. **Build the project**:
   - Wait for Gradle sync to complete
   - Click "Build" > "Make Project" or press Ctrl+F9

4. **Run on emulator or device**:
   - Connect an Android device or start an emulator
   - Click "Run" > "Run 'app'" or press Shift+F10

## How to Play

### Single Player Mode

1. Tap "Single Player" on the main menu
2. Tap "Start New Game" to generate a new board with 24 random Pokemon
3. **Select a Pokemon**: Tap any card to select it (it will be highlighted in yellow)
4. **Eliminate Cards**: Tap cards to mark them as eliminated
   - First tap: Card gets red X overlay and fades
   - Eliminated cards move to the bottom of the list
5. **Toggle View**: Use the "Hide X" button to hide eliminated cards
6. **Change Grid Layout**: Tap the column number buttons (1-6) to adjust grid size

### Multiplayer Mode (Coming Soon)

- Connect with another player via Bluetooth or local WiFi
- Both players get the same board
- Alternate turns asking questions
- Find the matching Pokemon before your opponent

## Controls

| Control | Action |
|---------|--------|
| Tap Card | Select and mark as eliminated |
| "Hide X" Button | Toggle visibility of eliminated cards |
| Grid Buttons (1-6) | Change number of columns |
| Deselect (X) | Clear selected Pokemon from top section |

## Pokemon Data

The app fetches Pokemon data from [PokéAPI](https://pokeapi.co/), including:
- Pokemon name and ID
- Official artwork images
- Type classification (Fire, Water, Electric, etc.)
- Base stats (HP, Attack, Defense, Sp. Atk, Sp. Def, Speed)
- Abilities

## Features in Detail

### Responsive Grid Layout

Switch between different column layouts:
- **1-Column**: Focus on one Pokemon at a time
- **2-Column**: Desktop-like view
- **3-Column**: Balanced view
- **4-6 Columns**: See all cards at once

### Type-Based Colors

Each Pokemon type has its own color scheme matching official Pokemon games:
- Fire 🔥: Orange
- Water 💧: Blue
- Electric ⚡: Yellow
- Grass 🌿: Green
- Psychic: Pink
- Dragon: Purple
- And more...

### Card Features

Each card displays:
- Pokemon name
- Official artwork
- Type badges
- Base stats (HP, Attack, Defense)

## Architecture

The app follows MVVM (Model-View-ViewModel) architecture:

- **Model**: `GamePokemon`, `Pokemon`, `GameState`
- **ViewModel**: `PokemonViewModel` manages state and API calls
- **View**: Jetpack Compose UI components

State management uses `StateFlow` for reactive updates.

## Networking

### API Integration

Retrofit client connects to PokéAPI v2:
- Base URL: `https://pokeapi.co/api/v2/`
- Automatic retry logic with OkHttp
- Timeout: 15 seconds

### Bluetooth (Coming Soon)

- Supports Android 12+ (API 31+) Bluetooth permissions
- Uses UUID `12345678-1234-1234-1234-123456789012`
- Service name: `PokemonGuessWho`

## Future Enhancements

- [ ] Complete Bluetooth multiplayer implementation
- [ ] WiFi Direct connectivity option
- [ ] Sound effects for card interactions
- [ ] Game statistics and leaderboard
- [ ] Different Pokemon generation options
- [ ] Difficulty levels
- [ ] Animation polish and particle effects
- [ ] Accessibility improvements

## Troubleshooting

### Build Issues

- **Gradle sync failed**: Try "File" > "Invalidate Caches" > "Invalidate and Restart"
- **Missing dependencies**: Run "Sync Now" in the Gradle notification
- **Compilation errors**: Ensure Android SDK API 34 is installed

### Runtime Issues

- **No Pokemon loading**: Check internet connection, may be PokeAPI rate limit
- **App crashes**: Check logcat with `adb logcat` for detailed error messages
- **Bluetooth not working**: Ensure Bluetooth is enabled and permissions are granted

## Permissions

The app requests the following permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## API Rate Limiting

PokéAPI has rate limits. The app includes:
- Connection timeout: 15 seconds
- Read timeout: 15 seconds
- Caching via local ViewModel

If you encounter rate limiting, consider implementing local data storage.

## Copyright Notice

This is a personal project for educational purposes. Pokemon artwork and data are used for non-commercial use only.

## License

This project is for personal use only and is not published.

## Support

For issues or questions, please check:
- Android Studio documentation: https://developer.android.com/docs
- PokeAPI documentation: https://pokeapi.co/docs/v2
- Jetpack Compose docs: https://developer.android.com/jetpack/compose
