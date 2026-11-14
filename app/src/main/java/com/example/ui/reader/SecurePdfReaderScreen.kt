package com.example.ui.reader

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.PdfDocumentMeta
import com.example.data.model.PdfPayload
import com.example.data.model.UiState
import com.example.security.EncryptedPdfCacheManager
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ErrorRecoveryBanner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiquidProgressIndicator
import com.example.ui.theme.LiquidYellow
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SecurePdfReaderScreen(
    document: PdfDocumentMeta?,
    payloadState: UiState<PdfPayload>,
    userEmail: String,
    userId: String,
    initialPage: Int,
    onSaveProgress: (Int) -> Unit,
    onRetryLoad: () -> Unit,
    onLogEvent: (String) -> Unit,
    onBackgroundAutoLock: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val cacheManager = remember(context) { EncryptedPdfCacheManager(context) }

    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(initialPage.coerceAtLeast(0)) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showSearch by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }

    // Enforce Android FLAG_SECURE for screenshot/recording/recents protection while reader is active
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            try {
                pdfRenderer?.close()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
            cacheManager.wipeFileSecurely(tempFile)
            cacheManager.wipeAllTemporaryFiles()
            onLogEvent("reader_closed")
        }
    }

    // Automatically wipe transient PDF files and lock session when app moves to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                try {
                    pdfRenderer?.close()
                } catch (_: Exception) {}
                try {
                    pfd?.close()
                } catch (_: Exception) {}
                pdfRenderer = null
                pfd = null
                cacheManager.wipeFileSecurely(tempFile)
                cacheManager.wipeAllTemporaryFiles()
                onLogEvent("session_locked")
                onBackgroundAutoLock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Decrypt and initialize PdfRenderer when authorized payload arrives
    LaunchedEffect(payloadState) {
        if (payloadState is UiState.Success && document != null) {
            withContext(Dispatchers.IO) {
                try {
                    val base64 = payloadState.data.pdfBase64
                    if (base64.isBlank()) {
                        renderError = "Protected PDF content is empty."
                        return@withContext
                    }
                    val (descriptor, file) = cacheManager.openProtectedPdfDescriptor(document.id, base64)
                    val renderer = PdfRenderer(descriptor)
                    pfd = descriptor
                    tempFile = file
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    if (currentPage >= renderer.pageCount) {
                        currentPage = 0
                    }
                    renderError = null
                } catch (e: Exception) {
                    renderError = e.localizedMessage ?: "Failed to decrypt or render PDF."
                }
            }
        }
    }

    // Render current page bitmap off the main thread
    LaunchedEffect(pdfRenderer, currentPage) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        if (currentPage < 0 || currentPage >= renderer.pageCount) return@LaunchedEffect
        onSaveProgress(currentPage)
        withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(currentPage)
                val targetW = (page.width * 2).coerceIn(600, 1800)
                val targetH = ((targetW.toFloat() / page.width.toFloat()) * page.height).toInt().coerceAtLeast(400)
                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pageBitmap = bitmap
            } catch (e: Exception) {
                renderError = e.localizedMessage ?: "Page rendering failed."
            }
        }
    }

    val searchMatches = remember(payloadState, searchTerm) {
        if (payloadState !is UiState.Success || searchTerm.isBlank()) emptyList()
        else {
            payloadState.data.pageTexts.mapIndexedNotNull { index, text ->
                if (text.contains(searchTerm, ignoreCase = true)) index else null
            }
        }
    }

    val watermarkText = remember(userEmail, userId) {
        val dateStr = SimpleDateFormat("dd-MM-yy HH:mm", Locale.getDefault()).format(Date())
        "$userEmail • ${userId.take(8)} • $dateStr"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Secure Top Header (No Share, No Print, No Export)
        Surface(color = ObsidianSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close Reader", tint = PureWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document?.title ?: "Secure PDF",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = LiquidYellow, modifier = Modifier.size(13.dp))
                        Text(
                            text = "FLAG_SECURE ACTIVE • WATERMARKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiquidYellow
                        )
                    }
                }
                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.testTag("reader_search_toggle")) {
                    Icon(
                        imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "Search inside PDF",
                        tint = LiquidYellow
                    )
                }
                IconButton(onClick = {
                    zoomScale = (zoomScale - 0.35f).coerceAtLeast(1f)
                    if (zoomScale == 1f) panOffset = Offset.Zero
                }) {
                    Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom Out", tint = PureWhite)
                }
                IconButton(onClick = {
                    zoomScale = (zoomScale + 0.35f).coerceAtMost(3.5f)
                }) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom In", tint = PureWhite)
                }
            }
        }

        if (showSearch && payloadState is UiState.Success) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchTerm,
                    onValueChange = { searchTerm = it },
                    placeholder = { Text("Search keywords within document...", color = MutedZinc) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LiquidYellow,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reader_search_input")
                )
                if (searchTerm.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchMatches.isEmpty()) "No matches found" else "${searchMatches.size} matching page(s): ${searchMatches.joinToString { "P${it + 1}" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiquidYellow
                        )
                        if (searchMatches.isNotEmpty()) {
                            val nextMatch = searchMatches.firstOrNull { it > currentPage } ?: searchMatches.first()
                            Surface(
                                color = LiquidYellow,
                                contentColor = DeepBlack,
                                shape = RoundedCornerShape(10.dp),
                                onClick = { currentPage = nextMatch }
                            ) {
                                Text(
                                    text = "Jump P${nextMatch + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main PDF Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                payloadState is UiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Decrypting Protected PDF...", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidProgressIndicator(modifier = Modifier.fillMaxWidth(0.7f))
                    }
                }
                payloadState is UiState.Error -> {
                    ErrorRecoveryBanner(
                        title = "Access Denied or Load Failed",
                        message = payloadState.message,
                        isNetworkError = payloadState.isNetworkError,
                        onRetry = onRetryLoad
                    )
                }
                renderError != null -> {
                    ErrorRecoveryBanner(
                        title = "PDF Render Failure",
                        message = renderError!!,
                        onRetry = onRetryLoad
                    )
                }
                pageBitmap != null -> {
                    val bmp = pageBitmap!!
                    val aspect = bmp.width.toFloat() / bmp.height.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                    zoomScale = newScale
                                    panOffset = if (newScale > 1f) panOffset + pan else Offset.Zero
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (zoomScale > 1.1f) {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        } else {
                                            zoomScale = 2f
                                        }
                                    }
                                )
                            }
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Protected PDF Page ${currentPage + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        DynamicWatermarkOverlay(
                            watermark = watermarkText,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    LiquidProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
                }
            }
        }

        // Bottom Page Navigation & Progress
        if (pageCount > 0) {
            Surface(color = ObsidianSurface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0,
                            modifier = Modifier.testTag("reader_prev_page")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous Page", tint = if (currentPage > 0) LiquidYellow else MutedZinc)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Page ${currentPage + 1} of $pageCount",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            val pct = (((currentPage + 1).toFloat() / pageCount.toFloat()) * 100).toInt()
                            Text(
                                text = "Reading Progress: $pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = LiquidYellow
                            )
                        }
                        IconButton(
                            onClick = { if (currentPage < pageCount - 1) currentPage++ },
                            enabled = currentPage < pageCount - 1,
                            modifier = Modifier.testTag("reader_next_page")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next Page", tint = if (currentPage < pageCount - 1) LiquidYellow else MutedZinc)
                        }
                    }
                    if (pageCount > 1) {
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { currentPage = it.toInt().coerceIn(0, pageCount - 1) },
                            valueRange = 0f..(pageCount - 1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = LiquidYellow,
                                activeTrackColor = LiquidYellow,
                                inactiveTrackColor = CharcoalElevated
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicWatermarkOverlay(
    watermark: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val style = TextStyle(
        color = Color(0x36101010),
        fontSize = 11.sp,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val textLayout = textMeasurer.measure(watermark, style = style)
        rotate(degrees = -28f) {
            val stepX = textLayout.size.width + 80f
            val stepY = 140f
            var y = -size.height
            while (y < size.height * 2f) {
                var x = -size.width
                while (x < size.width * 2f) {
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x, y)
                    )
                    x += stepX
                }
                y += stepY
            }
        }
    }
}
