# OneDrive Integration Guide

## Overview

The Android Expenses App now supports automatic receipt uploads to Microsoft OneDrive with category-based folder organization.

## Features

### Automatic Folder Organization
- **Fuel Receipts**: Uploaded to `Receipts/Fuel/` folder
- **Other Receipts**: Uploaded to `Receipts/Other/` folder

### Upload Process
1. Capture or import a receipt
2. Edit and categorize the receipt
3. Tap "Upload to OneDrive" button
4. Receipt image is uploaded to the appropriate folder based on category

## Setup Instructions

### Prerequisites
1. Microsoft Azure account
2. Registered application in Azure AD
3. Microsoft Graph API permissions configured
4. Valid access token

### Configuration Steps

1. **Access Settings**
   - Open the app
   - Tap the Settings icon (⚙️) in the top right corner

2. **Enable OneDrive**
   - Toggle "Enable OneDrive Integration" switch
   - Enter your OneDrive access token
   - Tap "Save Configuration"

3. **Getting an Access Token**
   
   For development/testing:
   ```
   1. Visit https://portal.azure.com
   2. Go to "Azure Active Directory" → "App registrations"
   3. Create a new registration or select existing
   4. Add Microsoft Graph API permissions:
      - Files.ReadWrite
      - Files.ReadWrite.All (for folder creation)
   5. Generate access token using authentication flow
   ```

   **Note**: In production, implement proper OAuth2 authentication flow with MSAL library.

## Usage

### Uploading a Receipt

1. **From Edit Screen**:
   - Open any receipt
   - Tap "Upload to OneDrive" button
   - Wait for upload confirmation

2. **Upload Status**:
   - Green checkmark: Successfully uploaded
   - Red error: Upload failed (check token/permissions)

### Receipt Status Indicators

- **Needs Review**: Not yet uploaded
- **Exported**: Successfully uploaded to OneDrive
- **Export Failed**: Upload attempt failed

## File Naming Convention

Uploaded files follow this pattern:
```
DD.MM.YYYY - MERCHANT - TOTAL.jpg
```

Example: `28.01.2026 - Shell Gas Station - 45.50.jpg`

## Folder Structure on OneDrive

```
OneDrive/
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

1. **OneDriveService.kt**
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

- Upload file: `PUT /me/drive/root:/{folder}/{filename}:/content`
- Check folder: `GET /me/drive/root:/{folder}`

### Dependencies

```kotlin
// Microsoft Graph SDK
implementation("com.microsoft.graph:microsoft-graph:6.7.0")
implementation("com.microsoft.identity.client:msal:5.0.0")

// HTTP Client
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

**"OneDrive is not configured"**
- Solution: Go to Settings and configure access token

**"Upload failed: 401"**
- Solution: Token expired, generate new token

**"Upload failed: 403"**
- Solution: Check API permissions in Azure AD

**"Upload failed: 404"**
- Solution: Folder structure not created, check permissions

### Debug Steps

1. Verify token is valid
2. Check Azure AD permissions
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

Microsoft Graph API limits:
- 10,000 requests per 10 minutes per app
- File size limit: 4GB (250MB recommended)

## Support

For issues or questions:
1. Check Azure AD configuration
2. Verify API permissions
3. Review app logs
4. Contact support with error details

## Version History

- **v1.0** (Jan 2026): Initial OneDrive integration
  - Category-based folder organization
  - Manual upload from edit screen
  - Basic token configuration
