# Testing Guide

## Overview

This guide provides instructions for testing the Pokemon Guess Who application across different scenarios and platforms.

## Prerequisite Testing Setup

### Prerequisites

- Android Studio with Giraffe or newer
- Android SDK 26+ installed
- At least one of:
  - Android Virtual Device (Emulator)
  - Physical Android device with USB debugging enabled
- Internet connection (for PokeAPI)

### Initial Setup

1. **Open Project**: Load project in Android Studio
2. **Gradle Sync**: Let Gradle build and sync
3. **Select Device**: Choose emulator or physical device
4. **Run App**: Click Run button or Shift+F10

## Manual Testing Checklist

### 1. Startup & Loading

- [ ] App launches without crashing
- [ ] Main menu screen displays correctly
- [ ] App title "Pokemon Guess Who" is visible
- [ ] Buttons are clickable
- [ ] Layout is responsive on different screen sizes

### 2. Loading Screen

- [ ] "Start New Game" button works
- [ ] Loading indicator appears while fetching Pokemon
- [ ] Pokemon data loads within 10 seconds
- [ ] Error message displays if network unavailable

### 3. Game Screen

**Board Display**:
- [ ] Game board appears with cards in grid
- [ ] All cards are visible initially
- [ ] Cards have Pokemon names
- [ ] Pokemon images load from API
- [ ] Type badges display correctly
- [ ] Stats (HP, ATK, DEF) show on each card

### 4. Card Selection

**Selecting Cards**:
- [ ] Tap card → card highlights in yellow
- [ ] Tapped card appears in "Your Pokemon" section
- [ ] Multiple taps switch selection between cards
- [ ] Selection shows full Pokemon artwork

**Your Pokemon Section**:
- [ ] Section displays "Your Pokemon" label
- [ ] Selected Pokemon name displays
- [ ] Pokemon image displays with proper scaling
- [ ] X button deselects Pokemon
- [ ] Section updates when selecting new card

### 5. Card Elimination

**Marking as Eliminated**:
- [ ] Tapping eliminated card shows red X overlay
- [ ] Card fades (reduced opacity ~50%)
- [ ] Animation is smooth (no stuttering)
- [ ] Card remains on board initially

**Moving Eliminated Cards**:
- [ ] Eliminated cards move to bottom of grid
- [ ] Non-eliminated cards stay in original position
- [ ] Multiple eliminations work correctly
- [ ] Order of eliminated cards is logical (bottom of list)

### 6. Toggle Eliminated Cards

**Hide Eliminated**:
- [ ] "Hide X" button toggles state
- [ ] Button highlights when active
- [ ] Eliminated cards disappear from view
- [ ] Remaining cards reflow to fill space

**Show Eliminated**:
- [ ] Clicking again shows eliminated cards
- [ ] Cards reappear at bottom
- [ ] Previous state is preserved
- [ ] Animation is smooth

### 7. Grid Layout Switching

**Layout Options**:
- [ ] 1-column layout shows cards vertically
- [ ] 2-column layout shows 2 cards per row
- [ ] 3-column layout shows 3 cards per row
- [ ] 4, 5, 6-column layouts all work
- [ ] Buttons highlight current layout

**Switching Layouts**:
- [ ] Layout changes instantly
- [ ] Cards reflow smoothly
- [ ] No cards are lost during switch
- [ ] Eliminated cards remain at bottom
- [ ] Selected Pokemon section persists

### 8. Visual Design

**Colors & Typography**:
- [ ] Material Design 3 colors are correct
- [ ] Text is readable and properly sized
- [ ] Type badges have correct colors
- [ ] Cards have proper shadows/elevation

**Responsive Design**:
- [ ] Layout works on small phones (4.5")
- [ ] Layout works on large phones (6"+)
- [ ] Layout works on tablets (7"+)
- [ ] Landscape orientation supported (if applicable)

### 9. Animations

**Card Animations**:
- [ ] Selection fade/scale animation is smooth
- [ ] Elimination red X appears smoothly
- [ ] Layout transition animates properly
- [ ] No animation stuttering or jank

**Performance**:
- [ ] Animations run at 60 FPS
- [ ] App remains responsive during animation
- [ ] No memory leaks from animations

### 10. Navigation

**Screen Navigation**:
- [ ] Start New Game button → game screen
- [ ] Back button returns to menu (if implemented)
- [ ] Menu shows after game (if implemented)
- [ ] Navigation preserves state

## Test Scenarios

### Scenario 1: Fresh Install

1. Install app on clean device
2. Launch app
3. Observe loading (5-10 seconds)
4. Verify Pokemon load correctly
5. Check all 151 Pokemon load (or appropriate subset)

**Expected Result**: ✅ App loads, Pokemon display

### Scenario 2: Gameplay Session

1. Start new game
2. Tap 5 different cards
3. Mark 10 cards as eliminated
4. Switch to 4-column layout
5. Toggle eliminate visibility
6. Select a new Pokemon

**Expected Result**: ✅ All actions work smoothly

### Scenario 3: Multiple Games

1. Play game 1 (mark 15 cards)
2. Return to menu
3. Start game 2
4. Verify new random board generated
5. Verify previous eliminations don't carry over

**Expected Result**: ✅ Each game is independent

### Scenario 4: Network Interruption

1. Enable airplane mode while loading
2. Observe error message
3. Disable airplane mode
4. Restart app
5. Verify Pokemon load

**Expected Result**: ✅ Graceful error handling

### Scenario 5: Low Memory

1. Open app with limited RAM (emulator setting)
2. Play normal game
3. Mark cards as eliminated
4. Switch layouts
5. Monitor for crashes

**Expected Result**: ✅ App handles low memory

### Scenario 6: Different Screen Sizes

Test on various devices:
- Emulator: Pixel 4 (5.7")
- Emulator: Pixel 3a (5.6")
- Emulator: Nexus 7 (7" tablet)
- Physical device (various sizes)

**Expected Result**: ✅ UI scales correctly

## Automated Testing Setup

### Unit Tests

**Location**: `app/src/test/java/`

**Test GameManager**:
```kotlin
@Test
fun testPokemonElimination() {
    val pokemon = GamePokemon(
        pokemonId = 1,
        name = "Bulbasaur",
        imageUrl = "url",
        types = listOf("Grass", "Poison")
    )
    
    val result = gameManager.togglePokemonElimination(pokemon)
    assertTrue(result.isEliminated)
}
```

**Run Tests**:
```bash
./gradlew test
```

### Instrumented Tests

**Location**: `app/src/androidTest/java/`

**Test Composables**:
```kotlin
@Rule
val composeTestRule = createComposeRule()

@Test
fun testPokemonCardDisplay() {
    composeTestRule.setContent {
        PokemonCardComponent(
            pokemon = testPokemon,
            onCardClick = {}
        )
    }
    
    composeTestRule
        .onNodeWithText("Bulbasaur")
        .assertIsDisplayed()
}
```

**Run Tests**:
```bash
./gradlew connectedAndroidTest
```

## Performance Testing

### Memory Profiling

1. **Open Profiler**: Tools > Profiler
2. **Monitor Memory**:
   - Record baseline at startup
   - Play game (30 seconds)
   - Toggle layouts (10 times)
   - Toggle eliminations (20 times)
3. **Check Metrics**:
   - Peak memory < 200MB
   - No continuous growth
   - GC events reasonable

### CPU Profiling

1. **Trace CPU**:
   - Open Profiler
   - Start CPU recording
   - Perform UI interactions
   - Stop recording
2. **Analyze**:
   - Main thread usage reasonable
   - No long frames (>16ms = 60fps drop)
   - Smooth 60 FPS rendering

### Network Monitoring

1. **Open Network Profiler**:
   - Profiler > Network tab
2. **Monitor**:
   - Initial load: 5-10 requests
   - Data transferred: ~2-3 MB
   - Load time: < 10 seconds
   - No repeated requests

## Device Testing

### Test Devices

| Device | OS | Screen | Notes |
|--------|----|---------| ----|
| Pixel 3a Emulator | 11 | 5.6" | Standard phone |
| Pixel 5 Emulator | 12 | 6.0" | Current flagship |
| Nexus 7 Emulator | 10 | 7.0" | Tablet |
| Physical Phone | 13+ | Varies | Real device |

### Test Configurations

- [ ] Min API 26 device
- [ ] Target API 34 device
- [ ] Portrait orientation
- [ ] Landscape orientation (if supported)
- [ ] Light theme
- [ ] Dark theme

## Edge Cases

### Test Edge Cases

1. **Empty/Error States**:
   - No internet connection
   - API unavailable
   - Corrupted image data
   - Empty Pokemon list

2. **User Actions**:
   - Rapid card tapping
   - Rapid layout switching
   - Select + Eliminate + Hide same card
   - Back button during load

3. **Device States**:
   - Low battery mode
   - Low memory
   - Rotation changes
   - Lock/unlock screen

4. **Data Edge Cases**:
   - Pokemon with no image
   - Missing stats
   - Unusual type combinations
   - Very long Pokemon names

## Accessibility Testing

### Accessibility Checks

- [ ] All buttons have content descriptions
- [ ] Text has sufficient contrast (WCAG AA)
- [ ] Touch targets >= 48dp (per Material Design)
- [ ] No information only conveyed by color
- [ ] Keyboard navigation works

### Test with Accessibility Tools

1. Enable "Accessibility Scanner" app
2. Run on game screen
3. Review recommendations
4. Fix critical issues

## Localization Testing

### Multi-Language Support (Future)

- [ ] Test with different language settings
- [ ] Verify text layout adjusts
- [ ] Check RTL languages (Arabic, Hebrew)
- [ ] Monitor for text overflow

## Beta Testing Checklist

### Pre-Release

- [ ] All manual tests pass
- [ ] All automated tests pass
- [ ] Performance metrics acceptable
- [ ] No crashes in 30-minute session
- [ ] All UI elements visible
- [ ] All interactions responsive

### Release Checklist

- [ ] Version number updated
- [ ] CHANGELOG.md updated
- [ ] README.md current
- [ ] Build succeeds with no warnings
- [ ] APK size acceptable
- [ ] No sensitive data in code

## Regression Testing

### After Updates

1. **Run Full Test Suite**:
   ```bash
   ./gradlew build
   ```

2. **Manual Verification**:
   - [ ] All features still work
   - [ ] No new bugs introduced
   - [ ] Performance not degraded
   - [ ] Visual design intact

3. **Device Testing**:
   - Test on multiple devices
   - Verify backward compatibility

## Troubleshooting Testing Issues

### Common Issues

**App crashes on startup**
- Check Logcat for stack trace
- Verify internet connection
- Clear app data and reinstall

**Images don't load**
- Check network connection
- Verify PokeAPI is accessible
- Check image URLs in Logcat

**Performance issues**
- Check device memory (Profiler)
- Verify not running other apps
- Test on different device

**Tests fail**
- Ensure Android SDK installed
- Verify device/emulator running
- Check test configuration

## Continuous Integration (Future)

### Setup GitHub Actions

```yaml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
      - run: ./gradlew build
      - run: ./gradlew test
```

## Test Report Template

### Test Session Report

**Date**: [Date]
**Tester**: [Name]
**Device**: [Model & OS]
**Version**: [App version]

**Tests Run**: [#]/[Total]
**Passed**: [#]
**Failed**: [#]
**Skipped**: [#]

**Issues Found**:
1. [Issue description]
2. [Issue description]

**Notes**: [Any observations]

## Success Criteria

✅ **Minimum Requirements**:
- All features work as designed
- No crashes in extended gameplay
- Load time < 15 seconds
- UI responsive on all device sizes
- Proper error handling

✅ **Performance**:
- Memory usage < 200MB peak
- 60 FPS animations
- Smooth transitions
- Quick layout switching

✅ **Quality**:
- Clean code
- Proper error handling
- Comprehensive docs
- Accessibility compliant

## Next Steps

1. **Run Manual Tests**: Use checklist above
2. **Perform Performance Testing**: Use Profiler
3. **Test on Multiple Devices**: Ensure compatibility
4. **Document Issues**: Report findings
5. **Fix & Retest**: Iterate on issues
6. **Release When Ready**: Deploy v1.0

---

**Test Status**: Ready for Testing
**Coverage**: Comprehensive
**Documentation**: Complete
