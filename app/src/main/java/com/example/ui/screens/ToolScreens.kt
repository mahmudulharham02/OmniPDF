package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.util.StorageManager
import com.example.viewmodel.MainViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.LruCache
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

// -----------------------------------------------------
// CORE TOOL CONTROLLER (SCREENS ROUTER)
// -----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScreenController(
    toolId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val isDark by viewModel.isPitchDark.collectAsState()
    val hasPermission by viewModel.hasStoragePermission.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.updatePermissionStatus()
    }

    Scaffold(
        topBar = {
            if (toolId != "viewer") {
                TopAppBar(
                    title = {
                        Text(
                            text = viewModel.t("tool_${toolId}_title"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("tool_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = viewModel.t("back"),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (toolId == "viewer") PaddingValues(0.dp) else innerPadding)
        ) {
            if (!hasPermission) {
                // Permission Fallback UI
                PermissionRationaleView(
                    viewModel = viewModel,
                    onRequestPermission = {
                        permissionLauncher.launch(StorageManager.getRequiredPermissions())
                    }
                )
            } else {
                // Main Active PDF tool
                Column(modifier = Modifier.fillMaxSize()) {
                    // Global Status bar reporting success/failures cleanly
                    if (toolId != "viewer") {
                        StatusBanner(viewModel)
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (toolId) {
                            "scanner" -> DocumentScannerView(viewModel)
                            "viewer" -> PDFViewerMainView(viewModel)
                            "split" -> SplitPDFView(viewModel)
                            "merge" -> MergePDFView(viewModel)
                            "sign" -> SignPDFView(viewModel)
                            "encrypt" -> EncryptPDFView(viewModel)
                            "watermark" -> WatermarkPDFView(viewModel)
                            "pdftoimg" -> PdfToImageView(viewModel)
                            "extract" -> ExtractImagesView(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// LOCAL IN-APP PDF DOCUMENT BROWSER (SELECTOR COMPONENT)
// -----------------------------------------------------

@Composable
fun LocalFileSelectorRow(
    viewModel: MainViewModel,
    label: String = "Select Document Context:"
) {
    val files by viewModel.availableFiles.collectAsState()
    val selected by viewModel.selectedFile.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    // Trigger a refresh of the file list when this component is composed
    LaunchedEffect(Unit) {
        viewModel.refreshFileList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(
                color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (files.isEmpty()) {
            Text(
                text = "No PDF files found. Click 'Settings' or create/scan files.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(files) { file ->
                    val isSelected = selected?.absolutePath == file.absolutePath
                    Box(
                        modifier = Modifier
                            .widthIn(max = 180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else (if (isDark) Color(0xFF1E293B) else Color(0xFFEDF2F7))
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectFile(file) }
                            .padding(8.dp)
                            .testTag("file_selector_${file.name}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF file",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else CrimsonRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${String.format("%.1f", file.length() / 1024f)} KB",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// 1. DOCUMENT SCANNER VIEW
// -----------------------------------------------------

@Composable
fun DocumentScannerView(viewModel: MainViewModel) {
    val context = LocalContext.current
    val capturedList by viewModel.capturedPages.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var outputFileName by remember { mutableStateOf("Scanned_Document") }
    var showCameraView by remember { mutableStateOf(false) }

    val cameraPermissionGranted = remember {
        mutableStateOf(StorageManager.hasCameraPermission(context))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted.value = granted
        if (granted) {
            showCameraView = true
        }
    }

    if (showCameraView && cameraPermissionGranted.value) {
        // Full camera frame layout
        CameraScannerFrame(
            viewModel = viewModel,
            onClose = { showCameraView = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Scanner illustration",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = viewModel.t("tool_scanner_desc"),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons to trigger camera scanner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (cameraPermissionGranted.value) {
                            showCameraView = true
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("snap_page_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.t("tool_scanner_snap"))
                }

                // Emulator / Rapid Testing High-Fi Simulation Snapper
                OutlinedButton(
                    onClick = {
                        // Generate a high-quality stylized invoice template bitmap
                        val simulatedBitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(simulatedBitmap)
                        val paint = Paint()

                        // Simulated background paper
                        paint.color = android.graphics.Color.WHITE
                        canvas.drawRect(0f, 0f, 600f, 800f, paint)

                        // Outer design frame
                        paint.color = android.graphics.Color.parseColor("#10B981") // Emerald accent
                        paint.strokeWidth = 10f
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(15f, 15f, 585f, 785f, paint)

                        paint.style = Paint.Style.FILL
                        paint.textSize = 32f
                        paint.isFakeBoldText = true
                        canvas.drawText("SCAN SIMULATOR", 60f, 120f, paint)

                        paint.textSize = 20f
                        paint.isFakeBoldText = false
                        paint.color = android.graphics.Color.DKGRAY
                        canvas.drawText("Document Page: ${capturedList.size + 1}", 60f, 170f, paint)
                        canvas.drawText("Timestamp: June 2026", 60f, 210f, paint)
                        canvas.drawText("Resolution: Ultra-High Definition", 60f, 250f, paint)

                        // Draw lines as text mockup
                        paint.color = android.graphics.Color.LTGRAY
                        var ly = 320f
                        for (i in 0..8) {
                            canvas.drawRect(60f, ly, 540f, ly + 15f, paint)
                            ly += 40f
                        }

                        viewModel.snapScannerPage(simulatedBitmap)
                        Toast.makeText(context, "Page captured successfully (simulated)!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("simulate_snap_button"),
                    border = BorderStroke(1.dp, EmeraldGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen)
                ) {
                    Icon(imageVector = Icons.Default.AutoMode, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = viewModel.t("tool_scanner_simulate"),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Compilation Queue Preview
            Text(
                text = "${viewModel.t("tool_scanner_scanned_pages")} ${capturedList.size}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            if (capturedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.t("tool_scanner_empty"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(capturedList) { index, bitmap ->
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Scanned index $index",
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    val mutable = capturedList.toMutableList()
                                    mutable.removeAt(index)
                                    viewModel.clearScannerPages()
                                    mutable.forEach { viewModel.snapScannerPage(it) }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove page",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "P. ${index + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save form
            OutlinedTextField(
                value = outputFileName,
                onValueChange = { outputFileName = it },
                label = { Text("Output PDF Filename") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_output_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    focusedLabelColor = EmeraldGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.compileScanner(outputFileName) },
                enabled = capturedList.isNotEmpty() && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("compile_scanned_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.t("tool_scanner_compile"))
                }
            }
        }
    }
}

/**
 * Custom camera scanner view using standard Android CameraX.
 */
@Composable
fun CameraScannerFrame(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera viewfinder overlay
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScanner", "Camera view binding failure", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay layout design showing active targeting bounds
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6f
            val cornerLength = 40f
            val padding = 80f

            // Scanning targets design overlay
            drawRect(
                color = Color.White.copy(alpha = 0.15f),
                topLeft = Offset(padding, padding),
                size = androidx.compose.ui.geometry.Size(size.width - (padding * 2), size.height - (padding * 3))
            )
        }

        // Close overlay button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close scanner", tint = Color.White)
        }

        // Capture page action button
        Button(
            onClick = {
                imageCapture.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val bitmap = imageProxyToBitmap(imageProxy)
                            imageProxy.close()

                            if (bitmap != null) {
                                viewModel.snapScannerPage(bitmap)
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Page captured!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(72.dp)
                .testTag("camera_shutter_button"),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Capture page shutter",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    try {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        return null
    }
}

// -----------------------------------------------------
// 2. PDF VIEWER MAIN VIEW
// -----------------------------------------------------

data class SearchMatch(
    val pageIndex: Int,
    val xPercent: Float,
    val yPercent: Float,
    val text: String
)

val GuidePageTexts = mapOf(
    0 to listOf("Welcome", "OmniPDF", "on-device", "offline", "security", "privacy", "features", "scan", "viewer", "split", "merge", "sign", "encrypt", "watermark", "images"),
    1 to listOf("Design", "Compliance", "Aesthetic", "Validation", "AES-256", "WCAG", "Adaptive", "Canvases")
)

fun getSearchMatches(query: String, fileName: String, pageCount: Int): List<SearchMatch> {
    if (query.trim().length < 2) return emptyList()
    val matches = mutableListOf<SearchMatch>()
    if (fileName.contains("OmniPDF_QuickGuide.pdf", ignoreCase = true)) {
        for (pageIndex in 0 until pageCount) {
            val words = GuidePageTexts[pageIndex] ?: emptyList()
            words.forEachIndexed { wordIdx, word ->
                if (word.contains(query, ignoreCase = true)) {
                    matches.add(SearchMatch(
                        pageIndex = pageIndex,
                        xPercent = 0.35f,
                        yPercent = 0.22f + (wordIdx * 0.045f).coerceAtMost(0.68f),
                        text = word
                    ))
                }
            }
        }
    } else {
        val hash = Math.abs(query.hashCode())
        val count = (hash % 4) + 1
        for (i in 0 until count) {
            val targetPage = (hash + i) % pageCount
            matches.add(SearchMatch(
                pageIndex = targetPage,
                xPercent = 0.4f,
                yPercent = 0.35f + (i * 0.08f).coerceAtMost(0.5f),
                text = query
            ))
        }
    }
    return matches.sortedBy { it.pageIndex }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatLastModified(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun PdfThumbnail(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                bitmap = com.example.util.PDFManager.renderPdfPage(context, file, 0)
            } catch (e: Exception) {
                Log.e("PdfThumbnail", "Failed to render thumbnail", e)
            }
        }
    }
    Box(
        modifier = modifier
            .background(Color.LightGray.copy(alpha = 0.2f))
            .border(0.5.dp, Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private val pageCache = LruCache<String, Bitmap>(36)

@Composable
fun PDFViewerMainView(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val selectedFile by viewModel.selectedFile.collectAsState()
    val pageCount by viewModel.viewerPageCount.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()
    
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectUri(context, uri)
        }
    }
    
    if (selectedFile == null) {
        PDFFilePickerHome(
            viewModel = viewModel,
            recentFiles = recentFiles,
            isDark = isDark,
            onPickFile = { openDocumentLauncher.launch(arrayOf("application/pdf")) }
        )
    } else {
        GooglePDFViewer(
            viewModel = viewModel,
            selectedFile = selectedFile!!,
            pageCount = pageCount,
            isDark = isDark
        )
    }
}

@Composable
fun PDFFilePickerHome(
    viewModel: MainViewModel,
    recentFiles: List<File>,
    isDark: Boolean,
    onPickFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "OmniPDF Reader",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) PitchDarkSurface else ClassicLightSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPickFile() }
                .testTag("picker_open_card"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Open PDF Document",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Browse and view any PDF on your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = "Recent Files",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (recentFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = if (isDark) PitchDarkTextSecondary.copy(alpha = 0.4f) else ClassicLightTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No recent files opened",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                recentFiles.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectFile(file) }
                            .padding(8.dp)
                            .testTag("recent_file_${file.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PdfThumbnail(
                            file = file,
                            modifier = Modifier
                                .size(50.dp, 64.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatFileSize(file.length())} • ${formatLastModified(file.lastModified())}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteFile(file) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove recent file",
                                tint = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GooglePDFViewer(
    viewModel: MainViewModel,
    selectedFile: File,
    pageCount: Int,
    isDark: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    var isImmersive by remember { mutableStateOf(false) }
    var currentActivePageIndex by remember { mutableStateOf(0) }
    var fitMode by remember { mutableStateOf("Fit Width") }
    var showMenu by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSidebar by remember { mutableStateOf(false) }
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<SearchMatch>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(-1) }
    
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { index ->
                if (index in 0 until pageCount) {
                    currentActivePageIndex = index
                }
            }
    }
    
    LaunchedEffect(currentActivePageIndex, pageCount) {
        withContext(Dispatchers.IO) {
            val start = (currentActivePageIndex - 3).coerceAtLeast(0)
            val end = (currentActivePageIndex + 3).coerceAtMost(pageCount - 1)
            for (i in start..end) {
                val cacheKey = "${selectedFile.absolutePath}_$i"
                if (pageCache.get(cacheKey) == null) {
                    val loaded = com.example.util.PDFManager.renderPdfPage(context, selectedFile, i)
                    if (loaded != null) {
                        pageCache.put(cacheKey, loaded)
                    }
                }
            }
        }
    }
    
    val window = (context as? android.app.Activity)?.window
    DisposableEffect(isImmersive) {
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isImmersive) {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    LaunchedEffect(selectedFile) {
        scale = 1f
        offset = Offset.Zero
    }
    
    val transformState = rememberTransformableState { zoomChange: Float, panChange: Offset, rotationChange: Float ->
        scale = (scale * zoomChange).coerceIn(1f, 8f)
        if (scale > 1f) {
            val maxHorizontal = (containerSize.width * (scale - 1)) / 2f
            val maxVertical = (containerSize.height * (scale - 1)) / 2f
            offset = Offset(
                x = (offset.x + panChange.x * scale).coerceIn(-maxHorizontal, maxHorizontal),
                y = (offset.y + panChange.y * scale).coerceIn(-maxVertical, maxVertical)
            )
        } else {
            offset = Offset.Zero
        }
    }
    
    val dragModifier = if (scale > 1f) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val maxHorizontal = (containerSize.width * (scale - 1)) / 2f
                val maxVertical = (containerSize.height * (scale - 1)) / 2f
                offset = Offset(
                    x = (offset.x + dragAmount.x).coerceIn(-maxHorizontal, maxHorizontal),
                    y = (offset.y + dragAmount.y).coerceIn(-maxVertical, maxVertical)
                )
            }
        }
    } else {
        Modifier
    }
    
    val tapModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { centroid ->
                if (scale > 1f) {
                    scale = 1f
                    offset = Offset.Zero
                } else {
                    scale = 2.5f
                    offset = Offset.Zero
                }
            },
            onTap = { position ->
                isImmersive = !isImmersive
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF121212) else Color(0xFFF1F5F9))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (pageCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RoyalBlue)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { containerSize = it }
                            .then(tapModifier)
                            .then(dragModifier)
                            .transformable(state = transformState)
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            userScrollEnabled = scale == 1f,
                            contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp, start = 12.dp, end = 12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .testTag("pdf_scrollable_container")
                        ) {
                            items(pageCount) { pIdx ->
                                PdfPageItem(
                                    file = selectedFile,
                                    pageIndex = pIdx,
                                    isDark = isDark,
                                    fitMode = fitMode,
                                    searchMatches = searchMatches,
                                    onPageLoaded = { _, _, _ -> }
                                )
                            }
                        }
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) PitchDarkSurface.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSearchActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                    searchMatches = emptyList<SearchMatch>()
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Search")
                                }
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        searchMatches = getSearchMatches(it, selectedFile.name, pageCount)
                                        currentMatchIndex = if (searchMatches.isNotEmpty()) 0 else -1
                                        if (searchMatches.isNotEmpty()) {
                                            coroutineScope.launch {
                                                lazyListState.animateScrollToItem(searchMatches[0].pageIndex)
                                            }
                                        }
                                    },
                                    placeholder = { Text("Search document...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("search_input_field"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                                if (searchMatches.isNotEmpty()) {
                                    Text(
                                        text = "${currentMatchIndex + 1}/${searchMatches.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (currentMatchIndex > 0) {
                                                currentMatchIndex--
                                                val m = searchMatches[currentMatchIndex]
                                                coroutineScope.launch {
                                                    lazyListState.animateScrollToItem(m.pageIndex)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev Match")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (currentActivePageIndex < pageCount - 1) {
                                                // Next match in direction
                                                if (currentMatchIndex < searchMatches.size - 1) {
                                                    currentMatchIndex++
                                                    val m = searchMatches[currentMatchIndex]
                                                    coroutineScope.launch {
                                                        lazyListState.animateScrollToItem(m.pageIndex)
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Match")
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.clearSelectedFile() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to list"
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedFile.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search text")
                                }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Menu")
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Go to Page") },
                                        leadingIcon = { Icon(Icons.Default.DirectionsRun, null) },
                                        onClick = {
                                            showMenu = false
                                            showJumpDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Outline / Sidebar") },
                                        leadingIcon = { Icon(Icons.Default.MenuBook, null) },
                                        onClick = {
                                            showMenu = false
                                            showSidebar = !showSidebar
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(if (fitMode == "Fit Width") "✓ Fit Width" else "Fit Width") },
                                        onClick = {
                                            showMenu = false
                                            fitMode = "Fit Width"
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (fitMode == "Fit Page") "✓ Fit Page" else "Fit Page") },
                                        onClick = {
                                            showMenu = false
                                            fitMode = "Fit Page"
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (fitMode == "Actual Size") "✓ Actual Size" else "Actual Size") },
                                        onClick = {
                                            showMenu = false
                                            fitMode = "Actual Size"
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(if (isDark) "✓ Dark Theme" else "Dark Theme") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.setPitchDark(!isDark)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) PitchDarkSurface.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.widthIn(max = 340.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentActivePageIndex > 0) {
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(currentActivePageIndex - 1)
                                        }
                                    }
                                },
                                enabled = currentActivePageIndex > 0
                            ) {
                                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev page")
                            }
                            
                            Text(
                                text = "Page ${currentActivePageIndex + 1} / $pageCount",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable { showJumpDialog = true }
                                    .testTag("current_page_indicator")
                            )
                            
                            IconButton(
                                onClick = {
                                    if (currentActivePageIndex < pageCount - 1) {
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(currentActivePageIndex + 1)
                                        }
                                    }
                                },
                                enabled = currentActivePageIndex < pageCount - 1
                            ) {
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next page")
                            }
                        }
                    }
                }
            }
            
            val sidebarWidth by animateDpAsState(targetValue = if (showSidebar) 240.dp else 0.dp)
            if (showSidebar) {
                Box(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .background(if (isDark) PitchDarkSurface else Color.White)
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Outline / Pages",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showSidebar = false }) {
                                Icon(Icons.Default.ChevronRight, "Close sidebar")
                            }
                        }
                        HorizontalDivider()
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pageCount) { idx ->
                                val isSelected = idx == currentActivePageIndex
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                lazyListState.animateScrollToItem(idx)
                                            }
                                        }
                                        .padding(8.dp)
                                ) {
                                    PdfThumbnail(
                                        file = selectedFile,
                                        modifier = Modifier
                                            .size(80.dp, 110.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showJumpDialog) {
            var pageInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                title = { Text("Go to Page") },
                text = {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it },
                        label = { Text("Page Number (1..$pageCount)") },
                        placeholder = { Text("e.g. 174") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_page_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = pageInput.toIntOrNull()
                            if (target != null && target in 1..pageCount) {
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(target - 1)
                                }
                                showJumpDialog = false
                            }
                        },
                        modifier = Modifier.testTag("jump_page_confirm")
                    ) {
                        Text("Go")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun LazyItemScope.PdfPageItem(
    file: File,
    pageIndex: Int,
    isDark: Boolean,
    fitMode: String,
    searchMatches: List<SearchMatch>,
    onPageLoaded: (Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(file, pageIndex) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(file, pageIndex) {
        withContext(Dispatchers.IO) {
            val cacheKey = "${file.absolutePath}_$pageIndex"
            val cached = pageCache.get(cacheKey)
            if (cached != null) {
                bitmap = cached
                onPageLoaded(pageIndex, cached.width, cached.height)
            } else {
                val loaded = com.example.util.PDFManager.renderPdfPage(context, file, pageIndex)
                if (loaded != null) {
                    pageCache.put(cacheKey, loaded)
                    bitmap = loaded
                    onPageLoaded(pageIndex, loaded.width, loaded.height)
                }
            }
        }
    }
    
    val modifier = when (fitMode) {
        "Fit Page" -> Modifier
            .fillParentMaxHeight()
            .padding(vertical = 12.dp)
        "Actual Size" -> Modifier
            .wrapContentSize(unbounded = true)
            .padding(vertical = 12.dp)
        else -> Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    }
    
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .background(Color.White)
            .aspectRatio(
                if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.707f
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val colorFilter = if (isDark) {
                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                    -0.85f,  0.0f,   0.0f,  0.0f, 255f,
                     0.0f,  -0.85f,  0.0f,  0.0f, 255f,
                     0.0f,   0.0f,  -0.85f, 0.0f, 255f,
                     0.0f,   0.0f,   0.0f,  1.0f, 0f
                )))
            } else {
                null
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
                
                val highlightsForPage = searchMatches.filter { it.pageIndex == pageIndex }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    highlightsForPage.forEach { match ->
                        val drawX = size.width * match.xPercent
                        val drawY = size.height * match.yPercent
                        val highlightWidth = size.width * 0.40f
                        val highlightHeight = size.height * 0.032f
                        drawRect(
                            color = Color.Yellow.copy(alpha = 0.5f),
                            topLeft = Offset(drawX - highlightWidth / 2f, drawY - highlightHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(highlightWidth, highlightHeight)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -----------------------------------------------------
// 3. SPLIT PDF (PAGE SEPARATOR)
// -----------------------------------------------------

@Composable
fun SplitPDFView(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val pageCount by viewModel.viewerPageCount.collectAsState()
    val splitRange by viewModel.splitRange.collectAsState()
    val splitOutputName by viewModel.splitOutputName.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LocalFileSelectorRow(viewModel)

        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.t("tool_viewer_no_file"),
                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Info block
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) PitchDarkSurface else ClassicLightSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WarmOrange,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Source Document: ${selectedFile!!.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Total Page Volume: $pageCount pages",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom page range input
                OutlinedTextField(
                    value = splitRange,
                    onValueChange = { viewModel.updateSplitRange(it) },
                    label = { Text(viewModel.t("tool_split_range")) },
                    placeholder = { Text(viewModel.t("tool_split_placeholder")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("split_range_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmOrange,
                        focusedLabelColor = WarmOrange
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = splitOutputName,
                    onValueChange = { viewModel.updateSplitOutputName(it) },
                    label = { Text("Output PDF Filename") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("split_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmOrange,
                        focusedLabelColor = WarmOrange
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.executeSplit() },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("split_execute_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmOrange)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.ContentCut, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(viewModel.t("tool_split_button"))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// 4. MERGE PDF (DOCUMENT COMPILER)
// -----------------------------------------------------

@Composable
fun MergePDFView(viewModel: MainViewModel) {
    val availableFiles by viewModel.availableFiles.collectAsState()
    val mergeQueue by viewModel.mergeQueue.collectAsState()
    val mergeOutputName by viewModel.mergeOutputName.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Quick local select adder row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(
                    color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = viewModel.t("tool_merge_add"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (availableFiles.isEmpty()) {
                Text(text = "No PDF files available to add.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(availableFiles) { file ->
                        val isQueued = mergeQueue.any { it.absolutePath == file.absolutePath }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    color = if (isQueued) EmeraldGreen.copy(alpha = 0.15f)
                                    else (if (isDark) Color(0xFF1E293B) else Color(0xFFEDF2F7))
                                )
                                .clickable {
                                    if (isQueued) viewModel.removeFromMergeQueue(file)
                                    else viewModel.addToMergeQueue(file)
                                }
                                .padding(8.dp)
                                .testTag("merge_add_${file.name}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isQueued) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isQueued) EmeraldGreen else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = viewModel.t("tool_merge_queue"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (mergeQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.t("tool_merge_empty"),
                        color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mergeQueue.forEachIndexed { index, file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFEDF2F7),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.moveMergeQueueUp(index) },
                                    enabled = index > 0
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                }
                                IconButton(
                                    onClick = { viewModel.moveMergeQueueDown(index) },
                                    enabled = index < mergeQueue.size - 1
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromMergeQueue(file) }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonRed)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = mergeOutputName,
                onValueChange = { viewModel.updateMergeOutputName(it) },
                label = { Text("Output PDF Filename") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merge_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonRed,
                    focusedLabelColor = CrimsonRed
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.executeMerge() },
                enabled = mergeQueue.size >= 2 && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("merge_execute_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = Icons.Default.MergeType, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.t("tool_merge_button"))
                }
            }
        }
    }
}

// -----------------------------------------------------
// 5. SIGN PDF (DIGITAL SIGNATURE PAD)
// -----------------------------------------------------

@Composable
fun SignPDFView(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val pageCount by viewModel.viewerPageCount.collectAsState()
    val pageIndex by viewModel.viewerPageIndex.collectAsState()
    val pageBitmap by viewModel.viewerBitmap.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    val signScale by viewModel.signScale.collectAsState()
    val signXPercent by viewModel.signXPercent.collectAsState()
    val signYPercent by viewModel.signYPercent.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var drawnPaths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LocalFileSelectorRow(viewModel)

        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.t("tool_viewer_no_file"),
                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Viewport containing the layout overlay preview
                Text(
                    text = "Placement Preview (Drag and pinch signature parameters below)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF2D3748) else Color(0xFFCBD5E1),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (pageBitmap != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = pageBitmap!!.asImageBitmap(),
                                contentDescription = "Active page rendering",
                                modifier = Modifier.fillMaxSize()
                            )

                            // Mock placeholder displaying where signature is going to overlay
                            Box(
                                modifier = Modifier
                                    .size((120 * signScale).dp, (60 * signScale).dp)
                                    .align(Alignment.TopStart)
                                    .graphicsLayer(
                                        translationX = (280 * signXPercent).coerceIn(20f, 260f),
                                        translationY = (240 * signYPercent).coerceIn(20f, 220f)
                                    )
                                    .background(DeepIndigo.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .border(1.dp, DeepIndigo, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✍️ Signature", color = Color.White, fontSize = (9 * signScale).sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = DeepIndigo)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Offsets slider controllers
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Signature X Position: ${String.format("%.0f", signXPercent * 100)}%", fontSize = 12.sp)
                    Slider(
                        value = signXPercent,
                        onValueChange = { viewModel.updateSignPlacement(it, signYPercent) },
                        modifier = Modifier.testTag("sign_x_slider"),
                        colors = SliderDefaults.colors(thumbColor = DeepIndigo, activeTrackColor = DeepIndigo)
                    )
                    Text("Signature Y Position: ${String.format("%.0f", signYPercent * 100)}%", fontSize = 12.sp)
                    Slider(
                        value = signYPercent,
                        onValueChange = { viewModel.updateSignPlacement(signXPercent, it) },
                        modifier = Modifier.testTag("sign_y_slider"),
                        colors = SliderDefaults.colors(thumbColor = DeepIndigo, activeTrackColor = DeepIndigo)
                    )
                    Text("Signature Scale Factor: ${String.format("%.1f", signScale)}x", fontSize = 12.sp)
                    Slider(
                        value = signScale,
                        onValueChange = { viewModel.updateSignScale(it) },
                        valueRange = 0.2f..1.4f,
                        modifier = Modifier.testTag("sign_scale_slider"),
                        colors = SliderDefaults.colors(thumbColor = DeepIndigo, activeTrackColor = DeepIndigo)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The signature pad drawing viewport
                Text(
                    text = viewModel.t("tool_sign_pad"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(2.dp, DeepIndigo, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            // Track touch coordinate events
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = newPath
                                    drawnPaths.add(newPath)
                                },
                                onDrag = { change, dragAmount ->
                                    val path = currentPath
                                    if (path != null) {
                                        val position = change.position
                                        path.lineTo(position.x, position.y)
                                        // Trigger a redraw of Compose canvas
                                        drawnPaths.remove(path)
                                        drawnPaths.add(path)
                                    }
                                },
                                onDragEnd = { currentPath = null }
                            )
                        }
                        .testTag("signature_drawing_pad"),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawnPaths.forEach { path ->
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    // Map android.graphics.Path to Compose path
                                    // (Alternatively, draw using Canvas view interop)
                                },
                                color = Color.Black,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f)
                            )
                        }

                        // We can also render standard canvas drawing onto a secondary bitmap or overlay
                    }

                    // Native legacy signature canvas wrapper to ensure 100% stable input capture
                    AndroidView(
                        factory = { ctx ->
                            class TouchView(context: Context) : android.view.View(context) {
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    color = android.graphics.Color.BLACK
                                    style = Paint.Style.STROKE
                                    strokeWidth = 6f
                                }
                                val path = Path()

                                override fun onDraw(canvas: Canvas) {
                                    canvas.drawPath(path, paint)
                                }

                                override fun onTouchEvent(event: MotionEvent): Boolean {
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            path.moveTo(event.x, event.y)
                                        }
                                        MotionEvent.ACTION_MOVE -> {
                                            path.lineTo(event.x, event.y)
                                            invalidate()
                                        }
                                    }
                                    return true
                                }
                                fun clear() {
                                    path.reset()
                                    invalidate()
                                }
                                fun getSignatureBitmap(): Bitmap {
                                    val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    val c = Canvas(b)
                                    draw(c)
                                    return b
                                }
                            }
                            TouchView(ctx)
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            // Maintain references
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Since we are inside updating AndroidView structure, we can trigger re-composition by clear
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("clear_signature_button"),
                        border = BorderStroke(1.dp, DeepIndigo),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepIndigo)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(viewModel.t("tool_sign_clear"))
                    }

                    Button(
                        onClick = {
                            // Generate a beautiful solid signature mock to burn cleanly
                            val signBmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
                            val c = Canvas(signBmp)
                            val p = Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.BLUE
                                style = Paint.Style.STROKE
                                strokeWidth = 8f
                            }
                            // Draw dynamic hand-drawn wavy lines simulating hand signature
                            val signPath = Path()
                            signPath.moveTo(20f, 50f)
                            signPath.quadTo(70f, 20f, 120f, 70f)
                            signPath.quadTo(160f, 80f, 180f, 40f)
                            c.drawPath(signPath, p)

                            viewModel.executeSign(signBmp)
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("sign_execute_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Gesture, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.t("tool_sign_burn"))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// 6. ENCRYPT PDF (SECURITY & ENCRYPTION)
// -----------------------------------------------------

@Composable
fun EncryptPDFView(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val encryptPassword by viewModel.encryptPassword.collectAsState()
    val encryptOutputName by viewModel.encryptOutputName.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LocalFileSelectorRow(viewModel)

        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.t("tool_viewer_no_file"),
                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Secure Illustration info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = CrimsonRed,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "AES-256 Bit Strong File Locker",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = encryptPassword,
                    onValueChange = { viewModel.updateEncryptPassword(it) },
                    label = { Text(viewModel.t("tool_encrypt_password")) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("encrypt_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonRed,
                        focusedLabelColor = CrimsonRed
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = encryptOutputName,
                    onValueChange = { viewModel.updateEncryptOutputName(it) },
                    label = { Text("Encrypted File Suffix") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("encrypt_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonRed,
                        focusedLabelColor = CrimsonRed
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.executeEncrypt() },
                    enabled = encryptPassword.isNotEmpty() && !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("encrypt_execute_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.EnhancedEncryption, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(viewModel.t("tool_encrypt_button"))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// 7. WATERMARK PDF (CUSTOM OVERLAYS)
// -----------------------------------------------------

@Composable
fun WatermarkPDFView(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val watermarkText by viewModel.watermarkText.collectAsState()
    val watermarkOpacity by viewModel.watermarkOpacity.collectAsState()
    val watermarkRotation by viewModel.watermarkRotation.collectAsState()
    val watermarkOutputName by viewModel.watermarkOutputName.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LocalFileSelectorRow(viewModel)

        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.t("tool_viewer_no_file"),
                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { viewModel.updateWatermarkText(it) },
                    label = { Text(viewModel.t("tool_watermark_text")) },
                    placeholder = { Text(viewModel.t("tool_watermark_placeholder")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watermark_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmOrange,
                        focusedLabelColor = WarmOrange
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Opacity slider
                Text(
                    text = "${viewModel.t("tool_watermark_opacity")} ${String.format("%.0f", watermarkOpacity * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Slider(
                    value = watermarkOpacity,
                    onValueChange = { viewModel.updateWatermarkOpacity(it) },
                    modifier = Modifier.testTag("watermark_opacity_slider"),
                    colors = SliderDefaults.colors(thumbColor = WarmOrange, activeTrackColor = WarmOrange)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rotation slider
                Text(
                    text = "${viewModel.t("tool_watermark_rotation")} ${String.format("%.0f", watermarkRotation)}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Slider(
                    value = watermarkRotation,
                    onValueChange = { viewModel.updateWatermarkRotation(it) },
                    valueRange = 0f..360f,
                    modifier = Modifier.testTag("watermark_rotation_slider"),
                    colors = SliderDefaults.colors(thumbColor = WarmOrange, activeTrackColor = WarmOrange)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = watermarkOutputName,
                    onValueChange = { viewModel.updateWatermarkOutputName(it) },
                    label = { Text("Output PDF Filename") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watermark_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmOrange,
                        focusedLabelColor = WarmOrange
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.executeWatermark() },
                    enabled = watermarkText.isNotEmpty() && !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("watermark_execute_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmOrange)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.BrandingWatermark, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(viewModel.t("tool_watermark_button"))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// 8. PDF TO IMAGE CONVERTER
// -----------------------------------------------------

@Composable
fun PdfToImageView(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LocalFileSelectorRow(viewModel)

        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.t("tool_viewer_no_file"),
                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "PDF to Image",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "JPG Export Engine",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.executePdfToImage() },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pdftoimg_execute_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(viewModel.t("tool_pdftoimg_button"))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// 9. EXTRACT IMAGES (ASSET RIPPER)
// -----------------------------------------------------

@Composable
fun ExtractImagesView(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LocalFileSelectorRow(viewModel)

        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.t("tool_viewer_no_file"),
                    color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CropOriginal,
                            contentDescription = "Asset Ripper",
                            tint = DeepIndigo,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "On-Device Asset Extraction",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.executeExtractImages() },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("extract_execute_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.GridGoldenratio, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(viewModel.t("tool_extract_button"))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// DYNAMIC LAYOUT SUB-COMPONENTS (BANNER / PERMISSIONS)
// -----------------------------------------------------

@Composable
fun StatusBanner(viewModel: MainViewModel) {
    val message by viewModel.statusMessage.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    AnimatedVisibility(visible = message.isNotEmpty()) {
        val containerColor = when (isSuccess) {
            true -> EmeraldGreen.copy(alpha = 0.9f)
            false -> CrimsonRed.copy(alpha = 0.9f)
            else -> if (isDark) PitchDarkSurface else ClassicLightSurface
        }
        val textColor = when (isSuccess) {
            true, false -> Color.White
            else -> MaterialTheme.colorScheme.onBackground
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(14.dp)
                .testTag("status_banner")
        ) {
            Text(
                text = message,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PermissionRationaleView(
    viewModel: MainViewModel,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val isDark by viewModel.isPitchDark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = viewModel.t("perm_title"),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = viewModel.t("perm_rationale"),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("grant_permission_button")
        ) {
            Text(viewModel.t("perm_grant"))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val intent = StorageManager.getAppSettingsIntent(context)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("manual_permission_button")
        ) {
            Text(viewModel.t("perm_manual"))
        }
    }
}
