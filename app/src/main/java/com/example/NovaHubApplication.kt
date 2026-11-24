package com.example

import android.app.Application
import android.os.Build
import android.util.Log
import android.webkit.WebView

class NovaHubApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        prepareIsolatedWebViewStorage()
    }

    /**
     * Prevents Chromium Simple Cache Backend errors ("cache directory inaccessible right after creation")
     * caused by stale/corrupted cache locks from prior WebView renderer crashes or multi-profile conflicts.
     */
    private fun prepareIsolatedWebViewStorage() {
        try {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            // Clean up any stale or corrupted WebView cache trees (Default or suffixed) before WebView initializes
            cacheDir.listFiles()?.forEach { entry ->
                if (entry.name.startsWith("WebView")) {
                    entry.deleteRecursively()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to clean legacy WebView cache directory", t)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Isolate WebView data & cache into a clean app-specific suffix before first WebView init
                WebView.setDataDirectorySuffix("novahub")
            } catch (t: Throwable) {
                Log.w(TAG, "WebView data directory suffix setup skipped", t)
            }
        }
    }

    companion object {
        private const val TAG = "NovaHubApplication"
    }
}
