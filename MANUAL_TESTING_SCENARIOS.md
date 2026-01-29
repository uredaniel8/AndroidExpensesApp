# Manual Testing Scenarios for Category-Based Folder Routing

## Test Environment Setup
1. Install the app on a device or emulator
2. Grant necessary permissions (camera, storage)
3. Enable debug logging to view folder routing logs

## Test Scenario 1: Default Folder Routing (No Custom Folders)

### Test 1.1: Fuel Receipt to Default Fuel Folder
**Steps:**
1. Open the app
2. Add a new receipt (take photo or select from gallery)
3. Edit the receipt and set category to "Fuel"
4. Enter other required fields (merchant, amount)
5. Save the receipt

**Expected Results:**
- Log shows: `"Category 'Fuel' matched as Fuel - using Documents/Fuel Receipts folder"`
- Receipt saved to: `/Android/data/com.expenses.app/files/Documents/Fuel Receipts/`
- File name format: `DD.MM.YYYY - Description - Amount.jpg`

**Verification:**
- Check device file system at the path above
- File should exist in the Fuel folder

### Test 1.2: Non-Fuel Receipt to Default Other Folder
**Steps:**
1. Open the app
2. Add a new receipt
3. Edit the receipt and set category to "Food"
4. Enter other required fields
5. Save the receipt

**Expected Results:**
- Log shows: `"Category 'Food' matched as Other - using Documents/Expenses Receipts folder"`
- Receipt saved to: `/Android/data/com.expenses.app/files/Documents/Expenses Receipts/`

**Verification:**
- Check device file system
- File should exist in the Other folder

### Test 1.3: Case-Insensitive Fuel Matching
**Steps:**
1. Create receipts with categories: "fuel", "FUEL", "FuEl"
2. Save each receipt

**Expected Results:**
- All three receipts saved to `Documents/Fuel Receipts/` folder
- Logs confirm Fuel category matching for all variations

### Test 1.4: Various Other Categories
**Steps:**
1. Create receipts with categories: "Travel", "Entertainment", "Office Supplies"
2. Save each receipt

**Expected Results:**
- All receipts saved to `Documents/Expenses Receipts/` folder
- Each receipt in the same Other folder regardless of specific category name

## Test Scenario 2: Custom Folder Routing

### Test 2.1: Set Custom Fuel Folder
**Steps:**
1. Open Settings
2. Tap "Select Fuel Folder"
3. Choose a custom folder from device storage
4. Grant persistent permissions

**Expected Results:**
- Success message shown
- Folder URI saved in preferences
- Log shows: `"Custom folder found for category 'Fuel': {uri}"`

### Test 2.2: Save Fuel Receipt to Custom Folder
**Steps:**
1. After setting custom Fuel folder (Test 2.1)
2. Add a new receipt
3. Set category to "Fuel"
4. Save the receipt

**Expected Results:**
- Log shows: `"Attempting to save to custom folder: {uri}"`
- Log shows: `"Successfully saved to custom folder"`
- Receipt saved in the custom folder selected in Test 2.1
- File name matches expected format

**Verification:**
- Navigate to custom folder on device
- Verify receipt file exists there
- Verify NOT in default `/Documents/Fuel Receipts/` folder

### Test 2.3: Set Custom Other Folder
**Steps:**
1. Open Settings
2. Tap "Select Other Folder"
3. Choose a different custom folder
4. Grant permissions

**Expected Results:**
- Success message shown
- Separate folder URI saved for Other category

### Test 2.4: Save Other Receipt to Custom Folder
**Steps:**
1. Add new receipt
2. Set category to "Food" (or any non-Fuel)
3. Save the receipt

**Expected Results:**
- Receipt saved to custom Other folder
- NOT in default `/Documents/Expenses Receipts/` folder

### Test 2.5: Different Categories Use Correct Custom Folders
**Steps:**
1. Ensure both custom folders are set
2. Create receipt with category "Fuel" → Save
3. Create receipt with category "Travel" → Save

**Expected Results:**
- Fuel receipt in custom Fuel folder
- Travel receipt in custom Other folder
- Each in their respective custom locations

## Test Scenario 3: Fallback Behavior

### Test 3.1: Custom Folder Deleted
**Steps:**
1. Set custom Fuel folder
2. Save a Fuel receipt (should succeed)
3. Using file manager, delete the custom folder
4. Save another Fuel receipt

**Expected Results:**
- First receipt saves to custom folder
- Second receipt:
  - Log shows: `"Failed to save to custom folder, falling back to default"`
  - Saves to default `/Documents/Fuel Receipts/` folder
  - No app crash

### Test 3.2: Custom Folder Permission Revoked
**Steps:**
1. Set custom folder
2. Go to Android Settings → Apps → Expenses App → Permissions
3. Revoke storage permissions
4. Return to app and save receipt

**Expected Results:**
- Falls back to default folder
- Log shows security exception
- Receipt still saved successfully

### Test 3.3: Reset Custom Folder
**Steps:**
1. Set custom Fuel folder
2. Save a receipt (should go to custom folder)
3. In Settings, tap "Reset Fuel Folder"
4. Save another receipt

**Expected Results:**
- First receipt in custom folder
- After reset, second receipt in default `/Documents/Fuel Receipts/` folder
- Permissions properly released

## Test Scenario 4: Edge Cases

### Test 4.1: Empty Category Name
**Steps:**
1. Create receipt
2. Set category to empty string ""
3. Save receipt

**Expected Results:**
- Receipt saved to `Documents/Expenses Receipts/` folder
- No crash

### Test 4.2: Uncategorized Receipts
**Steps:**
1. Create receipt
2. Leave category as "Uncategorized"
3. Save receipt

**Expected Results:**
- Receipt saved to `Documents/Expenses Receipts/` folder

### Test 4.3: Special Characters in Category
**Steps:**
1. Create custom category with special characters: "F&B", "Office/Home"
2. Save receipts with these categories

**Expected Results:**
- All saved to `Documents/Expenses Receipts/` folder
- File names properly sanitized

### Test 4.4: Very Long Category Name
**Steps:**
1. Create category with very long name (100+ characters)
2. Save receipt

**Expected Results:**
- Receipt saves successfully
- File name generated correctly
- No buffer overflow or crash

## Test Scenario 5: App Restart and Persistence

### Test 5.1: Custom Folders Persist After Restart
**Steps:**
1. Set custom Fuel and Other folders
2. Force close the app
3. Restart the app
4. Check Settings

**Expected Results:**
- Custom folder URIs still displayed in Settings
- Can still save to custom folders
- Permissions still valid

### Test 5.2: Receipts Remain Accessible After Restart
**Steps:**
1. Save receipts to custom folders
2. Restart app
3. View receipts in app

**Expected Results:**
- All receipts visible
- Images load correctly
- File paths still valid

## Test Scenario 6: Performance and Concurrent Operations

### Test 6.1: Multiple Receipts Quickly
**Steps:**
1. Create and save 10 receipts in quick succession
2. Alternate categories (Fuel, Food, Fuel, Travel, etc.)

**Expected Results:**
- All receipts saved to correct folders
- No race conditions
- No data loss

### Test 6.2: Large Receipt Files
**Steps:**
1. Create receipt with high-resolution image (10+ MB)
2. Save with category "Fuel"

**Expected Results:**
- File saves successfully
- Correct folder routing maintained
- Reasonable performance

## Verification Commands

Use ADB to verify file locations:

```bash
# List Fuel receipts in default folder
adb shell ls -la /sdcard/Android/data/com.expenses.app/files/Documents/Fuel Receipts/

# List Other receipts in default folder
adb shell ls -la /sdcard/Android/data/com.expenses.app/files/Documents/Expenses Receipts/

# Pull logs to check folder routing
adb logcat | grep FileUtils
adb logcat | grep ReceiptViewModel
```

## Expected Log Patterns

**Fuel Receipt to Default Folder:**
```
D/ReceiptViewModel: Category 'Fuel' matched as Fuel - checking for custom Fuel folder
D/ReceiptViewModel: No custom folder set for category 'Fuel', will use default
D/FileUtils: Starting saveReceiptImage for category: Fuel
D/FileUtils: Category 'Fuel' matched as Fuel - using Documents/Fuel Receipts folder
D/FileUtils: Using default category folder: /storage/.../Documents/Fuel Receipts
I/FileUtils: Successfully saved 123456 bytes to: /storage/.../Documents/Fuel Receipts/01.01.2024 - Test - 45.67.jpg
```

**Other Receipt to Custom Folder:**
```
D/ReceiptViewModel: Category 'Food' is not Fuel - checking for custom Other folder
D/ReceiptViewModel: Custom folder found for category 'Food': content://...
D/FileUtils: Starting saveReceiptImage for category: Food
D/FileUtils: Attempting to save to custom folder: content://...
I/FileUtils: Successfully saved to custom folder
```

## Pass/Fail Criteria

**PASS Criteria:**
- All Fuel receipts go to Fuel folder (default or custom)
- All non-Fuel receipts go to Other folder (default or custom)
- Case-insensitive Fuel matching works
- Custom folders take precedence over defaults
- Fallback to defaults works when custom folders fail
- No crashes or data loss in any scenario
- Permissions properly managed

**FAIL Criteria:**
- Fuel receipts in Other folder or vice versa
- Custom folders not used when set
- App crashes during save
- Receipts lost or not saved
- Permissions not properly released
- No fallback when custom folder fails
