# Developer Documentation

## Getting Started

This document provides detailed information for developers working on the Android Expenses & Receipts application.

## Development Environment Setup

### Required Tools
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: Version 17 (bundled with Android Studio)
- **Android SDK**: API level 26-34
- **Git**: For version control

### Initial Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/uredaniel8/AndroidExpensesApp.git
   cd AndroidExpensesApp
   ```

2. Open the project in Android Studio

3. Wait for Gradle sync to complete

4. Run the app on an emulator or physical device

## Project Configuration

### Gradle Setup
The project uses Gradle with Kotlin DSL for build configuration.

#### Root `build.gradle.kts`
- Android Gradle Plugin: 8.2.0
- Kotlin: 1.9.20
- KSP (Kotlin Symbol Processing): 1.9.20-1.0.14

#### App `build.gradle.kts`
Key dependencies:
- Jetpack Compose BOM: 2023.10.01
- Room: 2.6.1
- CameraX: 1.3.1
- ML Kit Text Recognition: 16.0.0
- Navigation Compose: 2.7.6
- Accompanist Permissions: 0.32.0
- PDFBox Android: 2.0.27.0
- Gson: 2.10.1

### Android Configuration
- **Namespace**: `com.expenses.app`
- **Compile SDK**: 34
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **JVM Target**: 17

## Architecture Overview

### MVVM Architecture
The app follows the Model-View-ViewModel (MVVM) architectural pattern with the Repository pattern for data management.

```
UI Layer (Compose) → ViewModel → Repository → Data Source (Room/OCR)
```

### Data Flow
1. **User Interaction**: User interacts with Compose UI
2. **ViewModel**: Processes user actions and updates state
3. **Repository**: Abstracts data operations
4. **Data Sources**: Room database for persistence, ML Kit for OCR
5. **State Update**: Flow/StateFlow propagates changes back to UI

## Code Organization

### Package Structure

#### `data/`
Contains all data-related classes:
- **Entities**: Room database entities
- **DAOs**: Data Access Objects for database operations
- **Repository**: Implements repository pattern
- **Converters**: Type converters for Room

#### `ui/`
Contains UI-related classes:
- **Screens**: Composable functions for each screen
- **ViewModels**: Business logic and state management
- **Theme**: App theming (colors, typography, shapes)
- **Navigation**: Navigation graph and routes

#### `util/`
Contains utility classes:
- **OCR**: ML Kit text recognition processing
- **File Management**: File operations and naming
- **Export**: CSV generation

## Key Components

### 1. Database Layer (Room)

#### Receipt Entity
Represents a single receipt with all its metadata:
```kotlin
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey val id: String,
    val receiptDate: Long,
    val merchant: String?,
    val totalAmount: Double,
    // ... other fields
)
```

#### ReceiptDao
Provides CRUD operations with Flow for reactive updates:
```kotlin
@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY receiptDate DESC")
    fun getAllReceipts(): Flow<List<Receipt>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: Receipt)
    
    // ... other operations
}
```

#### Type Converters
Handles complex types:
- `List<String>` → JSON string (for tags)
- `ExportStatus` → String enum value

### 2. OCR Processing

#### OcrProcessor
Handles ML Kit text recognition:
```kotlin
class OcrProcessor {
    suspend fun processImage(imageUri: Uri): OcrResult
    
    private fun extractMerchant(text: String): String?
    private fun extractDate(text: String): Long?
    private fun extractTotal(text: String): Double?
    private fun extractVat(text: String): Double?
    private fun extractCurrency(text: String): String?
}
```

#### Pattern Recognition
- **Dates**: Multiple formats (YYYY-MM-DD, DD/MM/YYYY, DD.MM.YYYY)
- **Amounts**: Regex patterns for currency values
- **Merchants**: First line heuristic
- **Confidence**: Average confidence from ML Kit elements

### 3. ViewModel Layer

#### ReceiptViewModel
Manages UI state and business logic:
```kotlin
class ReceiptViewModel : AndroidViewModel {
    val receipts: StateFlow<List<Receipt>>
    val isProcessing: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    fun processReceipt(imageUri: Uri)
    fun updateReceipt(receipt: Receipt)
    fun deleteReceipt(receipt: Receipt)
}
```

### 4. UI Layer (Jetpack Compose)

#### Screen Components
- **HomeScreen**: Lists receipts with status indicators
- **AddReceiptScreen**: Camera/Gallery selection
- **EditReceiptScreen**: Form for editing receipt data
- **ReportsScreen**: Summary statistics and CSV export

#### Navigation
Uses Jetpack Navigation Compose:
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddReceipt : Screen("add_receipt")
    object EditReceipt : Screen("edit_receipt/{receiptId}")
    object Reports : Screen("reports")
}
```

## Common Development Tasks

### Adding a New Field to Receipt

1. Update the `Receipt` entity:
```kotlin
@Entity
data class Receipt(
    // ... existing fields
    val newField: String?
)
```

2. Create a database migration:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE receipts ADD COLUMN newField TEXT")
    }
}
```

3. Update the database version:
```kotlin
@Database(entities = [Receipt::class], version = 2)
```

4. Update UI screens to display/edit the new field

### Adding a New Category

Update the `categories` list in `EditReceiptScreen.kt`:
```kotlin
val categories = listOf(
    "Uncategorized", "Fuel", "Lunch", "Dinner", 
    "Hotel", "Transport", "Office Supplies", "Entertainment",
    "NewCategory" // Add here
)
```

### Implementing a New Export Format

1. Create a new exporter utility:
```kotlin
object JsonExporter {
    fun exportToJson(receipts: List<Receipt>, outputFile: File): Boolean
}
```

2. Add UI button in `ReportsScreen`

3. Implement file picker for the new format

## Testing Guidelines

### Unit Testing

#### ViewModel Tests
Test business logic without Android dependencies:
```kotlin
@Test
fun `processReceipt updates receipts list`() = runTest {
    // Given
    val viewModel = ReceiptViewModel(application)
    
    // When
    viewModel.processReceipt(mockUri)
    
    // Then
    assertTrue(viewModel.receipts.value.isNotEmpty())
}
```

#### Repository Tests
Use in-memory database:
```kotlin
@Test
fun `insertReceipt adds receipt to database`() = runTest {
    // Given
    val database = Room.inMemoryDatabaseBuilder(context, ReceiptDatabase::class.java).build()
    val dao = database.receiptDao()
    val repository = ReceiptRepository(dao)
    
    // When
    repository.insertReceipt(testReceipt)
    
    // Then
    val receipts = dao.getAllReceipts().first()
    assertEquals(1, receipts.size)
}
```

### UI Testing

#### Compose Tests
Test UI components:
```kotlin
@Test
fun homeScreen_displaysReceipts() {
    composeTestRule.setContent {
        HomeScreen(
            receipts = testReceipts,
            onAddReceipt = {},
            onUploadReceipt = {},
            onReceiptClick = {},
            onViewReports = {}
        )
    }
    
    composeTestRule.onNodeWithText("Test Merchant").assertIsDisplayed()
}
```

## Debugging Tips

### Database Inspection
Use Android Studio's Database Inspector:
1. Run the app
2. View → Tool Windows → App Inspection
3. Select Database Inspector tab

### OCR Debugging
Enable detailed logging:
```kotlin
val ocrResult = ocrProcessor.processImage(uri)
Log.d("OCR", "Raw text: ${ocrResult.rawText}")
Log.d("OCR", "Confidence: ${ocrResult.confidence}")
Log.d("OCR", "Merchant: ${ocrResult.merchant}")
```

### Compose Layout Inspector
Debug Compose layouts:
1. Run the app
2. Tools → Layout Inspector
3. Select your app process

## Performance Considerations

### Database Queries
- Use `Flow` for reactive updates
- Implement pagination for large datasets
- Add indexes for frequently queried columns

### Image Processing
- Scale images before OCR processing
- Process OCR on background thread (coroutines)
- Cache OCR results in database

### Compose Performance
- Use `remember` for expensive calculations
- Implement `key` parameter in lazy lists
- Avoid unnecessary recomposition

## Security Best Practices

### Data Storage
- Room database is encrypted by default on modern Android
- Use Android Keystore for sensitive data
- Clear temporary files after processing

### Permissions
- Request permissions at runtime
- Explain permission rationale to users
- Handle permission denial gracefully

### File Access
- Use scoped storage (Android 10+)
- Validate file types and sizes
- Use FileProvider for file sharing

## CI/CD (Future)

### Recommended Setup
1. **GitHub Actions** for CI
2. **Automated tests** on every PR
3. **Code coverage** reporting
4. **APK signing** for releases
5. **Firebase App Distribution** for beta testing

### Sample GitHub Actions Workflow
```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run tests
        run: ./gradlew test
```

## Troubleshooting

### Common Issues

#### Gradle Sync Fails
- Check internet connection
- Invalidate caches: File → Invalidate Caches
- Update Gradle wrapper: `./gradlew wrapper --gradle-version 8.2`

#### Camera Not Working
- Check camera permission in manifest
- Verify permission request in AddReceiptScreen
- Test on physical device (emulator camera can be flaky)

#### Room Database Migration Issues
- Fallback strategy: `.fallbackToDestructiveMigration()`
- Export schema: `exportSchema = true` in @Database
- Uninstall app to clear database

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable/function names
- Add KDoc comments for public APIs
- Format code: Cmd/Ctrl + Alt + L

### Pull Request Process
1. Create a feature branch
2. Make changes with clear commits
3. Run tests locally
4. Submit PR with description
5. Address review comments

### Commit Messages
Format: `[Type] Brief description`
- `[Feature]` New functionality
- `[Fix]` Bug fixes
- `[Refactor]` Code restructuring
- `[Docs]` Documentation changes
- `[Test]` Test additions/changes

## Resources

### Documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Persistence](https://developer.android.com/training/data-storage/room)
- [ML Kit](https://developers.google.com/ml-kit)
- [CameraX](https://developer.android.com/training/camerax)

### Learning Resources
- [Android Developers](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Compose Samples](https://github.com/android/compose-samples)

## Support

For issues or questions:
1. Check this documentation
2. Search existing GitHub issues
3. Create a new issue with detailed description
4. Include logs and screenshots when applicable
