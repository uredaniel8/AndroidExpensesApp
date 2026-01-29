# Image Saving Functionality Fix

## Problem Statement
The Android expenses app was failing to save images locally to selected custom folders. While the UI allowed users to select custom folders and the preferences were properly stored, the actual image saving code never utilized these custom folders, resulting in a silent failure where images were always saved to default app folders.

## Root Cause Analysis

### What Was Broken
1. **Disconnected Components**: The app had all infrastructure for custom folder selection (UI, preferences, permissions) but the actual file saving logic ignored it completely.

2. **Missing DocumentFile Integration**: The `FileUtils.saveReceiptImage()` method only saved to default app folders using traditional File API, with no support for Storage Access Framework (SAF).

3. **No URI Passing**: The `ReceiptViewModel` maintained custom folder URIs in StateFlow but never passed them to `FileUtils` when saving images.

### Code Flow Before Fix
```
User selects custom folder → URI saved to preferences → ReceiptViewModel loads URI
                                                                ↓
                                                          (URI never used)
                                                                ↓
Receipt saved → FileUtils.saveReceiptImage() → Always uses default folders
```

## Solution Implementation

### 1. FileUtils.kt Changes

#### New Method: `saveToCustomFolder()`
```kotlin
private fun saveToCustomFolder(
    context: Context,
    sourceUri: Uri,
    customFolderUri: Uri,
    fileName: String
): Pair<String?, String?>?
```

**Features:**
- Uses DocumentFile API to write to SAF-managed folders
- Handles file existence checking and collision avoidance
- Comprehensive error handling with null checks
- Cleans up created files if operations fail
- Logs detailed error information for debugging

#### Enhanced Method: `saveReceiptImage()`
```kotlin
fun saveReceiptImage(
    context: Context,
    sourceUri: Uri,
    receipt: Receipt,
    customFolderUri: Uri? = null  // NEW PARAMETER
): Pair<String?, String?>
```

**Logic Flow:**
1. Generate standardized filename from receipt metadata
2. If `customFolderUri` provided:
   - Try to save to custom folder using DocumentFile API
   - If successful, return content:// URI
   - If fails, log warning and fall through to default
3. Save to default app folder as fallback
4. Return file path or (null, null) on error

**Key Design Decisions:**
- **Optional parameter** maintains backward compatibility
- **Fallback behavior** ensures images are never lost
- **Different URI formats** (content:// vs file path) documented
- **Explicit null checks** prevent silent failures

### 2. ReceiptViewModel.kt Changes

#### New Helper Method
```kotlin
private fun getCustomFolderForCategory(category: String): Uri?
```

**Purpose:** Eliminates code duplication by centralizing category-to-folder logic:
- "Fuel" category → uses `_fuelFolderUri`
- All other categories → uses `_otherFolderUri`

#### Updated Methods
Both `updateReceipt()` and `uploadToProtonDrive()` now:
1. Call `getCustomFolderForCategory()` to get appropriate URI
2. Pass the URI to `FileUtils.saveReceiptImage()`
3. Image is saved to custom folder if available

### 3. Dependency Addition

Added to `build.gradle.kts`:
```kotlin
implementation("androidx.documentfile:documentfile:1.0.1")
```

**Security:** No known vulnerabilities (verified via GitHub Advisory Database)

## Technical Details

### Storage Access Framework (SAF) Integration

**Why DocumentFile?**
- Provides access to user-selected directories via `ACTION_OPEN_DOCUMENT_TREE`
- Works across different storage providers (internal, SD card, cloud)
- Respects Android's scoped storage requirements (Android 10+)
- Manages permissions automatically through persistent URI grants

**URI Permissions Flow:**
1. User selects folder via `OpenDocumentTree` activity
2. System grants URI with READ/WRITE permissions
3. ViewModel calls `takePersistableUriPermission()` to persist access
4. URI saved to SharedPreferences
5. On app restart, URI is reloaded and permissions remain valid
6. FileUtils uses ContentResolver with the URI to write files

### Error Handling Strategy

**Graceful Degradation:**
- Custom folder inaccessible? → Fall back to default folder
- Input stream null? → Return error immediately
- Output stream null? → Clean up and return error
- Zero bytes copied? → Log warning but continue

**Logging Levels:**
- `Log.e()`: Critical failures (folder inaccessible, stream null)
- `Log.w()`: Warnings (fallback triggered, zero bytes, delete failed)
- No user-visible errors for fallback (transparent UX)

### URI Format Handling

**Two Storage Modes:**

1. **Custom Folder (SAF)**:
   - Returns: `content://com.android.externalstorage.documents/tree/.../document/...`
   - Accessed via: ContentResolver with DocumentFile
   - Persists across app restarts via persistent URI permission

2. **Default Folder (File API)**:
   - Returns: `/storage/emulated/0/Android/data/com.expenses.app/files/Receipts/[Fuel|Other]/filename.jpg`
   - Accessed via: Traditional File I/O
   - Always accessible (app-specific external storage)

**Note:** The app correctly handles both URI formats when loading/displaying images.

## Testing Recommendations

### Manual Testing Scenarios

#### 1. Basic Custom Folder Selection
- [ ] Open Settings → Select custom folder for Fuel receipts
- [ ] Verify "Custom folder selected" status appears
- [ ] Add a Fuel receipt with image
- [ ] Enable local storage and save the receipt
- [ ] Use a file manager to verify image appears in selected folder
- [ ] Verify image filename format: `DD.MM.YYYY - Description - Amount.ext`

#### 2. Category-Based Routing
- [ ] Select different folders for Fuel and Other receipts
- [ ] Add a Fuel receipt → verify it goes to Fuel folder
- [ ] Add an "Other" category receipt → verify it goes to Other folder
- [ ] Change receipt category → save again → verify it moves to correct folder

#### 3. Persistence Across Restarts
- [ ] Select custom folders for both categories
- [ ] Force close the app (Settings → Apps → Force Stop)
- [ ] Reopen app → verify Settings still shows "Custom folder selected"
- [ ] Save a new receipt → verify it uses the custom folder

#### 4. Fallback Behavior
- [ ] Select a custom folder
- [ ] Use file manager to delete/rename the selected folder
- [ ] Try to save a receipt
- [ ] Verify image still saves successfully (to default folder)
- [ ] Check logcat for fallback warning

#### 5. Permission Handling
- [ ] Select a custom folder
- [ ] Revoke storage permissions in Android Settings
- [ ] Try to save a receipt
- [ ] Verify app handles permission denial gracefully

#### 6. Edge Cases
- [ ] Select folder on external SD card (if available)
- [ ] Select folder with special characters in name
- [ ] Select same folder for both Fuel and Other
- [ ] Try saving very large images (>10MB)
- [ ] Try saving different file formats (JPG, PNG, PDF)

### Automated Testing (If Implemented)

```kotlin
@Test
fun `saveReceiptImage with custom folder saves to SAF location`() {
    // Setup mock custom folder URI
    // Create test receipt
    // Call saveReceiptImage with customFolderUri
    // Verify DocumentFile.createFile was called
    // Verify content was written
}

@Test
fun `saveReceiptImage falls back to default when custom folder fails`() {
    // Setup invalid custom folder URI
    // Create test receipt
    // Call saveReceiptImage with customFolderUri
    // Verify default folder was used
    // Verify fallback warning was logged
}

@Test
fun `getCustomFolderForCategory returns correct URI for Fuel`() {
    // Set fuel folder URI
    // Call with category "Fuel"
    // Verify returns fuel URI
}
```

## Security Considerations

### Permission Management
✅ **Proper**: Uses SAF which respects Android's permission model
✅ **Persistent**: Calls `takePersistableUriPermission()` for long-term access
✅ **Release**: Calls `releasePersistableUriPermission()` when folder reset
✅ **Scope**: Only accesses user-selected folders, no broad storage access

### Data Privacy
✅ **User Choice**: Only writes to explicitly user-selected locations
✅ **Fallback Safe**: Falls back to app-specific storage (private by default)
✅ **No Leakage**: Error messages don't expose internal paths
✅ **Cleanup**: Removes failed file creations to avoid orphaned data

### Vulnerability Assessment
- ✅ No path traversal vulnerabilities (SAF handles path validation)
- ✅ No SQL injection (no database queries with user input)
- ✅ No insecure file permissions (uses Android's managed permissions)
- ✅ Dependency scan passed (androidx.documentfile:documentfile:1.0.1)

## Known Limitations

### 1. No Folder Validation on Selection
- User can select any folder, even if it's not writable
- Validation happens only when saving (may surprise user)
- **Mitigation**: Clear error messages when save fails

### 2. URI Format Inconsistency
- Custom folder: content:// URI
- Default folder: file path string
- Both formats stored in same `storedUri` field
- **Mitigation**: App correctly handles both formats when loading

### 3. No User Feedback on Fallback
- When custom folder fails, silently falls back to default
- User isn't notified their preferred folder wasn't used
- **Mitigation**: Logs warning for debugging; UX could be improved

### 4. Build Verification Limited
- Cannot perform full Android build in sandbox due to network restrictions
- Code verified through manual review and syntax checking
- **Mitigation**: CI/CD pipeline should run full build and tests

## Future Enhancements

### Short Term
1. Add toast notification when fallback occurs
2. Display selected folder path in Settings (not just "Custom folder selected")
3. Test folder writability when selected (before user tries to save)
4. Add "Open folder" button to view receipts in file manager

### Medium Term
5. Support per-category folders beyond just Fuel/Other
6. Automatic folder creation if selected folder deleted
7. Batch file migration when changing folder settings
8. Folder size/space monitoring with warnings

### Long Term
9. Cloud storage provider integration (Google Drive, Dropbox, etc.)
10. Automatic backup/restore of folder preferences
11. Export wizard for bulk organization
12. OCR improvement with folder-based training data

## Deployment Checklist

Before merging this PR:
- [x] Code review completed and feedback addressed
- [x] Security scan passed (CodeQL)
- [x] Dependency vulnerability check passed
- [ ] Manual testing on physical device (Google Pixel 9a as requested)
- [ ] Manual testing on Android 10, 11, 12, 13, 14 (various API levels)
- [ ] Verify Settings UI displays correctly
- [ ] Verify images save to correct folders
- [ ] Verify fallback behavior works
- [ ] Verify persistence across app restarts
- [ ] Logcat review for any unexpected warnings/errors
- [ ] Update user documentation/help text if needed

## Rollback Plan

If issues are discovered post-deployment:

1. **Immediate**: Revert to previous commit (preserves existing functionality)
2. **Data Safety**: User's stored URIs remain in SharedPreferences (no data loss)
3. **Backward Compat**: Optional parameter means old code paths still work
4. **Testing**: Run integration tests on reverted code to ensure stability

## Support Information

### Debugging Steps for Issues

**Problem: Images not saving to custom folder**
1. Check logcat for FileUtils warnings/errors
2. Verify Settings shows "Custom folder selected"
3. Check if folder still exists on device
4. Try re-selecting the folder
5. Test with default folder to isolate issue

**Problem: App crashes when saving**
1. Check logcat for stack trace
2. Verify permissions granted
3. Check available storage space
4. Try clearing app cache/data
5. Reinstall app if persistent

### Log Messages to Look For

- `FileUtils: Failed to save to custom folder, falling back to default` - Custom folder unavailable
- `FileUtils: Custom folder is not accessible` - Folder deleted or permissions lost
- `FileUtils: Failed to open input stream` - Source image unavailable
- `FileUtils: Failed to create file in custom folder` - Write permission issue
- `FileUtils: No bytes copied` - Empty file or read failure

## References

- [Android Storage Access Framework Guide](https://developer.android.com/guide/topics/providers/document-provider)
- [DocumentFile API Documentation](https://developer.android.com/reference/androidx/documentfile/provider/DocumentFile)
- [Scoped Storage Best Practices](https://developer.android.com/training/data-storage)
- [Content Resolver and URIs](https://developer.android.com/guide/topics/providers/content-provider-basics)

## Contributors

- Implementation: GitHub Copilot Agent
- Code Review: Automated review system
- Testing: Pending (requires physical device access)
