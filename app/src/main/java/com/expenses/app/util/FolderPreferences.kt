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
     * Clears all folder preferences.
     */
    fun clear() {
        preferences.edit().clear().apply()
    }
}
