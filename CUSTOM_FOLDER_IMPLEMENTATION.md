# Custom Folder Selection Feature - Implementation Summary

## Overview
This implementation adds the ability for users to select custom folders for storing fuel receipts and other receipts, with preferences persisting across app restarts.

## Files Modified/Created

### New Files
1. **FolderPreferences.kt** - Manages folder preferences using SharedPreferences
   - Stores custom folder URIs for fuel and other receipts
   - Provides methods to get, set, and clear folder preferences
   - Handles URI permission cleanup

### Modified Files
1. **SettingsScreen.kt** - Added custom folder selection UI
   - Two folder picker buttons with visual status indicators
   - Reset buttons for each folder type
   - Information dialog explaining custom folders
   - Uses ActivityResultContracts.OpenDocumentTree() for folder selection

2. **ReceiptViewModel.kt** - Added folder management logic
   - StateFlow properties for tracking folder URIs
   - Methods to set/reset folders with proper permission handling
   - Loads saved preferences on initialization
   - Releases permissions when folders are reset

3. **ProtonDriveService.kt** - Enhanced to support custom folders
   - Checks for custom folders before using defaults
   - Implements saveToCustomFolder() using DocumentFile API
   - Thread-safe URI access
   - Comprehensive error handling

4. **MainActivity.kt** - Wired up the new functionality
   - Passes folder URIs to SettingsScreen
   - Connects callbacks for folder selection

5. **FileUtils.kt** - Minor cleanup
   - Removed unused custom folder variables

## Key Features

### 1. Folder Picker UI
- **Location**: Settings screen
- **Controls**: 
  - "Select Folder" button for fuel receipts
  - "Select Folder" button for other receipts
  - "Reset" buttons to clear custom selections
  - Status text showing "Custom folder selected" or "Using default folder"

### 2. Persistence
- Uses SharedPreferences to store folder URIs
- URIs persist across app restarts
- Persistent URI permissions requested via takePersistableUriPermission()
- Permissions released when folders are reset via releasePersistableUriPermission()

### 3. Storage Access Framework (SAF)
- Uses OpenDocumentTree() to let users pick folders
- Requests READ and WRITE permissions
- Works with any folder accessible via SAF
- Falls back to default folders if custom folders not set or inaccessible

### 4. Error Handling
- **Folder not accessible**: Shows error message, falls back to default
- **Permission denied**: Specific error message asking user to select different folder
- **Unsupported provider**: Error message suggesting device storage folders
- **Folder deleted**: Gracefully falls back to default folder
- **Output stream failure**: Returns error result instead of crashing

### 5. Thread Safety
- Makes local copies of URI references in uploadReceipt()
- Uses @Volatile for URI storage variables
- All persistence operations use SharedPreferences atomic operations

## User Flow

1. User opens Settings screen
2. User taps "Select Folder" for fuel or other receipts
3. System shows folder picker dialog (OpenDocumentTree)
4. User navigates and selects a folder
5. App requests persistent permissions for the selected folder
6. App saves the folder URI to SharedPreferences
7. App updates UI to show "Custom folder selected"
8. When saving receipts:
   - If custom folder set: Use DocumentFile API to save to custom folder
   - If custom folder not set or inaccessible: Use default app folder
9. User can tap "Reset" to clear custom folder and revert to default

## Testing Instructions

### Manual Testing Checklist

#### Basic Functionality
- [ ] Open Settings screen and verify custom folder selection UI is visible
- [ ] Tap "Select Folder for Fuel Receipts" and verify folder picker opens
- [ ] Select a folder and verify "Custom folder selected" status appears
- [ ] Tap "Select Folder for Other Receipts" and select a different folder
- [ ] Verify both folders show custom status

#### Receipt Saving
- [ ] Enable local storage in settings
- [ ] Add a fuel receipt and save it to local storage
- [ ] Verify receipt is saved to the custom fuel folder (if set)
- [ ] Add a non-fuel receipt and save it to local storage
- [ ] Verify receipt is saved to the custom other folder (if set)

#### Persistence
- [ ] Select custom folders for both fuel and other receipts
- [ ] Close the app completely
- [ ] Reopen the app and navigate to Settings
- [ ] Verify both custom folders still show "Custom folder selected" status
- [ ] Save a receipt and verify it goes to the custom folder

#### Reset Functionality
- [ ] Select a custom folder
- [ ] Tap the "Reset" button
- [ ] Verify status changes to "Using default folder"
- [ ] Save a receipt and verify it goes to default app folder
- [ ] Close and reopen app
- [ ] Verify reset persists (shows default folder)

#### Error Handling
- [ ] Select a custom folder
- [ ] Use a file manager to delete/rename the selected folder
- [ ] Try to save a receipt
- [ ] Verify error message appears and receipt saves to default folder instead
- [ ] Try selecting a folder from external SD card or cloud storage
- [ ] Verify appropriate error handling if access fails

#### Edge Cases
- [ ] Select the same folder for both fuel and other receipts (should work)
- [ ] Select a folder, then select a different folder (should release old permission)
- [ ] Try selecting a system folder (should fail gracefully with error message)
- [ ] Test with very long folder paths
- [ ] Test with special characters in folder names

## Known Limitations

1. **FileUtils.saveReceiptImage()** still uses default folders
   - This is intentional - it's for local app cache
   - ProtonDriveService handles custom folder exports

2. **Folder access validation**
   - No upfront validation that selected folder is writable
   - Validation happens when actually saving a receipt

3. **No folder browsing**
   - Once selected, users can't see the full path
   - Could add path display in future enhancement

4. **Network access required for build**
   - Cannot verify compilation without network access
   - Code has been syntax-checked and reviewed

## Security Considerations

1. **URI Permissions**: Properly manages persistent permissions
   - Takes permissions when folder selected
   - Releases permissions when folder reset
   - Handles permission errors gracefully

2. **Data Privacy**: Custom folders respect user choice
   - No hardcoded paths to sensitive locations
   - Uses SAF which respects Android security model
   - Falls back to app-specific storage if custom folder fails

3. **Error Messages**: No sensitive information leaked
   - Generic error messages for permission failures
   - Doesn't expose internal paths or URIs to user

## Future Enhancements

1. Display selected folder path in Settings
2. Add "Open folder" button to view receipts in file manager
3. Validate folder access before confirming selection
4. Add folder size/space monitoring
5. Support for per-category custom folders (not just Fuel/Other)
6. Automatic folder creation if selected folder deleted
7. Backup/restore folder preferences

## Implementation Notes

- Total lines of code added: ~450
- Total files modified: 5
- Total files created: 1
- No breaking changes to existing functionality
- Backward compatible with existing installations
- No new dependencies required
- Uses standard Android APIs (SAF, SharedPreferences)
