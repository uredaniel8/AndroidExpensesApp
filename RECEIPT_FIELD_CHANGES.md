# Receipt Field Changes - Manual Date Selection and Blank Fields

## Overview
This document describes the changes made to improve user control when adding receipts to the AndroidExpensesApp. The changes address two key requirements:

1. **Manual Date Selection**: Users can now manually select the receipt date using a calendar picker
2. **Blank Editable Fields**: Merchant, Total Amount, and VAT fields start blank instead of being pre-populated with OCR values

## Problem Statement
Previously, when adding a new receipt:
- The date was automatically set from OCR detection or current date, but couldn't be changed
- Merchant, Total Amount, and VAT fields were pre-populated with OCR-detected values
- If OCR detected incorrect values, users had to manually correct them
- There was no way to select a different date for the receipt

## Solution

### 1. Blank Fields on Receipt Creation
**File**: `app/src/main/java/com/expenses/app/ui/ReceiptViewModel.kt`

Modified the `processReceipt()` function to create receipts with blank/default values:

```kotlin
val receipt = Receipt(
    receiptDate = System.currentTimeMillis(),  // Current time instead of OCR date
    merchant = null,                            // Blank instead of OCR value
    totalAmount = 0.0,                         // 0 instead of OCR value
    vatAmount = null,                          // Blank instead of OCR value
    currency = ocrResult.currency ?: CurrencyUtils.getDefaultCurrency(),
    category = "Uncategorized",
    notes = null,
    tags = emptyList(),
    ocrRawText = ocrResult.rawText,            // OCR data still captured for reference
    ocrConfidence = ocrResult.confidence,
    originalUri = imageUri.toString()
)
```

**Benefits**:
- Users start with a clean slate and enter only accurate information
- Reduces errors from incorrect OCR detection
- OCR results are still available in `ocrRawText` for reference if needed

### 2. Interactive Date Picker
**File**: `app/src/main/java/com/expenses/app/ui/screens/EditReceiptScreen.kt`

#### Changes Made:
1. **Added date state variable**:
   ```kotlin
   var receiptDate by remember { mutableStateOf(receipt?.receiptDate ?: System.currentTimeMillis()) }
   var showDatePicker by remember { mutableStateOf(false) }
   ```

2. **Replaced read-only date display with interactive field**:
   ```kotlin
   val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
   OutlinedTextField(
       value = dateFormat.format(Date(receiptDate)),
       onValueChange = {},
       label = { Text("Receipt Date") },
       readOnly = true,
       trailingIcon = {
           IconButton(onClick = { showDatePicker = true }) {
               Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
           }
       },
       modifier = Modifier.fillMaxWidth()
   )
   ```

3. **Added Material3 DatePickerDialog**:
   ```kotlin
   if (showDatePicker) {
       val datePickerState = rememberDatePickerState(
           initialSelectedDateMillis = receiptDate
       )
       DatePickerDialog(
           onDismissRequest = { showDatePicker = false },
           confirmButton = {
               TextButton(
                   onClick = {
                       datePickerState.selectedDateMillis?.let { selectedDate ->
                           receiptDate = selectedDate
                       }
                       showDatePicker = false
                   }
               ) {
                   Text("OK")
               }
           },
           dismissButton = {
               TextButton(onClick = { showDatePicker = false }) {
                   Text("Cancel")
               }
           }
       ) {
           DatePicker(state = datePickerState)
       }
   }
   ```

4. **Updated save logic to include selected date**:
   ```kotlin
   val updatedReceipt = it.copy(
       receiptDate = receiptDate,  // Save the manually selected date
       merchant = merchant.takeIf { it.isNotBlank() },
       totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
       vatAmount = vatAmount.toDoubleOrNull(),
       // ... other fields
   )
   ```

**Benefits**:
- Users can select any date using a standard Material3 calendar picker
- Visual calendar icon indicates the field is interactive
- Consistent with Android Material Design guidelines
- Handles timezone correctly with UTC timestamps

### 3. Performance Optimization
Wrapped `SimpleDateFormat` in a `remember` block to avoid recreation on every recomposition:
```kotlin
val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
```

## User Experience

### Before Changes
1. User takes/selects receipt photo
2. OCR processes and auto-populates fields with potentially incorrect data
3. User must manually correct any errors
4. Date is locked and cannot be changed

### After Changes
1. User takes/selects receipt photo
2. Receipt is created with blank fields and current date
3. User can manually select the correct date via calendar picker
4. User manually enters accurate values for Merchant, Amount, and VAT
5. OCR text is still available in raw form if needed for reference

## Technical Details

### Date Handling
- Dates are stored as Long (milliseconds since epoch) in the Receipt entity
- DatePicker returns UTC midnight timestamps
- SimpleDateFormat formats dates using the user's locale
- Consistent date format across the app: "yyyy-MM-dd"

### Field Defaults
- `merchant`: null (empty string in UI)
- `totalAmount`: 0.0 (empty string in UI)
- `vatAmount`: null (empty string in UI)
- `receiptDate`: System.currentTimeMillis() (current date/time)
- `currency`: Auto-detected from OCR or system default

### OCR Data Preservation
While visible fields are blank, OCR data is preserved in:
- `ocrRawText`: Complete OCR text output
- `ocrConfidence`: OCR confidence score
- This allows users to reference OCR data if needed without forcing it into editable fields

## Files Modified
1. `app/src/main/java/com/expenses/app/ui/ReceiptViewModel.kt`
   - Modified `processReceipt()` to set blank default values
   
2. `app/src/main/java/com/expenses/app/ui/screens/EditReceiptScreen.kt`
   - Added calendar icon import
   - Added date state variables
   - Replaced read-only date text with interactive TextField
   - Added DatePickerDialog component
   - Updated save logic to include selected date
   - Optimized SimpleDateFormat with remember block

## Testing Recommendations
1. Test adding a new receipt and verify fields are blank
2. Test selecting different dates using the date picker
3. Test saving a receipt with manually entered data
4. Test timezone handling across different user locales
5. Verify OCR raw text is still captured
6. Verify performance with no lag when opening EditReceiptScreen

## Future Enhancements
Potential improvements for future releases:
1. Add optional "Use OCR Data" button to pre-fill fields when desired
2. Show OCR confidence indicator for each field
3. Add field-by-field comparison between OCR and manual entry
4. Implement date range validation
5. Add keyboard date entry option alongside calendar picker
