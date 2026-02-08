# Architecture Documentation

## Overview

The Pokemon Guess Who app uses **MVVM (Model-View-ViewModel)** architecture pattern with **Jetpack Compose** for reactive UI updates.

```
┌─────────────────────────────────────────────┐
│         UI Layer (Jetpack Compose)          │
│  - GameScreenUpdated                        │
│  - MainMenuScreen                           │
│  - PokemonCardComponent                     │
└──────────────────┬──────────────────────────┘
                   │ collectAsState()
┌──────────────────▼──────────────────────────┐
│      ViewModel Layer (PokemonViewModel)     │
│  - StateFlow<GameState>                     │
│  - StateFlow<List<GamePokemon>>             │
│  - Coroutine scopes                         │
└──────────────────┬──────────────────────────┘
                   │
     ┌─────────────┼─────────────┐
     │             │             │
┌────▼────┐  ┌────▼─────┐  ┌───▼──────┐
│ Network │  │   Game   │  │   Data   │
│ Layer   │  │  Manager │  │  Models  │
└────┬────┘  └────┬─────┘  └───┬──────┘
     │            │            │
┌────▼────┐  ┌────▼─────┐  ┌───▼──────┐
│ Retrofit│  │Game Logic│  │ Coroutines
│ +Gson   │  │ +State   │  │ + Flow
└────────┘  └──────────┘  └──────────┘
```

## Layer Details

### 1. UI Layer (View)

**Components:**
- `MainMenuScreen.kt` - Game menu
- `GameScreenUpdated.kt` - Main gameplay screen
- `PokemonCardComponent.kt` - Reusable card widget
- Navigation composables

**Responsibilities:**
- Display Pokemon cards
- Handle user input (taps, button clicks)
- Show game state (selected Pokemon, eliminated cards)
- Respond to ViewModel state changes

**Key Methods:**
```kotlin
@Composable
fun GameScreenUpdated(viewModel: PokemonViewModel)
```

### 2. ViewModel Layer

**Class:** `PokemonViewModel : ViewModel()`

**Exposed State:**
```kotlin
val gameState: StateFlow<GameState>        // Current game state
val pokemonList: StateFlow<List<GamePokemon>>  // All Pokemon
val isLoading: StateFlow<Boolean>          // Loading indicator
val errorMessage: StateFlow<String?>       // Error messages
```

**Public Functions:**
```kotlin
fun startNewGame()                                      // Start new game
fun selectPokemon(pokemon: GamePokemon)                 // Select Pokemon
fun togglePokemonElimination(pokemon: GamePokemon)      // Mark as eliminated
fun setGridColumns(columns: Int)                        // Change layout
fun toggleShowEliminated()                              // Toggle visibility
```

**Responsibilities:**
- Fetch Pokemon from API
- Manage game state
- Handle business logic
- Provide reactive data streams

### 3. Game Logic Layer

**Class:** `GameManager`

**Methods:**
```kotlin
fun generateGameBoard(pokemonList, boardSize): List<GamePokemon>
fun togglePokemonElimination(pokemon): GamePokemon
fun getVisiblePokemon(board, showEliminated): List<GamePokemon>
fun selectPokemon(pokemon): GamePokemon
fun getGridColumns(totalCards): List<Int>
```

**Responsibilities:**
- Board generation
- Card elimination logic
- Visibility filtering
- Layout calculations

### 4. Network Layer

**Components:**

**PokemonApiService (Interface)**
```kotlin
interface PokemonApiService {
    @GET("pokemon/{id}")
    suspend fun getPokemonDetails(@Path("id") id: Int): Pokemon
}
```

**PokemonApiClient (Singleton)**
```kotlin
object PokemonApiClient {
    val pokemonService: PokemonApiService
}
```

**Key Features:**
- Retrofit with OkHttp
- 15-second timeout
- Logging interceptor
- GSON JSON parsing

### 5. Data Layer

**Models:**

**Pokemon (API Response)**
```kotlin
data class Pokemon(
    val id: Int,
    val name: String,
    val sprites: Sprites,
    val types: List<TypeSlot>,
    val stats: List<Stat>
)
```

**GamePokemon (Game Model)**
```kotlin
data class GamePokemon(
    val pokemonId: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
    val hp: Int,
    val attack: Int,
    // ... other stats
    var isEliminated: Boolean
)
```

**GameState (UI State)**
```kotlin
data class GameState(
    val board: List<GamePokemon>,
    val selectedPokemon: GamePokemon?,
    val showEliminated: Boolean,
    val gridColumns: Int
)
```

### 6. Storage Layer

**PreferencesManager**
```kotlin
class PreferencesManager(context: Context) {
    fun saveGameState(state: String)
    fun getGameState(): Flow<String>
    fun savePlayerId(playerId: String)
    fun getPlayerId(): Flow<String>
}
```

Uses DataStore for secure preferences.

## Data Flow

### Game Startup

1. User launches app → MainActivity created
2. MainActivity creates PokemonViewModel
3. PokemonViewModel.init() calls loadPokemon()
4. Retrofit fetches Pokemon list from PokeAPI
5. PokemonViewModel.pokemonList updated
6. UI recomposes with new data

### Starting a Game

1. User taps "Start New Game"
2. ViewModel.startNewGame() called
3. GameManager.generateGameBoard() creates board
4. GameState.board updated
5. UI recomposes with new board

### Card Elimination

1. User taps Pokemon card
2. ViewModel.togglePokemonElimination(pokemon) called
3. GameManager.togglePokemonElimination() marks card
4. GameState.board updated with new eliminated state
5. UI recomposes, card fades and moves to bottom

### Grid Layout Change

1. User taps grid button (e.g., "3")
2. ViewModel.setGridColumns(3) called
3. GameState.gridColumns updated
4. LazyVerticalGrid recomposes with GridCells.Fixed(3)
5. Animated transition to new layout

## State Management

### Using StateFlow

All state is exposed as `StateFlow<T>`:

```kotlin
@Composable
fun GameScreen(viewModel: PokemonViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    // gameState updates whenever StateFlow emits new value
}
```

**Advantages:**
- Thread-safe
- Hot flow (always emits last value)
- Compose-integrated
- Testable

### Updating State

```kotlin
_gameState.value = _gameState.value.copy(
    board = newBoard,
    selectedPokemon = pokemon
)
```

## Coroutine Usage

### Scope Management

ViewModelScope automatically:
- Launches coroutines tied to ViewModel lifecycle
- Cancels coroutines when ViewModel is destroyed
- Prevents memory leaks

```kotlin
viewModelScope.launch {
    val data = PokemonApiClient.pokemonService.getPokemonDetails(id)
    _pokemonList.value = data
}
```

## Dependency Injection (Manual)

Currently using manual DI:

```kotlin
// In MainActivity
viewModel = ViewModelProvider(this).get(PokemonViewModel::class.java)

// In ViewModel
private val gameManager = GameManager()
```

Future: Consider Hilt for automatic DI.

## Error Handling

### API Errors

```kotlin
try {
    val response = api.getPokemonList()
    _pokemonList.value = response
} catch (e: Exception) {
    _errorMessage.value = "Failed: ${e.message}"
}
```

### UI Error Display

```kotlin
val errorMessage by viewModel.errorMessage.collectAsState()
if (errorMessage != null) {
    Text("Error: $errorMessage")
}
```

## Performance Considerations

### Compose Optimization

- Keys in LazyVerticalGrid for efficient recomposition
- AnimatedContent with proper label for transitions
- Image loading with Coil (automatic caching)

### Memory Management

- ViewModel survives configuration changes
- Coroutines scoped to ViewModel lifecycle
- MediaPlayer properly released in SoundManager

### Network Efficiency

- OkHttp connection pooling
- Retrofit retry logic
- No redundant API calls (via ViewModel caching)

## Testing Strategy

### Unit Tests

Test GameManager logic:
```kotlin
@Test
fun testPokemonElimination() {
    val pokemon = GamePokemon(...)
    val result = gameManager.togglePokemonElimination(pokemon)
    assertTrue(result.isEliminated)
}
```

### UI Tests

Test Compose components with compose testing library.

### API Tests

Mock Retrofit for testing without network.

## Future Improvements

1. **Hilt Dependency Injection**: Cleaner DI
2. **Repository Pattern**: Abstract data sources
3. **Remote Mediator**: Pagination for large Pokemon lists
4. **Room Database**: Local caching
5. **Bluetooth Module**: Multiplayer synchronization
6. **Animation Framework**: Unified animation system

## Debugging Tips

### Check State

```kotlin
// In composable
LaunchedEffect(gameState) {
    Log.d("GameState", "Board: ${gameState.board.size}")
}
```

### Network Debugging

Enable HTTP logging:
```kotlin
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
```

### Recomposition Tracing

Enable Compose Layout Inspector in Android Studio.

## References

- [Android Architecture Guide](https://developer.android.com/architecture)
- [Jetpack Compose State](https://developer.android.com/jetpack/compose/state)
- [ViewModel Documentation](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
