# Android Expenses & Receipts App

A comprehensive Android application for managing expenses and receipts with OCR capabilities, built using modern Android development practices.

## Features

### Core Functionality
- **Receipt Capture**: Capture receipts using device camera or upload from gallery (JPG, PNG, PDF support)
- **OCR Processing**: Extract key data from receipts using Google ML Kit:
  - Merchant name
  - Receipt date
  - Total amount
  - VAT amount  
  - Currency
  - Payment method (if applicable)
- **Data Management**: Review, edit, and categorize receipts with confidence scoring
- **Smart Categorization**: Predefined categories (Fuel, Lunch, Dinner, Hotel, Transport, etc.)
- **File Management**: Automatic file renaming in format `YYYY-MM-DD__MERCHANT__CATEGORY__TOTAL.ext`
- **Reports & Export**: Filter receipts by date range and export as CSV

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Database**: Room Database with coroutines/Flow
- **OCR**: Google ML Kit Text Recognition
- **Camera**: CameraX
- **Navigation**: Jetpack Navigation Compose
- **Permissions**: Accompanist Permissions
- **Export**: CSV generation, PDFBox (for future PDF support)

## Project Structure

```
app/src/main/java/com/expenses/app/
├── data/
│   ├── Receipt.kt              # Room entity for receipts
│   ├── ExportStatus.kt         # Enum for export states
│   ├── Converters.kt           # Type converters for Room
│   ├── ReceiptDao.kt           # Data Access Object
│   ├── ReceiptDatabase.kt      # Room database definition
│   └── ReceiptRepository.kt    # Repository pattern implementation
├── ui/
│   ├── MainActivity.kt         # Main activity entry point
│   ├── Navigation.kt           # Navigation routes
│   ├── ReceiptViewModel.kt     # ViewModel for receipt operations
│   ├── screens/
│   │   ├── HomeScreen.kt       # Home screen with receipt list
│   │   ├── AddReceiptScreen.kt # Camera/Gallery selection
│   │   ├── EditReceiptScreen.kt# Receipt edit form
│   │   └── ReportsScreen.kt    # Reports and CSV export
│   └── theme/
│       ├── Color.kt            # App colors
│       ├── Theme.kt            # Material3 theme
│       └── Type.kt             # Typography
└── util/
    ├── OcrProcessor.kt         # ML Kit OCR processing
    ├── OcrResult.kt            # OCR result data class
    ├── FileUtils.kt            # File naming and management
    └── CsvExporter.kt          # CSV export functionality
```

## Data Model

### Receipt Entity
```kotlin
@Entity
data class Receipt(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val receiptDate: Long,
    val merchant: String?,
    val totalAmount: Double,
    val vatAmount: Double?,
    val currency: String,
    val category: String,
    val notes: String?,
    val tags: List<String>,
    val ocrRawText: String?,
    val ocrConfidence: Float?,
    val originalUri: String?,
    val storedUri: String?,
    val renamedFileName: String?,
    val exportFolderUri: String?,
    val exportStatus: ExportStatus,
    val lastExportAttemptAt: Long?
)
```

### Export Status
- `NOT_EXPORTED`: Receipt needs review
- `EXPORTED`: Successfully exported
- `FAILED`: Export failed

## Features Implementation

### 1. Receipt Capture
- Camera permission handling with Accompanist
- CameraX integration for high-quality image capture
- File picker for gallery selection
- Support for JPG, PNG, and PDF files
- Temporary file management with FileProvider

### 2. OCR Processing
- ML Kit Text Recognition for Latin scripts
- Pattern-based extraction for:
  - Merchant: First line of text
  - Date: Multiple format support (YYYY-MM-DD, DD/MM/YYYY, etc.)
  - Total: Pattern matching for "Total", "Amount", "Sum"
  - VAT: Pattern matching for "VAT", "Tax", "MWST"
  - Currency: Detection of EUR, USD, GBP, CHF and symbols
- Confidence scoring for validation
- Low confidence warnings in UI

### 3. Receipt Management
- Real-time list of receipts with status indicators
- Editable forms with validation
- Category selection chips
- Optional notes and tags
- Date formatting and display

### 4. File Management
- Automatic filename generation: `YYYY-MM-DD__MERCHANT__CATEGORY__TOTAL.ext`
- Export folder selection
- File copying to export directory
- Export status tracking

### 5. Reports & Export
- Date range filtering
- Summary statistics:
  - Total amount
  - Receipt count
  - Category breakdown
- CSV export with columns:
  - Date, Merchant, Category, Total Amount, VAT Amount, Currency, Notes, Tags, Export Status

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with:
  - Platform: Android 14 (API 34)
  - Build Tools: 34.0.0
  - Min SDK: Android 8.0 (API 26)

### Setup
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on emulator or device

### Gradle Configuration
- Gradle: 8.2
- Android Gradle Plugin: 8.2.0
- Kotlin: 1.9.20
- Compose BOM: 2023.10.01

## Permissions Required

The app requests the following permissions:
- `CAMERA`: For capturing receipt photos
- `READ_MEDIA_IMAGES`: For selecting images from gallery (Android 13+)
- `READ_EXTERNAL_STORAGE`: For file access (Android 12 and below)

## Future Enhancements

1. **PDF Generation**: Use PDFBox for detailed PDF reports
2. **ZIP Exports**: Bundle receipts with their images
3. **Smart Tagging**: ML-based automatic tagging
4. **Cloud Sync**: Backup and sync across devices
5. **Multi-currency Support**: Exchange rate conversion
6. **Receipt Search**: Full-text search across OCR data
7. **Analytics**: Spending trends and insights
8. **Dark Mode**: Complete dark theme support

## Architecture Highlights

### MVVM Pattern
- **Model**: Room entities and data classes
- **View**: Jetpack Compose UI
- **ViewModel**: Business logic and state management

### Reactive Data Flow
- Kotlin Flow for reactive streams
- StateFlow for UI state
- Coroutines for async operations

### Single Source of Truth
- Room database as the primary data source
- Repository pattern for data abstraction
- ViewModels expose immutable state

## Testing Strategy

### Unit Tests
- ViewModel logic testing
- Repository testing with fake data sources
- Utility function testing (OCR, file naming, CSV generation)

### Integration Tests
- Room database operations
- OCR processing pipeline
- File management operations

### UI Tests (Compose)
- Screen navigation
- Form validation
- User interactions

## License

Copyright © 2024. All rights reserved.