# Custom Folder Selection - UI Changes

## Settings Screen UI Modifications

### New Section: "Custom Folder Selection"

The Settings screen now includes a new section below the "Local Storage" section with the following UI elements:

### Layout Structure

```
┌─────────────────────────────────────────────────────┐
│ Settings                                        [←] │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Local Storage                                       │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Configure local storage to automatically save  │ │
│ │ receipts:                                       │ │
│ │ • Fuel receipts → Documents/Fuel Receipts folder         │ │
│ │ • Other receipts → Documents/Expenses Receipts folder       │ │
│ │ Files are stored in the app's external         │ │
│ │ storage directory.                              │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ Custom Folder Selection                             │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Choose custom folders for storing receipts.    │ │
│ │ By default, receipts are saved to the app's    │ │
│ │ internal storage.                               │ │
│ │ ─────────────────────────────────────────────── │ │
│ │                                                 │ │
│ │ ┌───────────────────────────────────────────┐  │ │
│ │ │ Fuel Receipts Folder            📁       │  │ │
│ │ │ Using default folder                      │  │ │
│ │ │                                           │  │ │
│ │ │ [ Select Folder ]                         │  │ │
│ │ └───────────────────────────────────────────┘  │ │
│ │                                                 │ │
│ │ ┌───────────────────────────────────────────┐  │ │
│ │ │ Other Receipts Folder           📁       │  │ │
│ │ │ Using default folder                      │  │ │
│ │ │                                           │  │ │
│ │ │ [ Select Folder ]                         │  │ │
│ │ └───────────────────────────────────────────┘  │ │
│ │                                                 │ │
│ │ Why choose custom folders?                     │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ Enable Local Storage                    [ OFF ]    │
│                                                     │
│ Where are files saved?                             │
│                                                     │
│ [ Save Configuration ]                             │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### After Selecting Custom Folders

```
┌─────────────────────────────────────────────────────┐
│ Custom Folder Selection                             │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Choose custom folders for storing receipts.    │ │
│ │ By default, receipts are saved to the app's    │ │
│ │ internal storage.                               │ │
│ │ ─────────────────────────────────────────────── │ │
│ │                                                 │ │
│ │ ┌───────────────────────────────────────────┐  │ │
│ │ │ Fuel Receipts Folder            📁       │  │ │
│ │ │ ✓ Custom folder selected                 │  │ │
│ │ │                                           │  │ │
│ │ │ [ Select Folder ]  [ Reset ]              │  │ │
│ │ └───────────────────────────────────────────┘  │ │
│ │                                                 │ │
│ │ ┌───────────────────────────────────────────┐  │ │
│ │ │ Other Receipts Folder           📁       │  │ │
│ │ │ ✓ Custom folder selected                 │  │ │
│ │ │                                           │  │ │
│ │ │ [ Select Folder ]  [ Reset ]              │  │ │
│ │ └───────────────────────────────────────────┘  │ │
│ │                                                 │ │
│ │ Why choose custom folders?                     │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

## UI Components Details

### 1. Fuel Receipts Folder Card
- **Type**: OutlinedCard with padding
- **Layout**: Column with 8dp spacing
- **Components**:
  - **Header Row**:
    - Left: Title "Fuel Receipts Folder" (TitleMedium)
    - Right: Folder icon (📁) in primary color
  - **Status Text**:
    - When no folder selected: "Using default folder" (gray)
    - When folder selected: "Custom folder selected" (primary color)
  - **Action Row**:
    - Primary button: "Select Folder" (fills width)
    - Reset button: "Reset" (outlined, only visible when folder selected)

### 2. Other Receipts Folder Card
- **Type**: OutlinedCard with padding
- **Layout**: Column with 8dp spacing
- **Components**: Same structure as Fuel Receipts Folder Card
  - Title: "Other Receipts Folder"
  - Status and action buttons identical to fuel card

### 3. Information Button
- **Type**: TextButton
- **Text**: "Why choose custom folders?"
- **Action**: Opens information dialog

### 4. Information Dialog (when "Why choose custom folders?" clicked)
```
┌─────────────────────────────────────────────────────┐
│ Custom Folder Selection                             │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Custom folders allow you to:                       │
│ • Choose where receipts are stored on your device  │
│ • Easily access receipts from other apps like      │
│   file managers                                     │
│ • Back up receipts to cloud storage more easily    │
│ • Organize receipts with your own folder structure │
│                                                     │
│ Note:                                               │
│ • The app needs permission to write to the         │
│   selected folder                                   │
│ • If a folder is deleted, the app will fall back   │
│   to default storage                                │
│ • Custom folders persist across app restarts       │
│                                                     │
│                                          [ OK ]     │
└─────────────────────────────────────────────────────┘
```

## Folder Picker Dialog (System Dialog)

When user taps "Select Folder", the system's OpenDocumentTree picker appears:

```
┌─────────────────────────────────────────────────────┐
│ Select a folder                                 [×] │
├─────────────────────────────────────────────────────┤
│                                                     │
│  📁 Downloads                                       │
│  📁 Documents                                       │
│  📁 Pictures                                        │
│  📁 DCIM                                            │
│  📁 My Receipts                                     │
│                                                     │
│                         [Cancel]  [Use this folder] │
└─────────────────────────────────────────────────────┘
```

## User Interactions

### Selecting a Folder
1. User taps "Select Folder" button
2. System folder picker dialog opens
3. User navigates to desired folder
4. User taps "Use this folder"
5. App requests persistent permissions
6. Status text changes to "Custom folder selected" (green/primary color)
7. "Reset" button appears next to "Select Folder"

### Resetting a Folder
1. User taps "Reset" button (only visible when custom folder is set)
2. Status text changes to "Using default folder" (gray)
3. "Reset" button disappears
4. App releases URI permissions
5. App clears saved preference
6. Future receipts save to default app folder

### Error States
When folder selection fails, a Snackbar or Toast appears with error message:
- "Cannot access selected folder. Please try selecting a different folder."
- "Selected folder does not support persistent access. Please choose a folder from your device storage."
- "Failed to obtain folder access: [reason]"

## Visual Design Elements

### Colors
- **Primary actions**: Material3 primary color scheme
- **Status - Custom selected**: Primary color
- **Status - Default**: OnSurfaceVariant (gray)
- **Folder icon**: Primary color
- **Card borders**: Outlined style with default border color

### Typography
- **Section header**: HeadlineSmall (Material3)
- **Card title**: TitleMedium
- **Description text**: BodyMedium
- **Status text**: BodySmall
- **Button text**: Default button style

### Spacing
- **Section spacing**: 16dp
- **Card padding**: 16dp
- **Card internal spacing**: 12dp
- **Row spacing**: 8dp
- **Icon-text spacing**: Standard Material3 spacing

### Icons
- **Folder icon**: Icons.Default.Folder
- **Back arrow**: Icons.Default.ArrowBack (in top bar)

## Accessibility

- All buttons have proper click targets (minimum 48dp)
- Status text uses contrasting colors (primary vs onSurfaceVariant)
- Folder icon has contentDescription = "Folder"
- All interactive elements are keyboard navigable
- Screen reader friendly with proper semantic structure

## Responsive Behavior

- Cards fill width of screen
- Text wraps appropriately
- Vertical scrolling enabled for small screens
- Buttons scale with screen width
- Works in both portrait and landscape orientations

## State Persistence

The UI reflects the persisted state on app restart:
- If custom folder previously selected → Shows "Custom folder selected" + Reset button
- If no custom folder → Shows "Using default folder" + no Reset button
- State loads from SharedPreferences on ViewModel initialization
