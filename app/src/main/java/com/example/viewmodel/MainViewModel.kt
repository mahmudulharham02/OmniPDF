package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.PDFManager
import com.example.util.StorageManager
import com.example.util.TranslationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("OmniPDF_Prefs", Context.MODE_PRIVATE)

    // Global Settings State
    private val _activeLang = MutableStateFlow(prefs.getString("activeLang", "en") ?: "en")
    val activeLang: StateFlow<String> = _activeLang.asStateFlow()

    private val _isPitchDark = MutableStateFlow(prefs.getBoolean("isPitchDark", true)) // Pitch Dark by default as OLED dark is elegant
    val isPitchDark: StateFlow<Boolean> = _isPitchDark.asStateFlow()

    private val _useMonospace = MutableStateFlow(prefs.getBoolean("useMonospace", false))
    val useMonospace: StateFlow<Boolean> = _useMonospace.asStateFlow()

    // Navigation state
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Storage permissions state
    private val _hasStoragePermission = MutableStateFlow(StorageManager.hasStoragePermissions(application))
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    // File Repository State
    private val _availableFiles = MutableStateFlow<List<File>>(emptyList())
    val availableFiles: StateFlow<List<File>> = _availableFiles.asStateFlow()

    private val _selectedFile = MutableStateFlow<File?>(null)
    val selectedFile: StateFlow<File?> = _selectedFile.asStateFlow()

    // Status & Progress Message States
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow<Boolean?>(null)
    val isSuccess: StateFlow<Boolean?> = _isSuccess.asStateFlow()

    // -----------------------------------------------------
    // MODULE SPECIFIC STATES
    // -----------------------------------------------------

    // 1. Scanner State
    private val _capturedPages = MutableStateFlow<List<Bitmap>>(emptyList())
    val capturedPages: StateFlow<List<Bitmap>> = _capturedPages.asStateFlow()

    // 2. Viewer State
    private val _viewerPageIndex = MutableStateFlow(0)
    val viewerPageIndex: StateFlow<Int> = _viewerPageIndex.asStateFlow()

    private val _viewerPageCount = MutableStateFlow(0)
    val viewerPageCount: StateFlow<Int> = _viewerPageCount.asStateFlow()

    private val _viewerBitmap = MutableStateFlow<Bitmap?>(null)
    val viewerBitmap: StateFlow<Bitmap?> = _viewerBitmap.asStateFlow()

    // 3. Split PDF State
    private val _splitRange = MutableStateFlow("1-2")
    val splitRange: StateFlow<String> = _splitRange.asStateFlow()

    private val _splitOutputName = MutableStateFlow("Split_Segment_1")
    val splitOutputName: StateFlow<String> = _splitOutputName.asStateFlow()

    // 4. Merge PDF State
    private val _mergeQueue = MutableStateFlow<List<File>>(emptyList())
    val mergeQueue: StateFlow<List<File>> = _mergeQueue.asStateFlow()

    private val _mergeOutputName = MutableStateFlow("Compiled_Document")
    val mergeOutputName: StateFlow<String> = _mergeOutputName.asStateFlow()

    // 5. Sign PDF State
    private val _signScale = MutableStateFlow(0.4f)
    val signScale: StateFlow<Float> = _signScale.asStateFlow()

    private val _signXPercent = MutableStateFlow(0.5f)
    val signXPercent: StateFlow<Float> = _signXPercent.asStateFlow()

    private val _signYPercent = MutableStateFlow(0.75f)
    val signYPercent: StateFlow<Float> = _signYPercent.asStateFlow()

    private val _signPageIndex = MutableStateFlow(0)
    val signPageIndex: StateFlow<Int> = _signPageIndex.asStateFlow()

    // 6. Encrypt PDF State
    private val _encryptPassword = MutableStateFlow("")
    val encryptPassword: StateFlow<String> = _encryptPassword.asStateFlow()

    private val _encryptOutputName = MutableStateFlow("Secured_Doc")
    val encryptOutputName: StateFlow<String> = _encryptOutputName.asStateFlow()

    // 7. Watermark PDF State
    private val _watermarkText = MutableStateFlow("CONFIDENTIAL")
    val watermarkText: StateFlow<String> = _watermarkText.asStateFlow()

    private val _watermarkOpacity = MutableStateFlow(0.35f)
    val watermarkOpacity: StateFlow<Float> = _watermarkOpacity.asStateFlow()

    private val _watermarkRotation = MutableStateFlow(45f)
    val watermarkRotation: StateFlow<Float> = _watermarkRotation.asStateFlow()

    private val _watermarkOutputName = MutableStateFlow("Watermarked_Doc")
    val watermarkOutputName: StateFlow<String> = _watermarkOutputName.asStateFlow()

    // 8. PDF to Image / Asset Ripper exported lists
    private val _exportedImagePaths = MutableStateFlow<List<String>>(emptyList())
    val exportedImagePaths: StateFlow<List<String>> = _exportedImagePaths.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<File>>(emptyList())
    val recentFiles: StateFlow<List<File>> = _recentFiles.asStateFlow()

    init {
        loadRecentFiles()
        // Initialize with default template guide so user has instant document access
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sampleFile = PDFManager.createSamplePdf(getApplication())
                addToRecentFiles(sampleFile)
                _selectedFile.value = sampleFile
                refreshFileList()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to create sample PDF", e)
            }
        }
    }

    fun loadRecentFiles() {
        val savedString = prefs.getString("recentFiles", "") ?: ""
        if (savedString.isNotEmpty()) {
            val paths = savedString.split("|")
            val files = paths.map { File(it) }.filter { it.exists() }
            _recentFiles.value = files
        }
    }

    fun addToRecentFiles(file: File) {
        val current = _recentFiles.value.toMutableList()
        current.removeAll { it.absolutePath == file.absolutePath }
        current.add(0, file)
        val trimmed = current.take(10)
        _recentFiles.value = trimmed
        
        val pathsString = trimmed.joinToString("|") { it.absolutePath }
        prefs.edit().putString("recentFiles", pathsString).apply()
    }

    fun selectUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            try {
                val contentResolver = context.contentResolver
                var fileName = "Imported_Document_${System.currentTimeMillis()}.pdf"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrEmpty()) {
                            fileName = name
                        }
                    }
                }
                
                if (!fileName.endsWith(".pdf", ignoreCase = true)) {
                    fileName += ".pdf"
                }
                
                val destFile = File(PDFManager.getOmniPdfDirectory(context), fileName)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                if (destFile.exists()) {
                    addToRecentFiles(destFile)
                    _selectedFile.value = destFile
                    loadSelectedFileRendererData()
                    refreshFileList()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing URI", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // -----------------------------------------------------
    // GLOBAL ACTIONS
    // -----------------------------------------------------

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        _statusMessage.value = ""
        _isSuccess.value = null
        // Reset navigation or screen local sub-states if necessary
        if (screen == "viewer" || screen == "split" || screen == "sign" || screen == "watermark") {
            loadSelectedFileRendererData()
        }
    }

    fun setLanguage(lang: String) {
        _activeLang.value = lang
        prefs.edit().putString("activeLang", lang).apply()
    }

    fun setPitchDark(isDark: Boolean) {
        _isPitchDark.value = isDark
        prefs.edit().putBoolean("isPitchDark", isDark).apply()
    }

    fun setUseMonospace(useMono: Boolean) {
        _useMonospace.value = useMono
        prefs.edit().putBoolean("useMonospace", useMono).apply()
    }

    fun updatePermissionStatus() {
        val granted = StorageManager.hasStoragePermissions(getApplication())
        _hasStoragePermission.value = granted
        if (granted) {
            refreshFileList()
        }
    }

    fun refreshFileList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = PDFManager.getOmniPdfDirectory(getApplication())
                val files = dir.listFiles { file -> file.isFile && file.name.endsWith(".pdf") }?.toList() ?: emptyList()
                _availableFiles.value = files.sortedByDescending { it.lastModified() }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching file list", e)
            }
        }
    }

    fun selectFile(file: File) {
        addToRecentFiles(file)
        _selectedFile.value = file
        loadSelectedFileRendererData()
    }

    fun clearSelectedFile() {
        _selectedFile.value = null
    }

    fun deleteFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) {
                file.delete()
                if (_selectedFile.value?.absolutePath == file.absolutePath) {
                    _selectedFile.value = null
                }
                refreshFileList()
            }
        }
    }

    private fun loadSelectedFileRendererData() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val count = PDFManager.getPageCount(file)
            _viewerPageCount.value = count
            _viewerPageIndex.value = 0
            if (count > 0) {
                loadViewerBitmap(0)
            } else {
                _viewerBitmap.value = null
            }
        }
    }

    fun setViewerPageIndex(index: Int) {
        val maxPages = _viewerPageCount.value
        if (index in 0 until maxPages) {
            _viewerPageIndex.value = index
            loadViewerBitmap(index)
        }
    }

    private fun loadViewerBitmap(index: Int) {
        val file = _selectedFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = PDFManager.renderPdfPage(getApplication(), file, index)
            _viewerBitmap.value = bitmap
        }
    }

    // Translate helper shortcut
    fun t(key: String): String {
        return TranslationHelper.get(key, _activeLang.value)
    }

    // -----------------------------------------------------
    // PDF UTILITIES ACTION LOGICS
    // -----------------------------------------------------

    // 1. Scanner Actions
    fun snapScannerPage(bitmap: Bitmap) {
        val currentList = _capturedPages.value.toMutableList()
        currentList.add(bitmap)
        _capturedPages.value = currentList
    }

    fun clearScannerPages() {
        _capturedPages.value = emptyList()
    }

    fun compileScanner(outputName: String) {
        if (_capturedPages.value.isEmpty()) {
            _statusMessage.value = t("tool_scanner_empty")
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val compiledFile = PDFManager.compileScannedPages(
                    getApplication(),
                    _capturedPages.value,
                    outputName
                )
                _selectedFile.value = compiledFile
                refreshFileList()
                _isSuccess.value = true
                _statusMessage.value = "${t("status_success")}\n-> ${compiledFile.name}"
                clearScannerPages()
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // 3. Split PDF Actions
    fun updateSplitRange(range: String) {
        _splitRange.value = range
    }

    fun updateSplitOutputName(name: String) {
        _splitOutputName.value = name
    }

    fun executeSplit() {
        val file = _selectedFile.value
        if (file == null) {
            _statusMessage.value = t("tool_viewer_no_file")
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val splitFile = PDFManager.splitPdf(
                    getApplication(),
                    file,
                    _splitRange.value,
                    _splitOutputName.value
                )
                _selectedFile.value = splitFile
                refreshFileList()
                _isSuccess.value = true
                _statusMessage.value = "${t("status_success")}\n-> ${splitFile.name}"
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // 4. Merge PDF Actions
    fun addToMergeQueue(file: File) {
        val currentQueue = _mergeQueue.value.toMutableList()
        if (!currentQueue.any { it.absolutePath == file.absolutePath }) {
            currentQueue.add(file)
            _mergeQueue.value = currentQueue
        }
    }

    fun removeFromMergeQueue(file: File) {
        val currentQueue = _mergeQueue.value.toMutableList()
        currentQueue.removeAll { it.absolutePath == file.absolutePath }
        _mergeQueue.value = currentQueue
    }

    fun moveMergeQueueUp(index: Int) {
        if (index <= 0 || index >= _mergeQueue.value.size) return
        val currentQueue = _mergeQueue.value.toMutableList()
        val temp = currentQueue[index]
        currentQueue[index] = currentQueue[index - 1]
        currentQueue[index - 1] = temp
        _mergeQueue.value = currentQueue
    }

    fun moveMergeQueueDown(index: Int) {
        if (index < 0 || index >= _mergeQueue.value.size - 1) return
        val currentQueue = _mergeQueue.value.toMutableList()
        val temp = currentQueue[index]
        currentQueue[index] = currentQueue[index + 1]
        currentQueue[index + 1] = temp
        _mergeQueue.value = currentQueue
    }

    fun updateMergeOutputName(name: String) {
        _mergeOutputName.value = name
    }

    fun executeMerge() {
        if (_mergeQueue.value.size < 2) {
            _statusMessage.value = "Please select at least 2 files to merge."
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val mergedFile = PDFManager.mergePdfs(
                    getApplication(),
                    _mergeQueue.value,
                    _mergeOutputName.value
                )
                _selectedFile.value = mergedFile
                refreshFileList()
                _isSuccess.value = true
                _statusMessage.value = "${t("status_success")}\n-> ${mergedFile.name}"
                _mergeQueue.value = emptyList()
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // 5. Sign PDF Actions
    fun updateSignPlacement(x: Float, y: Float) {
        _signXPercent.value = x.coerceIn(0.05f, 0.95f)
        _signYPercent.value = y.coerceIn(0.05f, 0.95f)
    }

    fun updateSignScale(scale: Float) {
        _signScale.value = scale.coerceIn(0.1f, 1.5f)
    }

    fun updateSignPageIndex(idx: Int) {
        _signPageIndex.value = idx.coerceIn(0, Math.max(0, _viewerPageCount.value - 1))
    }

    fun executeSign(signature: Bitmap) {
        val file = _selectedFile.value
        if (file == null) {
            _statusMessage.value = t("tool_viewer_no_file")
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val signedFile = PDFManager.signPdf(
                    getApplication(),
                    file,
                    signature,
                    _signPageIndex.value,
                    _signXPercent.value,
                    _signYPercent.value,
                    _signScale.value,
                    "${file.nameWithoutExtension}_Signed"
                )
                _selectedFile.value = signedFile
                refreshFileList()
                _isSuccess.value = true
                _statusMessage.value = "${t("status_success")}\n-> ${signedFile.name}"
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // 6. Encrypt PDF Actions
    fun updateEncryptPassword(p: String) {
        _encryptPassword.value = p
    }

    fun updateEncryptOutputName(name: String) {
        _encryptOutputName.value = name
    }

    fun executeEncrypt() {
        val file = _selectedFile.value
        if (file == null) {
            _statusMessage.value = t("tool_viewer_no_file")
            _isSuccess.value = false
            return
        }
        if (_encryptPassword.value.isEmpty()) {
            _statusMessage.value = "Please enter a valid lock password."
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val encryptedFile = PDFManager.encryptPdf(
                    getApplication(),
                    file,
                    _encryptPassword.value,
                    _encryptOutputName.value
                )
                refreshFileList()
                _isSuccess.value = true
                _statusMessage.value = "${t("tool_encrypt_secured")}\n-> ${encryptedFile.name}"
                _encryptPassword.value = ""
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // 7. Watermark PDF Actions
    fun updateWatermarkText(text: String) {
        _watermarkText.value = text
    }

    fun updateWatermarkOpacity(opacity: Float) {
        _watermarkOpacity.value = opacity.coerceIn(0f, 1f)
    }

    fun updateWatermarkRotation(rotation: Float) {
        _watermarkRotation.value = rotation.coerceIn(0f, 360f)
    }

    fun updateWatermarkOutputName(name: String) {
        _watermarkOutputName.value = name
    }

    fun executeWatermark() {
        val file = _selectedFile.value
        if (file == null) {
            _statusMessage.value = t("tool_viewer_no_file")
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val watermarkedFile = PDFManager.watermarkPdf(
                    getApplication(),
                    file,
                    _watermarkText.value,
                    _watermarkOpacity.value,
                    _watermarkRotation.value,
                    _watermarkOutputName.value
                )
                _selectedFile.value = watermarkedFile
                refreshFileList()
                _isSuccess.value = true
                _statusMessage.value = "${t("status_success")}\n-> ${watermarkedFile.name}"
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // 8. PDF to Image Actions
    fun executePdfToImage() {
        val file = _selectedFile.value
        if (file == null) {
            _statusMessage.value = t("tool_viewer_no_file")
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val exported = PDFManager.convertPdfToImages(getApplication(), file)
                _isSuccess.value = true
                _exportedImagePaths.value = exported.map { it.absolutePath }
                _statusMessage.value = "${t("status_success")}\nExported ${exported.size} pages to Pictures/OmniPDF_Images/"
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // 9. Extract Images Actions
    fun executeExtractImages() {
        val file = _selectedFile.value
        if (file == null) {
            _statusMessage.value = t("tool_viewer_no_file")
            _isSuccess.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _statusMessage.value = t("status_processing")
            _isSuccess.value = null
            try {
                val extracted = PDFManager.extractImagesFromPdf(getApplication(), file)
                _isSuccess.value = true
                _exportedImagePaths.value = extracted.map { it.absolutePath }
                _statusMessage.value = "${t("status_success")}\nRipped ${extracted.size} illustrations to Pictures/OmniPDF_Extracted/"
            } catch (e: Exception) {
                _isSuccess.value = false
                _statusMessage.value = "${t("status_error")} ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Secure Decrypter Check & Decrypt flow
    fun attemptDecryptAndOpen(encryptedFile: File, pass: String, onSuccess: (File) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(getApplication<Application>().cacheDir, "decrypted_temp_${System.currentTimeMillis()}.pdf")
            val success = PDFManager.decryptPdf(getApplication(), encryptedFile, pass, tempFile)
            if (success && tempFile.exists() && tempFile.length() > 0L) {
                _selectedFile.value = tempFile
                loadSelectedFileRendererData()
                Handler(Looper.getMainLooper()).post {
                    onSuccess(tempFile)
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    onFailure()
                }
            }
        }
    }
}
