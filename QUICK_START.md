# Quick Start Guide

## 5-Minute Setup

### Prerequisites
- Android Studio installed
- Android device or emulator with Android 8.0+ (API 26+)
- Internet connection (for initial Pokemon load)

### Steps

1. **Open Project**
   - Launch Android Studio
   - Open the `Pokemon_Guess_Who` folder
   - Wait for Gradle sync (~2-3 minutes)

2. **Run App**
   - Click the green play button (▶) or press **Shift+F10**
   - Select your device/emulator
   - App installs and launches in ~30 seconds

3. **Play!**
   - Tap "Single Player"
   - Tap "Start New Game"
   - Start marking Pokemon cards

## Game Rules

### Objective
Identify which Pokemon your opponent is thinking of by asking strategic questions and eliminating cards.

### How to Play
1. **Select a Pokemon** - Tap any card to view it in the "Your Pokemon" section
2. **Ask a Question** - (In multiplayer) Ask opponent if their Pokemon matches your criteria
3. **Eliminate Cards** - Tap cards again to mark them with red X (based on opponent's answers)
4. **Hide Eliminated** - Use the "Hide X" button to declutter your board
5. **Change View** - Use grid buttons (1-6) to zoom in/out
6. **Win** - Narrow down to one remaining card and guess!

## UI Overview

### Top Section: Game Header
- **Title**: "Pokemon Guess Who"
- **Hide X Button**: Toggle visibility of eliminated cards
- **Grid Buttons**: Switch between 1-6 column layouts

### Middle Section: Your Pokemon
- Shows currently selected Pokemon
- Displays name and artwork
- Highlighted in yellow
- X button to deselect

### Bottom Section: Game Board
- Grid of all Pokemon cards
- Eliminated cards faded at bottom
- Tap to select/eliminate
- Responsive to grid size changes

## Features Explained

### Card Information
Each card shows:
- **Name**: Pokemon name
- **Image**: Official artwork
- **Type Badges**: Colored type labels (Fire, Water, etc.)
- **Stats**: HP, Attack, Defense

### Grid Layouts
- **1 Column**: Focus view - see one Pokemon at a time
- **2 Columns**: Compact view
- **3 Columns**: Standard view (recommended)
- **4-6 Columns**: See more cards at once
- Click any grid button to switch instantly

### Card States
- **Normal**: Blue outline, full opacity
- **Selected**: Yellow background, elevated
- **Eliminated**: Red X overlay, faded, moved to bottom

### Elimination System
- Tap a card to eliminate it
- Card gets red X and fades
- Moves to bottom of grid
- Toggle visibility with "Hide X" button

## Tips & Tricks

### Strategy
1. **Ask Type-Based Questions**: "Is it Fire type?" eliminates whole categories
2. **Ask Stat-Based Questions**: "Does it have high Attack?" narrows down
3. **Use Color Coding**: Types are color-coded for quick scanning
4. **Eliminate Systematically**: Work through one attribute at a time

### Performance
- App loads Pokemon data on first run (5-10 seconds)
- Subsequent games load instantly
- Smooth animations at 60fps
- Optimized image caching

### Controls
- **Double Tap**: Quick selection (future feature)
- **Long Press**: Show stats details (future feature)
- **Swipe**: Alternative navigation (future feature)

## Troubleshooting

### App Won't Load
**Problem**: Stuck on loading screen
**Solutions**:
- Check internet connection
- Ensure API is accessible (check PokeAPI status)
- Force close and restart app
- Clear app cache: Settings > Apps > Pokemon Guess Who > Storage > Clear Cache

### Cards Not Appearing
**Problem**: Blank cards without images
**Solutions**:
- Network issue - check connectivity
- Wait for images to load (may take a few seconds)
- Restart app
- Check available storage

### Crashes on Startup
**Problem**: App crashes immediately
**Solutions**:
- Ensure Android 8.0+ (API 26+)
- Update Android Studio
- Reinstall app: uninstall and run again
- Check Logcat for error messages

### Performance Issues
**Problem**: Slow, laggy gameplay
**Solutions**:
- Close other apps
- Enable Hardware Acceleration in AVD settings (emulator)
- Restart device
- Lower screen refresh rate if on high (120Hz+)

## Advanced Features

### Viewing Raw Stats
1. Select a Pokemon
2. Stats are visible on the card (HP, ATK, DEF)
3. In future: Full stats popup coming

### Type Filter (Future)
Once implemented, filter board by type:
1. Tap filter icon
2. Select type(s)
3. Board shows only matching Pokemon

### Game History (Future)
1. After game ends
2. View statistics
3. Replay previous games
4. Compare scores

## Multiplayer Setup (Coming Soon)

### Bluetooth
1. Enable Bluetooth on both devices
2. Host opens "Host Game"
3. Guest opens "Find Games"
4. Select host from list
5. Game syncs and begins

### WiFi
Similar to Bluetooth but over WiFi Direct (future feature)

## Settings (Coming Soon)

Future settings will include:
- Sound volume control
- Animations toggle
- Theme selection
- Difficulty level
- Pokemon generation selection

## Frequently Asked Questions

**Q: Can I play offline?**
A: Not yet - game requires internet for first load. Offline mode coming in v2.0.

**Q: How many Pokemon are in the game?**
A: Currently 151 (Generation I). More generations coming in future updates.

**Q: Can I play with friends?**
A: Multiplayer via Bluetooth coming in v1.1. WiFi and cloud options in v2.0.

**Q: Will my progress be saved?**
A: Game state is saved locally. Cloud save coming in v2.0.

**Q: Is there a time limit?**
A: Not in current version. Timed challenges coming in v2.0.

**Q: Can I customize the board?**
A: Basic layout options available. Full customization coming later.

**Q: What if the PokeAPI goes down?**
A: The app requires PokeAPI to function currently. Offline caching coming in v2.0.

## Keyboard Shortcuts (Android Studio)

If running from Android Studio:
- **Shift+F10**: Run app
- **Shift+F9**: Debug app
- **Ctrl+F10**: Hot reload (experimental)

## Getting Help

### In-App
- Check error messages carefully
- Test with different Pokemon
- Try different grid layouts

### Online
- [Android Developer Docs](https://developer.android.com)
- [PokeAPI Documentation](https://pokeapi.co/docs)
- [Jetpack Compose Guide](https://developer.android.com/jetpack/compose)

### Project Docs
- `README.md` - Project overview
- `DEVELOPMENT_SETUP.md` - Developer guide
- `ARCHITECTURE.md` - Technical details
- `FEATURES.md` - Roadmap

## Next Steps

### Try These
1. Start multiple games (random boards each time)
2. Switch between grid layouts
3. Eliminate cards and toggle visibility
4. Select different Pokemon for your section
5. Test on different device sizes

### Prepare for Future Updates
1. Watch for v1.1 Bluetooth announcement
2. Join community discussions
3. Provide feature feedback
4. Report bugs via proper channels

## Credits

### Technologies Used
- Jetpack Compose
- Retrofit & OkHttp
- Kotlin Coroutines
- Material Design 3
- Coil Image Loading

### Data Source
- [PokeAPI v2](https://pokeapi.co/)
- Pokemon official artwork

### Inspiration
- Classic Guess Who board game
- Pokemon Trading Card Game
- Mobile game design best practices

## License & Usage

This is a personal project for educational purposes.
Not for commercial distribution.
Pokemon is a trademark of Nintendo/The Pokemon Company.

## Version Info

- **Current Version**: 1.0
- **Min API Level**: 26 (Android 8.0)
- **Target API Level**: 34 (Android 14)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose

## Updates & Support

### Checking for Updates
Updates will be available from the same repository.
Check CHANGELOG.md for version history.

### Providing Feedback
- Feature requests: See FEATURES.md
- Bug reports: Via proper issue channels
- Suggestions: Community discussions

## Enjoy the Game! 🎮

Thank you for playing Pokemon Guess Who!
Have fun strategizing and may the best guesser win!
