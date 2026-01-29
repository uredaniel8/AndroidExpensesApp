package com.expenses.app.util

import android.content.Context
import android.net.Uri
import com.expenses.app.data.Receipt
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File

/**
 * Unit tests for FileUtils to validate category-based folder routing.
 * 
 * These tests verify that:
 * 1. Fuel receipts are routed to the "Receipts/Fuel" folder
 * 2. Non-fuel receipts are routed to the "Receipts/Other" folder
 * 3. Category matching is case-insensitive
 * 4. Various category names are correctly routed
 */
class FileUtilsTest {

    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock the external files directory
        val mockExternalFilesDir = File("/mock/external/files")
        `when`(mockContext.getExternalFilesDir(null)).thenReturn(mockExternalFilesDir)
    }

    @Test
    fun `getCategoryFolder should return Fuel folder for Fuel category`() {
        // Arrange
        val category = "Fuel"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Fuel'", folder.path.endsWith("Fuel"))
        assertTrue("Folder path should contain 'Receipts'", folder.path.contains("Receipts"))
    }

    @Test
    fun `getCategoryFolder should return Fuel folder for fuel category (lowercase)`() {
        // Arrange
        val category = "fuel"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Fuel'", folder.path.endsWith("Fuel"))
    }

    @Test
    fun `getCategoryFolder should return Fuel folder for FUEL category (uppercase)`() {
        // Arrange
        val category = "FUEL"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Fuel'", folder.path.endsWith("Fuel"))
    }

    @Test
    fun `getCategoryFolder should return Fuel folder for FuEl category (mixed case)`() {
        // Arrange
        val category = "FuEl"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Fuel'", folder.path.endsWith("Fuel"))
    }

    @Test
    fun `getCategoryFolder should return Other folder for Food category`() {
        // Arrange
        val category = "Food"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Other'", folder.path.endsWith("Other"))
        assertTrue("Folder path should contain 'Receipts'", folder.path.contains("Receipts"))
    }

    @Test
    fun `getCategoryFolder should return Other folder for Travel category`() {
        // Arrange
        val category = "Travel"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Other'", folder.path.endsWith("Other"))
    }

    @Test
    fun `getCategoryFolder should return Other folder for Entertainment category`() {
        // Arrange
        val category = "Entertainment"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Other'", folder.path.endsWith("Other"))
    }

    @Test
    fun `getCategoryFolder should return Other folder for Office Supplies category`() {
        // Arrange
        val category = "Office Supplies"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Other'", folder.path.endsWith("Other"))
    }

    @Test
    fun `getCategoryFolder should return Other folder for empty category`() {
        // Arrange
        val category = ""
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Other'", folder.path.endsWith("Other"))
    }

    @Test
    fun `getCategoryFolder should return Other folder for Uncategorized`() {
        // Arrange
        val category = "Uncategorized"
        
        // Act
        val folder = FileUtils.getCategoryFolder(mockContext, category)
        
        // Assert
        assertTrue("Folder path should end with 'Other'", folder.path.endsWith("Other"))
    }

    @Test
    fun `getCategoryFolder should return consistent path structure`() {
        // Act
        val fuelFolder = FileUtils.getCategoryFolder(mockContext, "Fuel")
        val otherFolder = FileUtils.getCategoryFolder(mockContext, "Food")
        
        // Assert
        // Both folders should be under the same parent Receipts directory
        assertEquals("Both folders should have same parent", 
            fuelFolder.parent, otherFolder.parent)
        
        // Parent directory should be named "Receipts"
        assertTrue("Parent should be 'Receipts'", 
            fuelFolder.parentFile?.name == "Receipts")
    }

    @Test
    fun `generateFileName should include category in the generated name`() {
        // Arrange
        val date = System.currentTimeMillis()
        val merchant = "Test Merchant"
        val category = "Fuel"
        val total = 45.67
        val extension = "jpg"
        val description = "Test receipt"
        
        // Act
        val fileName = FileUtils.generateFileName(
            date = date,
            merchant = merchant,
            category = category,
            total = total,
            extension = extension,
            description = description
        )
        
        // Assert
        assertNotNull("Filename should not be null", fileName)
        assertTrue("Filename should end with extension", fileName.endsWith(".$extension"))
        assertTrue("Filename should contain description", fileName.contains("Test receipt"))
        assertTrue("Filename should contain total", fileName.contains("45.67"))
    }

    @Test
    fun `generateFileName should handle null merchant gracefully`() {
        // Arrange
        val date = System.currentTimeMillis()
        val category = "Fuel"
        val total = 45.67
        val extension = "jpg"
        
        // Act
        val fileName = FileUtils.generateFileName(
            date = date,
            merchant = null,
            category = category,
            total = total,
            extension = extension
        )
        
        // Assert
        assertNotNull("Filename should not be null even with null merchant", fileName)
        assertTrue("Filename should end with extension", fileName.endsWith(".$extension"))
    }

    @Test
    fun `generateFileName should handle null description gracefully`() {
        // Arrange
        val date = System.currentTimeMillis()
        val merchant = "Test Merchant"
        val category = "Fuel"
        val total = 45.67
        val extension = "jpg"
        
        // Act
        val fileName = FileUtils.generateFileName(
            date = date,
            merchant = merchant,
            category = category,
            total = total,
            extension = extension,
            description = null
        )
        
        // Assert
        assertNotNull("Filename should not be null even with null description", fileName)
        assertTrue("Filename should end with extension", fileName.endsWith(".$extension"))
        assertTrue("Filename should use merchant when description is null", 
            fileName.contains("Test Merchant"))
    }

    @Test
    fun `getFileExtension should return correct extension for jpeg`() {
        // Arrange
        val mockUri = mock(Uri::class.java)
        val mockContentResolver = mock(android.content.ContentResolver::class.java)
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.getType(mockUri)).thenReturn("image/jpeg")
        
        // Act
        val extension = FileUtils.getFileExtension(mockUri, mockContext)
        
        // Assert
        assertEquals("Extension should be jpg for image/jpeg", "jpg", extension)
    }

    @Test
    fun `getFileExtension should return correct extension for png`() {
        // Arrange
        val mockUri = mock(Uri::class.java)
        val mockContentResolver = mock(android.content.ContentResolver::class.java)
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.getType(mockUri)).thenReturn("image/png")
        
        // Act
        val extension = FileUtils.getFileExtension(mockUri, mockContext)
        
        // Assert
        assertEquals("Extension should be png for image/png", "png", extension)
    }

    @Test
    fun `getFileExtension should return correct extension for pdf`() {
        // Arrange
        val mockUri = mock(Uri::class.java)
        val mockContentResolver = mock(android.content.ContentResolver::class.java)
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.getType(mockUri)).thenReturn("application/pdf")
        
        // Act
        val extension = FileUtils.getFileExtension(mockUri, mockContext)
        
        // Assert
        assertEquals("Extension should be pdf for application/pdf", "pdf", extension)
    }
}
