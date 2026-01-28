package com.expenses.app.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

/**
 * Manages folder preferences for storing receipts.
 * Persists custom folder URIs selected by the user.
 */
class FolderPreferences(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "folder_preferences"
        private const val KEY_FUEL_FOLDER_URI = "fuel_folder_uri"
        private const val KEY_OTHER_FOLDER_URI = "other_folder_uri"
    }
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Saves the URI of the folder for fuel receipts.
     */
    fun setFuelFolderUri(uri: Uri?) {
        preferences.edit().apply {
            if (uri != null) {
                putString(KEY_FUEL_FOLDER_URI, uri.toString())
            } else {
                remove(KEY_FUEL_FOLDER_URI)
            }
            apply()
        }
    }
    
    /**
     * Gets the URI of the folder for fuel receipts.
     * Returns null if no custom folder has been set.
     */
    fun getFuelFolderUri(): Uri? {
        val uriString = preferences.getString(KEY_FUEL_FOLDER_URI, null)
        return uriString?.let { Uri.parse(it) }
    }
    
    /**
     * Saves the URI of the folder for other receipts.
     */
    fun setOtherFolderUri(uri: Uri?) {
        preferences.edit().apply {
            if (uri != null) {
                putString(KEY_OTHER_FOLDER_URI, uri.toString())
            } else {
                remove(KEY_OTHER_FOLDER_URI)
            }
            apply()
        }
    }
    
    /**
     * Gets the URI of the folder for other receipts.
     * Returns null if no custom folder has been set.
     */
    fun getOtherFolderUri(): Uri? {
        val uriString = preferences.getString(KEY_OTHER_FOLDER_URI, null)
        return uriString?.let { Uri.parse(it) }
    }
    
    /**
     * Checks if a custom folder has been set for fuel receipts.
     */
    fun hasFuelFolder(): Boolean {
        return preferences.contains(KEY_FUEL_FOLDER_URI)
    }
    
    /**
     * Checks if a custom folder has been set for other receipts.
     */
    fun hasOtherFolder(): Boolean {
        return preferences.contains(KEY_OTHER_FOLDER_URI)
    }
    
    /**
     * Clears all folder preferences and releases URI permissions.
     * Note: This method requires a Context to release URI permissions.
     * Call clearWithoutReleasingPermissions() if you want to clear preferences only.
     */
    fun clear(context: Context) {
        // Get URIs before clearing
        val fuelUri = getFuelFolderUri()
        val otherUri = getOtherFolderUri()
        
        // Release permissions
        val contentResolver = context.contentResolver
        fuelUri?.let { uri ->
            try {
                contentResolver.releasePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if permission already released
            }
        }
        otherUri?.let { uri ->
            try {
                contentResolver.releasePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if permission already released
            }
        }
        
        // Clear preferences
        preferences.edit().clear().apply()
    }
    
    /**
     * Clears all folder preferences without releasing URI permissions.
     * Use this if you're managing permissions separately.
     */
    fun clearWithoutReleasingPermissions() {
        preferences.edit().clear().apply()
    }
}
