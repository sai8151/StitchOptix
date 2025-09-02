package com.saidev.stitchoptix

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var selectFileButton: MaterialButton
    private lateinit var processButton: MaterialButton
    private lateinit var selectedFileText: android.widget.TextView
    private lateinit var statusText: android.widget.TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var originalPreviewCard: MaterialCardView
    private lateinit var originalPreviewImageView: ImageView
    private lateinit var fileMetadataText: TextView

    // File handling
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    // Python
    private lateinit var python: Python

    // Save handling
    private var pendingTempOutputPath: String? = null
    private var lastResultText: String? = null

    // Launchers for file operations
    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            handleSelectedFile(uri)
        }
    }

    private val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri ->
        if (destUri != null && pendingTempOutputPath != null) {
            saveFileToUserLocation(pendingTempOutputPath!!, destUri)
        } else {
            Toast.makeText(this, "Save cancelled", Toast.LENGTH_SHORT).show()
        }
        pendingTempOutputPath = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)

        // Initialize Python
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        python = Python.getInstance()

        initializeViews()
        setupEventListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_support -> {
                startActivity(Intent(this, SupportActivity::class.java))
                true
            }
            R.id.action_faq -> {
                startActivity(Intent(this, FAQActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeViews() {
        selectFileButton = findViewById(R.id.selectFileButton)
        processButton = findViewById(R.id.processButton)
        selectedFileText = findViewById(R.id.selectedFileText)
        val previewCard = findViewById<MaterialCardView>(R.id.previewCard)
        previewCard.visibility = View.VISIBLE
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        // Original preview components
        originalPreviewCard = findViewById(R.id.originalPreviewCard)
        originalPreviewImageView = findViewById(R.id.originalPreviewImageView)
        fileMetadataText = findViewById(R.id.fileMetadataText)
    }

    @SuppressLint("SetTextI18n")
    private fun setupEventListeners() {
        selectFileButton.setOnClickListener { openFilePicker() }
        processButton.setOnClickListener { processFile() }
    }

    private fun openFilePicker() {
        openDocLauncher.launch(arrayOf("application/octet-stream", "*/*"))
    }

    private fun handleSelectedFile(fileUri: Uri) {
        try {
            selectedFileName = queryDisplayName(fileUri)
            selectedFileName?.let { fileName ->
                if (isValidEmbroideryFile(fileName)) {
                    selectedFileText.text = "Selected: $fileName"
                    processButton.isEnabled = true
                    statusText.text = "Generating preview..."
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.primary_color))

                    // Generate preview and metadata
                    lifecycleScope.launch {
                        generateOriginalPreview(fileUri)
                    }

                } else {
                    selectedFileText.text = "Invalid file format"
                    processButton.isEnabled = false
                    statusText.text = "Please select a valid embroidery file"
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.error_color))
                    originalPreviewCard.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error selecting file : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private suspend fun generateOriginalPreview(fileUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // Copy file to internal storage for preview
                val previewInputName = "preview_${selectedFileName ?: "file.dst"}"
                val previewInputPath = copyFileToInternalStorage(fileUri, previewInputName)

                if (previewInputPath != null) {
                    // Call Python preview function
                    val pythonModule = python.getModule("stitch_reducer")
                    val pyResult: PyObject = pythonModule.callAttr("preview_dst", previewInputPath)

                    val resultMap: Map<*, *>? = try {
                        pyResult.toJava(Map::class.java) as? Map<*, *>
                    } catch (_: Exception) { null }

                    val parsed: Map<String, Any?>? = if (resultMap != null) {
                        @Suppress("UNCHECKED_CAST")
                        resultMap as Map<String, Any?>
                    } else {
                        try {
                            val py = Python.getInstance()
                            val jsonModule = py.getModule("json")
                            val jsonStr = jsonModule.callAttr("dumps", pyResult).toString()
                            val jsonObj = org.json.JSONObject(jsonStr)
                            jsonObj.keys().asSequence().associateWith { k -> jsonObj.opt(k) }
                        } catch (e: Exception) { null }
                    }

                    if (parsed != null && parsed["status"] == "success") {
                        val stitchCount = (parsed["stitch_count"] as? Number)?.toInt() ?: 0
                        val fileSize = (parsed["file_size"] as? Number)?.toLong() ?: 0L
                        val filePath = parsed["file_path"]?.toString() ?: ""
                        val bounds = parsed["bounds"]?.toString() ?: "Unknown"
                        val pngPath = parsed["png_path"]?.toString()

                        withContext(Dispatchers.Main) {
                            // Display metadata
                            val metadata = buildString {
                                append("Stitches: ${formatNumber(stitchCount)}\n")
                                append("File Size: ${formatFileSize(fileSize)}\n")
                                append("Dimensions: $bounds\n")
                                append("Path: ${File(filePath).name}")
                            }
                            fileMetadataText.text = metadata

                            // Display preview image
                            if (pngPath != null) {
                                displayOriginalPreview(pngPath)
                            }

                            // Show the preview card
                            originalPreviewCard.visibility = View.VISIBLE

                            statusText.text = "Ready to process: ${selectedFileName}"
                            statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary_color))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            statusText.text = "Ready to process: ${selectedFileName} (Preview failed)"
                            statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary_color))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Ready to process: ${selectedFileName} (Preview error)"
                    statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary_color))
                }
            }
        }
    }
    private fun displayOriginalPreview(pngPath: String) {
        try {
            val pngFile = File(pngPath)
            if (pngFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(pngPath)
                if (bitmap != null) {
                    originalPreviewImageView.setImageBitmap(bitmap)
                    originalPreviewImageView.visibility = View.VISIBLE
                }
            }
        } catch (_: Exception) {}
    }

    // Add helper methods for formatting
    private fun formatNumber(number: Int): String {
        return String.format("%,d", number)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return null
    }

    private fun isValidEmbroideryFile(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.getDefault())
        return lower.endsWith(".dst") || lower.endsWith(".jef") ||
                lower.endsWith(".pes") || lower.endsWith(".exp") ||
                lower.endsWith(".vp3") || lower.endsWith(".u01") ||
                lower.endsWith(".pec") || lower.endsWith(".xxx")
    }

    private fun processFile() {
        val uri = selectedFileUri ?: run {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setTitle("Processing Embroidery File")
            setMessage("Optimizing stitches...")
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setCancelable(false)
        }

        lifecycleScope.launch {
            try {
                progressDialog.show()
                processButton.isEnabled = false
                statusText.text = "Processing embroidery file..."
                statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary_color))
                progressBar.visibility = View.VISIBLE

                val result = withContext(Dispatchers.IO) {
                    processFileInBackground(uri, progressDialog)
                }

                processButton.isEnabled = true
                progressBar.visibility = View.GONE

                if (result.startsWith("Success!")) {
                    val shown = result.substringBefore("\n||TEMP_PATH||=")
                    statusText.text = shown
                    statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success_color))
                    Toast.makeText(this@MainActivity, "Processing complete! Choose where to save.", Toast.LENGTH_LONG).show()

                    // Extract temp path and immediately prompt user to save
                    val tempPath = result.substringAfter("||TEMP_PATH||=").substringBefore("\n||PNG_PATH||=")
                    if (tempPath.isNotEmpty()) {
                        val suggestedName = buildSuggestedName(selectedFileName)
                        pendingTempOutputPath = tempPath
                        createDocLauncher.launch(suggestedName)
                    }
                } else {
                    statusText.text = result
                    statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.error_color))
                    Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                processButton.isEnabled = true
                progressBar.visibility = View.GONE
                val msg = "Error: ${e.message}"
                statusText.text = msg
                statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.error_color))
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            } finally {
                if (progressDialog.isShowing) progressDialog.dismiss()
            }
        }
    }

    private suspend fun processFileInBackground(fileUri: Uri, progressDialog: ProgressDialog): String {
        return try {
            withContext(Dispatchers.Main) {
                progressDialog.setMessage("Reading input file...")
            }

            val inputName = "input_${selectedFileName ?: "file.dst"}"
            val inputPath = copyFileToInternalStorage(fileUri, inputName)
                ?: return "Error: Could not copy input file"

            withContext(Dispatchers.Main) {
                progressDialog.setMessage("Processing with Python...")
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFileName = "optimized_${timestamp}_${selectedFileName ?: "file.dst"}"

            val pythonModule = python.getModule("stitch_reducer")
            val pyResult: PyObject = try {
                pythonModule.callAttr("optimize_pattern", inputPath, outputFileName)
            } catch (pe: PyException) {
                return "Python error:\n$pe"
            } catch (e: Exception) {
                return "Error calling Python: ${e.message}"
            }

            val resultMap: Map<*, *>? = try {
                pyResult.toJava(Map::class.java) as? Map<*, *>
            } catch (_: Exception) { null }

            val parsed: Map<String, Any?>? = if (resultMap != null) {
                @Suppress("UNCHECKED_CAST")
                resultMap as Map<String, Any?>
            } else {
                try {
                    val py = Python.getInstance()
                    val jsonModule = py.getModule("json")
                    val jsonStr = jsonModule.callAttr("dumps", pyResult).toString()
                    val jsonObj = org.json.JSONObject(jsonStr)
                    jsonObj.keys().asSequence().associateWith { k -> jsonObj.opt(k) }
                } catch (e: Exception) { null }
            }

            if (parsed == null) return "Unexpected Python response:\n$pyResult"

            val status = parsed["status"]?.toString()?.lowercase(Locale.getDefault()) ?: "error"
            val message = parsed["message"]?.toString() ?: "No message"

            if (status == "success") {
                val originalCount = (parsed["original_count"] as? Number)?.toInt() ?: 0
                val newCount = (parsed["new_count"] as? Number)?.toInt() ?: 0
                val outputPathFromPy = parsed["output_path"]?.toString()
                val pngPath = parsed["png_path"]?.toString()

                if (outputPathFromPy != null) {
                    val outputFile = File(outputPathFromPy)

                    if (!outputFile.exists()) {
                        return "Error: Output file not found: $outputPathFromPy"
                    }

                    if (outputFile.length() == 0L) {
                        return "Error: Output file is empty"
                    }

                    withContext(Dispatchers.Main) {
                        progressDialog.setMessage("Preparing to save...")
                    }

                    if (pngPath != null) displayPngPreview(pngPath)

                    val reductionPercent = if (originalCount > 0) {
                        (originalCount - newCount) * 100.0 / originalCount
                    } else 0.0

                    val resultText = "Success!\n" +
                            "Original stitches: $originalCount\n" +
                            "Optimized stitches: $newCount\n" +
                            "Reduction: ${"%.1f".format(reductionPercent)}%"
                    lastResultText = resultText

                    return "$resultText\n||TEMP_PATH||=$outputPathFromPy\n||PNG_PATH||=$pngPath"
                } else {
                    return "Error: No output path returned"
                }
            } else {
                return "Error: $message"
            }
        } catch (e: Exception) {
            "Processing error: ${e.message ?: "Unknown error"}"
        }
    }

    private fun displayPngPreview(pngPath: String) {
        try {
            val pngFile = File(pngPath)
            if (pngFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(pngPath)
                if (bitmap != null) {
                    runOnUiThread {
                        val previewImageView = findViewById<ImageView>(R.id.previewImageView)
                        previewImageView.setImageBitmap(bitmap)
                        previewImageView.visibility = View.VISIBLE
                        Toast.makeText(this, "Preview loaded", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveFileToUserLocation(srcPath: String, destUri: Uri) {
        val src = File(srcPath)

        if (!src.exists()) {
            Toast.makeText(this, "Source file not found", Toast.LENGTH_LONG).show()
            return
        }

        try {
            var total = 0L
            contentResolver.openOutputStream(destUri, "w")?.use { out ->
                FileInputStream(src).use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n == -1) break
                        out.write(buf, 0, n)
                        total += n
                    }
                    out.flush()
                }
            }

            if (total > 0L) {
                Toast.makeText(this, "File saved successfully to your chosen location!", Toast.LENGTH_LONG).show()
                statusText.text = buildString {
                    append(lastResultText ?: "")
                    append("\nSaved to your chosen location (${total} bytes)")
                }
                statusText.setTextColor(ContextCompat.getColor(this, R.color.success_color))
            } else {
                Toast.makeText(this, "Save failed - 0 bytes written", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun copyFileToInternalStorage(fileUri: Uri, fileName: String): String? {
        return try {
            val internalFile = File(filesDir, fileName)

            if (internalFile.exists()) internalFile.delete()

            var totalBytes = 0L
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(internalFile).use { outputStream ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val bytes = inputStream.read(buffer)
                        if (bytes == -1) break
                        outputStream.write(buffer, 0, bytes)
                        totalBytes += bytes
                    }
                    outputStream.flush()
                }
            }

            if (!internalFile.exists() || internalFile.length() == 0L) {
                return null
            }

            internalFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun buildSuggestedName(sourceName: String?): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        if (sourceName.isNullOrBlank()) return "optimized_$timestamp.dst"
        val dot = sourceName.lastIndexOf('.')
        val base = if (dot >= 0) sourceName.substring(0, dot) else sourceName
        val ext = if (dot >= 0) sourceName.substring(dot) else ".dst"
        return "${base}_optimized_$timestamp$ext"
    }
}
