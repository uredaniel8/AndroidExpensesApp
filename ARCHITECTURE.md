# Architecture Documentation

## Overview

The Android Expenses & Receipts application follows a **Clean Architecture** approach combined with **MVVM** (Model-View-ViewModel) pattern, leveraging modern Android development best practices.

## Architectural Layers

### 1. Presentation Layer (UI)
- **Technology**: Jetpack Compose
- **Components**: 
  - Composable functions for UI
  - ViewModels for state management
  - Navigation graph for screen routing
- **Responsibilities**:
  - Display data to users
  - Capture user interactions
  - Observe ViewModel state changes

### 2. Domain Layer (Business Logic)
- **Technology**: Pure Kotlin classes
- **Components**:
  - ViewModels
  - Use case classes (implicit in ViewModels for MVP)
- **Responsibilities**:
  - Process business rules
  - Coordinate data from repositories
  - Manage UI state

### 3. Data Layer
- **Technology**: Room, ML Kit, File System
- **Components**:
  - Repository pattern
  - DAOs (Data Access Objects)
  - Data sources (local database, OCR processor)
- **Responsibilities**:
  - Data persistence
  - Data transformation
  - Abstract data sources

## Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  HomeScreen  │  │ EditScreen   │  │ReportsScreen │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                  │                  │          │
│         └─────────┬────────┴────────┬─────────┘          │
│                   │                 │                    │
│            ┌──────▼─────────────────▼──────┐            │
│            │   ReceiptViewModel            │            │
│            └──────┬────────────────────────┘            │
└───────────────────┼─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                    Domain Layer                          │
│            ┌───────────────────────────┐                │
│            │  ReceiptRepository        │                │
│            └──────┬────────────────────┘                │
└───────────────────┼─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                    Data Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  ReceiptDao  │  │OcrProcessor  │  │  FileUtils   │  │
│  │   (Room)     │  │  (ML Kit)    │  │ (File I/O)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Data Flow

### Receipt Capture Flow
```
User Taps "Add Receipt" 
  → AddReceiptScreen (Compose)
  → Requests Camera Permission
  → CameraX captures image
  → Returns URI to ViewModel
  → ViewModel.processReceipt(uri)
  → OcrProcessor.processImage(uri)
  → ML Kit analyzes image
  → Returns OcrResult
  → Create Receipt entity
  → Repository.insertReceipt(receipt)
  → Room Database saves receipt
  → Flow emits updated list
  → UI recomposes with new data
```

### Receipt Edit Flow
```
User Taps Receipt Card
  → Navigate to EditReceiptScreen
  → Load Receipt by ID
  → Display in form fields
  → User edits fields
  → User taps Save
  → ViewModel.updateReceipt(receipt)
  → Repository.updateReceipt(receipt)
  → Room Database updates
  → Flow emits change
  → Navigate back
  → HomeScreen updates
```

### Export Flow
```
User Opens Reports
  → ReportsScreen loads receipts
  → User taps Export CSV
  → File picker opens
  → User selects location
  → CsvExporter.exportToCsv(receipts, file)
  → Write CSV data
  → Success/Error feedback
```

## State Management

### ViewModel State Pattern
```kotlin
class ReceiptViewModel : AndroidViewModel {
    // Immutable state exposed to UI
    val receipts: StateFlow<List<Receipt>>
    val isProcessing: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    // Mutable internal state
    private val _isProcessing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
}
```

### Reactive Updates with Flow
- **Database**: Room DAOs return `Flow<List<Receipt>>`
- **ViewModel**: Converts to `StateFlow` for UI consumption
- **UI**: Collects state with `collectAsState()`
- **Benefit**: Automatic UI updates when data changes

## Database Schema

### Receipt Table
```sql
CREATE TABLE receipts (
    id TEXT PRIMARY KEY NOT NULL,
    createdAt INTEGER NOT NULL,
    receiptDate INTEGER NOT NULL,
    merchant TEXT,
    totalAmount REAL NOT NULL,
    vatAmount REAL,
    currency TEXT NOT NULL,
    category TEXT NOT NULL,
    notes TEXT,
    tags TEXT NOT NULL,
    ocrRawText TEXT,
    ocrConfidence REAL,
    originalUri TEXT,
    storedUri TEXT,
    renamedFileName TEXT,
    exportFolderUri TEXT,
    exportStatus TEXT NOT NULL,
    lastExportAttemptAt INTEGER
);
```

### Indexes (Future Optimization)
```sql
CREATE INDEX idx_receiptDate ON receipts(receiptDate);
CREATE INDEX idx_category ON receipts(category);
CREATE INDEX idx_exportStatus ON receipts(exportStatus);
```

## Navigation Architecture

### Navigation Graph
```
Home Screen (Start Destination)
  ├─> Add Receipt Screen
  │     └─> (Returns to Home with URI)
  ├─> Edit Receipt Screen/{receiptId}
  │     └─> (Returns to Home)
  └─> Reports Screen
        └─> (Returns to Home)
```

### Route Definitions
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddReceipt : Screen("add_receipt")
    object EditReceipt : Screen("edit_receipt/{receiptId}") {
        fun createRoute(receiptId: String) = "edit_receipt/$receiptId"
    }
    object Reports : Screen("reports")
}
```

## Dependency Injection (Future)

### Recommended: Hilt
Currently using manual DI. Future implementation with Hilt:

```kotlin
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val ocrProcessor: OcrProcessor
) : ViewModel()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReceiptDatabase {
        return ReceiptDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideReceiptDao(database: ReceiptDatabase): ReceiptDao {
        return database.receiptDao()
    }
    
    @Provides
    @Singleton
    fun provideRepository(dao: ReceiptDao): ReceiptRepository {
        return ReceiptRepository(dao)
    }
}
```

## Error Handling

### Layered Error Handling
1. **Data Layer**: Catch exceptions, return Result/Resource
2. **Repository**: Transform data layer errors
3. **ViewModel**: Expose error state to UI
4. **UI**: Display user-friendly messages

### Example
```kotlin
// ViewModel
private val _error = MutableStateFlow<String?>(null)
val error: StateFlow<String?> = _error.asStateFlow()

fun processReceipt(uri: Uri) {
    viewModelScope.launch {
        try {
            _isProcessing.value = true
            val result = ocrProcessor.processImage(uri)
            repository.insertReceipt(createReceipt(result))
        } catch (e: Exception) {
            _error.value = "Failed to process receipt: ${e.message}"
        } finally {
            _isProcessing.value = false
        }
    }
}

// UI
val error by viewModel.error.collectAsState()
error?.let {
    Snackbar(message = it, onDismiss = { viewModel.clearError() })
}
```

## Threading Model

### Coroutines and Dispatchers
- **Main**: UI updates (Compose recomposition)
- **IO**: Database operations, file I/O
- **Default**: CPU-intensive work (OCR processing)

### Example Usage
```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val ocrResult = ocrProcessor.processImage(uri) // Heavy computation
    withContext(Dispatchers.Main) {
        _receipts.value = repository.getAllReceipts() // Update UI state
    }
}
```

### Room Configuration
Room handles threading automatically:
- Suspend functions run on background thread
- Flow emissions on background thread
- Collection on main thread via StateFlow

## Security Architecture

### Data Protection
- **Local Database**: SQLite encrypted at rest (Android 10+)
- **File Storage**: Scoped storage for user privacy
- **Permissions**: Runtime permission requests

### Best Practices Implemented
1. **Scoped Storage**: Use MediaStore API for images
2. **FileProvider**: Secure file sharing
3. **Permission Checks**: Before accessing sensitive resources
4. **Input Validation**: Sanitize user inputs

## Performance Optimization

### Database Performance
- **Indexing**: On frequently queried columns
- **Pagination**: For large datasets (future)
- **Transactions**: Batch operations

### UI Performance
- **Lazy Loading**: LazyColumn for lists
- **Remember**: Cache expensive calculations
- **Key**: Stable keys for list items
- **Derivation**: Only recompose affected parts

### Image Optimization
- **Compression**: Before saving
- **Thumbnail**: Generate for list view (future)
- **Caching**: ML Kit image processing

## Testing Strategy

### Unit Tests
- **ViewModels**: Business logic, state management
- **Repository**: Data operations
- **Utilities**: OCR extraction, file naming, CSV generation

### Integration Tests
- **Room**: Database operations end-to-end
- **Navigation**: Screen transitions
- **OCR Pipeline**: Image to receipt conversion

### UI Tests
- **Compose Tests**: UI component behavior
- **Screenshot Tests**: Visual regression (future)
- **Accessibility Tests**: TalkBack, font scaling

## Scalability Considerations

### Current Limitations
- No pagination (works for <1000 receipts)
- Synchronous file operations
- No background sync

### Future Scalability
1. **Pagination**: Implement paging library
2. **WorkManager**: Background upload/sync
3. **Cloud Backup**: Firebase or custom backend
4. **Multi-module**: Separate features into modules

## Monitoring and Analytics (Future)

### Recommended Tools
- **Firebase Crashlytics**: Crash reporting
- **Firebase Analytics**: User behavior
- **Firebase Performance**: App performance metrics

### Key Metrics to Track
- OCR accuracy rate
- Average processing time
- User retention
- Feature usage statistics

## Accessibility

### Current Implementation
- **Material3**: Built-in accessibility support
- **Content Descriptions**: For all interactive elements
- **Focus Order**: Logical navigation flow

### Future Enhancements
- **Voice Commands**: "Add receipt", "Export CSV"
- **Screen Reader**: Optimized TalkBack support
- **High Contrast**: Theme variants

## Internationalization (i18n)

### Current Implementation
- String resources in `strings.xml`
- Date formatting with Locale
- Number formatting with Locale

### Future Enhancements
- Multi-language support (Spanish, French, German)
- RTL layout support (Arabic, Hebrew)
- Region-specific date/currency formats

## Offline-First Architecture

### Current Implementation
- **Room Database**: All data stored locally
- **No Network Required**: App fully functional offline

### Future Cloud Integration
```
┌──────────────┐
│   Local DB   │ ←─┐
└──────────────┘   │
                   │ Sync
┌──────────────┐   │
│   Cloud DB   │ ←─┘
└──────────────┘
```

## Version Control Strategy

### Branch Strategy
- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: Feature development
- `bugfix/*`: Bug fixes
- `release/*`: Release preparation

### Release Process
1. Create release branch from develop
2. Version bump and changelog
3. Testing and bug fixes
4. Merge to main
5. Tag release
6. Deploy to Play Store

## Continuous Integration

### Recommended CI Pipeline
1. **Build**: Compile and package APK
2. **Lint**: Code style checks
3. **Test**: Unit and integration tests
4. **Code Coverage**: Generate reports
5. **Static Analysis**: Security vulnerabilities
6. **Deploy**: To Firebase App Distribution

## Conclusion

This architecture provides:
- **Separation of Concerns**: Clear layer boundaries
- **Testability**: Easy to mock and test
- **Maintainability**: Well-organized code structure
- **Scalability**: Ready for future enhancements
- **Performance**: Optimized for Android platform

The architecture follows Android best practices and is designed to be maintainable and extensible as the application grows.
