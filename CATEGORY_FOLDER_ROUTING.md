# Category-Based Folder Routing Implementation

## Overview
This document describes the implementation of category-based folder routing for receipt storage in the Android Expenses App. The system automatically routes receipts to different folders based on their category, with support for custom folder selections.

## Architecture

### Default Folder Structure
```
Receipts/
├── Fuel/          # For receipts with category "Fuel" (case-insensitive)
└── Other/         # For all other categories
```

### Custom Folder Support
Users can optionally select custom folders for each category type:
- Custom Fuel folder: User-selected location for Fuel receipts
- Custom Other folder: User-selected location for non-Fuel receipts

## Implementation Details

### 1. Category Detection (`FileUtils.getCategoryFolder()`)
**Location:** `app/src/main/java/com/expenses/app/util/FileUtils.kt` (lines 69-83)

```kotlin
fun getCategoryFolder(context: Context, category: String): File {
    val baseFolder = File(context.getExternalFilesDir(null), "Receipts")
    val categoryFolder = if (category.equals("Fuel", ignoreCase = true)) {
        Log.d("FileUtils", "Category '$category' matched as Fuel - using Documents/Fuel Receipts folder")
        File(baseFolder, "Fuel")
    } else {
        Log.d("FileUtils", "Category '$category' matched as Other - using Documents/Expenses Receipts folder")
        File(baseFolder, "Other")
    }
    if (!categoryFolder.exists()) {
        val created = categoryFolder.mkdirs()
        Log.d("FileUtils", "Created folder ${categoryFolder.absolutePath}: $created")
    }
    return categoryFolder
}
```

**Key Features:**
- Case-insensitive matching: "Fuel", "fuel", "FUEL", "FuEl" all match as Fuel
- All other categories (Food, Travel, Entertainment, Office Supplies, etc.) go to "Other"
- Automatically creates folders if they don't exist

### 2. Custom Folder Selection (`ReceiptViewModel.getCustomFolderForCategory()`)
**Location:** `app/src/main/java/com/expenses/app/ui/ReceiptViewModel.kt` (lines 208-230)

```kotlin
private fun getCustomFolderForCategory(category: String): Uri? {
    val folderUri = if (category.equals("Fuel", ignoreCase = true)) {
        Log.d("ReceiptViewModel", "Category '$category' matched as Fuel - checking for custom Fuel folder")
        _fuelFolderUri.value
    } else {
        Log.d("ReceiptViewModel", "Category '$category' is not Fuel - checking for custom Other folder")
        _otherFolderUri.value
    }
    
    if (folderUri != null) {
        Log.d("ReceiptViewModel", "Custom folder found for category '$category': $folderUri")
    } else {
        Log.d("ReceiptViewModel", "No custom folder set for category '$category', will use default")
    }
    
    return folderUri
}
```

**Key Features:**
- Checks for custom Fuel folder if category is "Fuel"
- Checks for custom Other folder for all other categories
- Returns null if no custom folder is set (fallback to default)

### 3. Receipt Saving (`FileUtils.saveReceiptImage()`)
**Location:** `app/src/main/java/com/expenses/app/util/FileUtils.kt` (lines 103-158)

**Save Process:**
1. **Try Custom Folder First:** If a custom folder URI is provided:
   - Attempts to save using Storage Access Framework (DocumentFile API)
   - Returns content:// URI on success
   - Falls through to default on failure

2. **Fallback to Default Folder:** If no custom folder or custom save fails:
   - Uses `getCategoryFolder()` to determine Fuel vs Other
   - Saves to app's external files directory
   - Returns file path string

3. **Error Handling:**
   - Comprehensive logging at each step
   - Graceful fallback from custom to default folders
   - Returns (null, null) only on complete failure

### 4. Integration Points

#### Receipt Update (`ReceiptViewModel.updateReceipt()`)
**Location:** `app/src/main/java/com/expenses/app/ui/ReceiptViewModel.kt` (lines 232-250)

```kotlin
fun updateReceipt(receipt: Receipt) {
    if (receipt.originalUri != null && receipt.storedUri == null) {
        val customFolderUri = getCustomFolderForCategory(receipt.category)
        val (storedPath, fileName) = FileUtils.saveReceiptImage(
            context = getApplication(),
            sourceUri = imageUri,
            receipt = receipt,
            customFolderUri = customFolderUri
        )
    }
}
```

#### Receipt Upload (`ReceiptViewModel.uploadToProtonDrive()`)
**Location:** `app/src/main/java/com/expenses/app/ui/ReceiptViewModel.kt` (lines 341-356)

Similar logic ensures receipts are saved with proper category routing when uploaded.

## Logging

The implementation includes comprehensive debug logging to track folder routing:

### FileUtils Logs
- `"Starting saveReceiptImage for category: {category}"`
- `"Category '{category}' matched as Fuel - using Documents/Fuel Receipts folder"`
- `"Category '{category}' matched as Other - using Documents/Expenses Receipts folder"`
- `"Using default category folder: {path}"`
- `"Successfully saved {bytes} bytes to: {path}"`

### ReceiptViewModel Logs
- `"Category '{category}' matched as Fuel - checking for custom Fuel folder"`
- `"Category '{category}' is not Fuel - checking for custom Other folder"`
- `"Custom folder found for category '{category}': {uri}"`
- `"No custom folder set for category '{category}', will use default"`

## Testing

Comprehensive unit tests are provided in `FileUtilsTest.kt` covering:

### Category Routing Tests
- ✅ Fuel category routes to Fuel folder
- ✅ Case-insensitive matching (fuel, FUEL, FuEl)
- ✅ Various categories (Food, Travel, Entertainment) route to Other
- ✅ Empty and Uncategorized route to Other

### File Operations Tests
- ✅ File name generation with category
- ✅ Null merchant/description handling
- ✅ Extension detection (jpg, png, pdf)
- ✅ Consistent path structure

## Usage Flow

### Typical Receipt Flow
1. **User adds receipt** → `processReceipt()` called with image URI
2. **User edits receipt** → Sets category (e.g., "Fuel", "Food")
3. **User saves receipt** → `updateReceipt()` called:
   - Checks if category is "Fuel" → Determines folder (custom or default)
   - Calls `saveReceiptImage()` with appropriate folder
   - Image saved to correct location based on category

### Custom Folder Flow
1. **User opens Settings** → Selects custom folder for Fuel or Other
2. **System requests permissions** → Takes persistable URI permissions
3. **Preference saved** → `FolderPreferences` stores URI
4. **Receipt saved** → Custom folder used for matching category

## Edge Cases Handled

1. **No Custom Folder Set:** Falls back to default app folders
2. **Custom Folder Unavailable:** Falls back to default app folders
3. **Permission Issues:** Catches SecurityException, falls back to default
4. **Invalid URI:** Attempts custom save, falls back on failure
5. **Folder Doesn't Exist:** Creates folder automatically
6. **Case Variations:** Case-insensitive category matching

## Benefits

1. **Organized Storage:** Receipts automatically organized by category
2. **User Choice:** Optional custom folder selection
3. **Robust Fallback:** Always saves receipts even if custom folder fails
4. **Clear Logging:** Easy to debug and track receipt saving
5. **Backward Compatible:** Existing receipts continue to work
6. **Extensible:** Easy to add more category types in the future

## Future Enhancements

Potential improvements:
1. Support for more than two category types
2. User-configurable category-to-folder mappings
3. Folder migration tool for existing receipts
4. Folder size and space management
5. Automatic backup of receipts to cloud storage
