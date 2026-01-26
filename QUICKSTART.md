# Quick Start Guide

## Prerequisites

Before you begin, ensure you have:
- **Android Studio** Hedgehog (2023.1.1) or later installed
- **JDK 17** (included with Android Studio)
- **Android SDK** with API levels 26-34
- An **Android device** or **emulator** for testing

## Getting Started in 5 Minutes

### Step 1: Clone the Repository
```bash
git clone https://github.com/uredaniel8/AndroidExpensesApp.git
cd AndroidExpensesApp
```

### Step 2: Open in Android Studio
1. Launch Android Studio
2. Select **"Open"** from the welcome screen
3. Navigate to the cloned repository folder
4. Click **"OK"**

### Step 3: Sync Gradle
Android Studio will automatically start syncing Gradle dependencies. This may take 2-5 minutes on first run.

**What's happening?**
- Downloading Android Gradle Plugin
- Downloading Kotlin compiler
- Downloading Jetpack Compose libraries
- Downloading Room, CameraX, and ML Kit dependencies

### Step 4: Run the App
1. Connect an Android device via USB **OR** start an emulator
2. Click the **green play button** (▶️) in the toolbar
3. Select your device/emulator
4. Wait for the app to build and install

**First Run Time**: 1-3 minutes (subsequent runs are faster)

### Step 5: Try the Features!

#### Add Your First Receipt
1. Tap the **camera button** (floating action button)
2. Grant camera permission when prompted
3. Take a photo of a receipt
4. Watch OCR extract data automatically
5. Edit any incorrect fields
6. Tap **Save** (top right)

#### View Your Receipts
- See all receipts on the home screen
- Tap any receipt to view/edit details
- Status indicators show: Needs Review, Exported, or Failed

#### Generate a Report
1. Tap the **chart icon** (top right)
2. View summary statistics
3. Tap the **download icon** to export CSV
4. Choose a save location
5. Open the CSV in Excel, Sheets, or any spreadsheet app

## Troubleshooting

### Gradle Sync Fails
**Problem**: "Failed to download dependencies"
**Solution**: 
- Check your internet connection
- Try again: File → Sync Project with Gradle Files
- If behind a proxy, configure in File → Settings → HTTP Proxy

### App Won't Install on Device
**Problem**: "Installation failed"
**Solution**:
- Enable USB Debugging on your device (Settings → Developer Options)
- Accept the debugging authorization popup on your device
- Ensure minimum Android 8.0 (API 26)

### Camera Not Working
**Problem**: "Camera permission denied"
**Solution**:
- Grant camera permission when prompted
- Manually enable: Device Settings → Apps → Expenses & Receipts → Permissions → Camera

### OCR Not Extracting Data
**Problem**: No data extracted from receipt
**Solution**:
- Ensure good lighting when taking photo
- Keep receipt flat and in focus
- Use a receipt with clear, printed text
- Edit fields manually if needed

### Build Errors
**Problem**: Compilation errors
**Solution**:
1. Clean project: Build → Clean Project
2. Rebuild: Build → Rebuild Project
3. Invalidate caches: File → Invalidate Caches / Restart
4. Check Java version: Android Studio should use JDK 17

## Project Structure at a Glance

```
AndroidExpensesApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/expenses/app/
│   │   │   ├── MainActivity.kt         # App entry point
│   │   │   ├── data/                   # Database & entities
│   │   │   ├── ui/                     # Screens & ViewModels
│   │   │   └── util/                   # OCR & utilities
│   │   ├── res/                        # Resources (UI strings, icons)
│   │   └── AndroidManifest.xml         # App configuration
│   └── build.gradle.kts                # App dependencies
├── build.gradle.kts                    # Project configuration
├── README.md                           # Main documentation
├── DEVELOPER_GUIDE.md                  # Detailed dev docs
└── ARCHITECTURE.md                     # Architecture details
```

## Key Files to Know

- **MainActivity.kt**: App starts here
- **HomeScreen.kt**: Main screen you see first
- **AddReceiptScreen.kt**: Camera/gallery selection
- **EditReceiptScreen.kt**: Form for editing receipts
- **ReportsScreen.kt**: Statistics and CSV export
- **ReceiptViewModel.kt**: Business logic and state
- **OcrProcessor.kt**: ML Kit OCR processing
- **Receipt.kt**: Data model for receipts

## Development Workflow

### Making Changes
1. Create a new branch: `git checkout -b feature/my-feature`
2. Make your changes
3. Test on device/emulator
4. Commit: `git commit -m "Add my feature"`
5. Push: `git push origin feature/my-feature`

### Running Tests (Future)
```bash
./gradlew test           # Unit tests
./gradlew connectedTest  # Instrumented tests
```

### Building Release APK
1. Build → Generate Signed Bundle / APK
2. Choose APK
3. Create or use existing keystore
4. Select release build variant
5. Wait for build to complete
6. Find APK in `app/release/`

## Next Steps

### For Developers
- Read **DEVELOPER_GUIDE.md** for detailed documentation
- Review **ARCHITECTURE.md** to understand the design
- Check **IMPLEMENTATION_SUMMARY.md** for what's implemented

### For Product Managers
- Read **README.md** for feature overview
- Try all features on a device
- Review the "Future Enhancements" section
- Provide feedback on UX

### For QA/Testers
- Test on multiple devices (different Android versions)
- Test with various receipt types
- Verify OCR accuracy
- Check CSV export in different apps
- Report issues on GitHub

## Common User Flows

### Flow 1: Add Receipt from Camera
```
Home → Tap Camera FAB → Grant Permission → Take Photo → Auto-extract → Edit if needed → Save
```

### Flow 2: Add Receipt from Gallery
```
Home → Tap Upload FAB → Select Image → Auto-extract → Edit if needed → Save
```

### Flow 3: Edit Existing Receipt
```
Home → Tap Receipt Card → Edit Fields → Tap Save → Back to Home
```

### Flow 4: Export Receipts
```
Home → Tap Reports Icon → View Summary → Tap Export → Choose Location → Save CSV
```

## Performance Tips

### For Testing
- Use a physical device for camera testing (emulator camera is slow)
- Test OCR with real receipts for accurate results
- Use recent devices (Android 10+) for best performance

### For Development
- Enable **R8 minification** for release builds
- Use **ProGuard** rules if needed
- Profile with **Android Profiler** (View → Tool Windows → Profiler)

## Resources

### Documentation
- **In this repo**:
  - README.md - Features and tech stack
  - DEVELOPER_GUIDE.md - Development details
  - ARCHITECTURE.md - Design patterns
  - IMPLEMENTATION_SUMMARY.md - What's built

### External Resources
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [ML Kit Documentation](https://developers.google.com/ml-kit)
- [CameraX Guide](https://developer.android.com/training/camerax)

## Getting Help

### Issues?
1. Check this guide first
2. Read the DEVELOPER_GUIDE.md
3. Search existing issues on GitHub
4. Create a new issue with:
   - Device/emulator info
   - Android version
   - Steps to reproduce
   - Screenshots or logs

### Questions?
- Open a discussion on GitHub
- Check Android Developer documentation
- Review the code comments

## Keyboard Shortcuts (Android Studio)

- **Build**: Ctrl+F9 (Cmd+F9 on Mac)
- **Run**: Shift+F10 (Ctrl+R on Mac)
- **Format Code**: Ctrl+Alt+L (Cmd+Opt+L on Mac)
- **Find**: Ctrl+F (Cmd+F on Mac)
- **Search Everywhere**: Double Shift

## Success Checklist

After setup, verify:
- [ ] Project builds without errors
- [ ] App installs on device/emulator
- [ ] Home screen displays
- [ ] Can add receipt from camera
- [ ] Can add receipt from gallery
- [ ] OCR extracts some data
- [ ] Can edit receipt
- [ ] Can view reports
- [ ] Can export CSV

**All checked?** 🎉 You're ready to develop!

## What's Next?

### Explore the Code
- Start with `MainActivity.kt`
- Understand the navigation flow
- Review the `HomeScreen.kt` composable
- Check out `OcrProcessor.kt` for ML Kit usage

### Try Customizing
- Change app colors in `Color.kt`
- Add a new category in `EditReceiptScreen.kt`
- Modify the CSV export format in `CsvExporter.kt`
- Add new fields to the Receipt entity

### Build Something
- Add a new screen
- Implement a new export format (JSON, PDF)
- Add receipt search functionality
- Implement data backup/restore

**Happy Coding!** 🚀
