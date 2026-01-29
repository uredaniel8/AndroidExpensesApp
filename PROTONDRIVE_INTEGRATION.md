# ProtonDrive Integration Guide

## Overview

The Android Expenses App now supports automatic receipt uploads to ProtonDrive with category-based folder organization.

## Features

### Automatic Folder Organization
- **Fuel Receipts**: Uploaded to `Documents/Fuel Receipts/` folder
- **Other Receipts**: Uploaded to `Documents/Expenses Receipts/` folder

### Upload Process
1. Capture or import a receipt
2. Edit and categorize the receipt
3. Tap "Upload to ProtonDrive" button
4. Receipt image is uploaded to the appropriate folder based on category

## Setup Instructions

### Prerequisites
1. ProtonDrive account
2. API access enabled in Proton account settings
3. Valid access token

### Configuration Steps

1. **Access Settings**
   - Open the app
   - Tap the Settings icon (⚙️) in the top right corner

2. **Enable ProtonDrive**
   - Toggle "Enable ProtonDrive Integration" switch
   - Enter your ProtonDrive access token
   - Tap "Save Configuration"

3. **Getting an Access Token**
   
   For development/testing:
   ```
   1. Visit https://account.proton.me
   2. Navigate to "Account Settings"
   3. Go to "Security" → "API Access"
   4. Generate an access token for ProtonDrive
   5. Copy the token for use in the app
   ```

   **Note**: In production, implement proper OAuth2 authentication flow.

## Usage

### Uploading a Receipt

1. **From Edit Screen**:
   - Open any receipt
   - Tap "Upload to ProtonDrive" button
   - Wait for upload confirmation

2. **Upload Status**:
   - Green checkmark: Successfully uploaded
   - Red error: Upload failed (check token/permissions)

### Receipt Status Indicators

- **Needs Review**: Not yet uploaded
- **Exported**: Successfully uploaded to ProtonDrive
- **Export Failed**: Upload attempt failed

## File Naming Convention

Uploaded files follow this pattern:
```
DD.MM.YYYY - MERCHANT - TOTAL.jpg
```

Example: `28.01.2026 - Shell Gas Station - 45.50.jpg`

## Folder Structure on ProtonDrive

```
ProtonDrive/
└── Receipts/
    ├── Fuel/
    │   ├── 28.01.2026 - Shell - 45.50.jpg
    │   └── 29.01.2026 - BP - 52.00.jpg
    └── Other/
        ├── 28.01.2026 - Hotel - 120.00.jpg
        └── 29.01.2026 - Restaurant - 35.75.jpg
```

## Technical Implementation

### Components

1. **ProtonDriveService.kt**
   - Handles API communication
   - Manages file uploads
   - Creates folder structure

2. **ReceiptViewModel.kt**
   - Coordinates upload process
   - Updates receipt status
   - Error handling

3. **SettingsScreen.kt**
   - Configuration UI
   - Token management

### API Endpoints Used

- Upload file: `POST /files/upload?path=/{folder}/{filename}`
- Create folder: `POST /folders?path=/{folder}`

### Dependencies

```kotlin
// HTTP Client for ProtonDrive API
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

## Security Considerations

### Current Implementation
- Access tokens are stored in memory (not persisted)
- Basic token validation
- HTTPS communication only

### Recommended Improvements
1. **Token Storage**: Use Android Keystore for secure storage
2. **OAuth2 Flow**: Implement proper authentication flow
3. **Token Refresh**: Automatic token renewal
4. **Encryption**: Encrypt tokens at rest

## Troubleshooting

### Common Issues

**"ProtonDrive is not configured"**
- Solution: Go to Settings and configure access token

**"Upload failed: 401"**
- Solution: Token expired or invalid, generate new token

**"Upload failed: 403"**
- Solution: Check API permissions in Proton account settings

**"Upload failed: 404"**
- Solution: Folder structure not created, check permissions

### Debug Steps

1. Verify token is valid
2. Check Proton account API access settings
3. Test API access with Postman/cURL
4. Review app logs for detailed errors

## Future Enhancements

1. **OAuth2 Integration**: Full authentication flow
2. **Batch Upload**: Upload multiple receipts at once
3. **Sync Status**: Real-time sync indicators
4. **Conflict Resolution**: Handle duplicate files
5. **Offline Queue**: Queue uploads when offline
6. **Progress Indicator**: Show upload progress
7. **Auto-upload**: Automatic upload on save

## API Rate Limits

ProtonDrive API limits:
- Check ProtonDrive documentation for current rate limits
- File size limit: Varies by plan (typically 25GB per file)

## Support

For issues or questions:
1. Check documentation files
2. Verify Proton account configuration
3. Review app logs
4. Contact support with error details

## Version History

- **v2.0** (Jan 2026): ProtonDrive integration
  - Replaced OneDrive with ProtonDrive
  - Category-based folder organization
  - Manual upload from edit screen
  - Token configuration in settings
