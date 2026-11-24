package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

private const val WEBVIEW_TAG = "BaseGameWebView"

/**
 * Base Game WebView Component
 * Crash-resilient sandbox container configured specifically for hosting
 * and executing browser-based HTML5, Canvas 2D, and WebGL kinetic games.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BaseGameWebView(
    gameUrl: String,
    gameTitle: String,
    techStack: List<String> = listOf("HTML5", "Canvas 2D", "WebGL"),
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageLoadingProgress by remember { mutableFloatStateOf(0.1f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isImmersiveFullscreen by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    // Pause/resume WebGL & audio cleanly with lifecycle (destruction is handled by AndroidView.onRelease)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewInstance?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewInstance?.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040507))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("base_game_webview_surface"),
        color = Color(0xFF040507)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040507))
        ) {
            // Game Top Control Bar
            if (showTopBar && !isImmersiveFullscreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurface)
                        .border(1.dp, StudioCardBorder)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("base_webview_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Game",
                                tint = StudioTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(StudioEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = gameTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTextPrimary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "HTML5 Sandbox • ${techStack.take(2).joinToString(" • ")}",
                                fontSize = 10.sp,
                                color = StudioTextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    // Game Control Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                loadError = null
                                isPageLoading = true
                                pageLoadingProgress = 0.1f
                                if (webViewInstance == null) {
                                    reloadNonce++
                                } else {
                                    webViewInstance?.reload()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("base_webview_reload_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Game",
                                tint = StudioCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { isImmersiveFullscreen = !isImmersiveFullscreen },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("base_webview_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = if (isImmersiveFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = StudioGold,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val url = gameUrl.ifBlank { "https://care-survey-89.prantoofficial46.workers.dev/" }
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "কোনো ব্রাউজার পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("base_webview_open_browser_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open in External Browser",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Slim loading progress bar
            AnimatedVisibility(
                visible = isPageLoading && loadError == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { pageLoadingProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = StudioGold,
                    trackColor = StudioSurfaceElevated
                )
            }

            // WebView Game Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF040507))
            ) {
                key(gameUrl, reloadNonce) {
                    AndroidView(
                        factory = { ctx ->
                            if (BuildConfig.DEBUG) {
                                try {
                                    WebView.setWebContentsDebuggingEnabled(true)
                                } catch (_: Throwable) {}
                            }
                            try {
                                if (!ctx.cacheDir.exists()) {
                                    ctx.cacheDir.mkdirs()
                                }
                            } catch (_: Throwable) {}
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    @Suppress("DEPRECATION")
                                    allowFileAccessFromFileURLs = false
                                    @Suppress("DEPRECATION")
                                    allowUniversalAccessFromFileURLs = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    mediaPlaybackRequiresUserGesture = false
                                    javaScriptCanOpenWindowsAutomatically = false
                                    setSupportMultipleWindows(false)
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        safeBrowsingEnabled = true
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        pageLoadingProgress = (newProgress / 100f).coerceIn(0.1f, 1f)
                                        if (newProgress >= 90) {
                                            isPageLoading = false
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isPageLoading = true
                                        loadError = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isPageLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isPageLoading = false
                                            loadError = "গেম লোড হতে পারেনি। ইন্টারনেট সংযোগ বা লিংক পরীক্ষা করুন।"
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        Log.e(WEBVIEW_TAG, "WebView renderer process gone (didCrash=${detail?.didCrash()})")
                                        // Cleanly detach and destroy the dead WebView so app does NOT crash
                                        (view?.parent as? ViewGroup)?.removeView(view)
                                        view?.destroy()
                                        if (webViewInstance == view) {
                                            webViewInstance = null
                                        }
                                        isPageLoading = false
                                        loadError = "ব্রাউজার রেন্ডারার রিস্টার্ট প্রয়োজন। নিচের বাটনে চাপ দিয়ে আবার চালু করুন।"
                                        return true
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        if (url.startsWith("http://") || url.startsWith("https://")) {
                                            return false // Keep inside WebView container
                                        }
                                        return true
                                    }
                                }

                                val targetUrl = gameUrl.ifBlank { "https://care-survey-89.prantoofficial46.workers.dev/" }
                                loadUrl(targetUrl)
                                webViewInstance = this
                            }
                        },
                        onRelease = { webView ->
                            if (webViewInstance == webView) {
                                webViewInstance = null
                            }
                            try {
                                webView.stopLoading()
                                webView.onPause()
                                (webView.parent as? ViewGroup)?.removeView(webView)
                                webView.clearHistory()
                                webView.removeAllViews()
                                webView.destroy()
                            } catch (e: Exception) {
                                Log.w(WEBVIEW_TAG, "Safe WebView release cleanup error", e)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Immersive exit button when in fullscreen
                if (isImmersiveFullscreen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF08090C).copy(alpha = 0.8f))
                            .border(1.dp, StudioCardBorder, CircleShape)
                            .clickable { isImmersiveFullscreen = false }
                            .padding(8.dp)
                            .testTag("exit_fullscreen_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = StudioGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Loading screen overlay
                if (isPageLoading && pageLoadingProgress < 0.35f && loadError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF040507).copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = StudioGold,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "HTML5 গেম ইঞ্জিন চালু হচ্ছে...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StudioTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = gameTitle,
                                fontSize = 11.sp,
                                color = StudioGold,
                                maxLines = 1,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Error & Renderer Recovery Overlay
                if (loadError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(StudioDarkBg.copy(alpha = 0.95f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurface)
                                .border(1.dp, StudioRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StudioRose,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "গেম লোড করা যায়নি",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = loadError ?: "",
                                fontSize = 12.sp,
                                color = StudioTextSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        loadError = null
                                        isPageLoading = true
                                        pageLoadingProgress = 0.1f
                                        reloadNonce++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("আবার চেষ্টা করুন (Retry)", color = Color(0xFF08090C), fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onClose,
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("বন্ধ করুন", color = StudioTextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
