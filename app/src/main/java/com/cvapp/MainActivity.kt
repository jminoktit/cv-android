package com.cvapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        webView = findViewById(R.id.webview)
        val fabSettings = findViewById<FloatingActionButton>(R.id.fabSettings)

        setupWebView()

        fabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, SETTINGS_REQUEST)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            minimumFontSize = 14
        }

        webView.addJavascriptInterface(CvBridge(this), "CvBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applySavedSettings()
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun applySavedSettings() {
        val theme = prefs.getString("theme", "dark") ?: "dark"
        val fontSize = prefs.getString("fontSize", "md") ?: "md"
        val viewMode = prefs.getString("viewMode", "standard") ?: "standard"
        val compact = prefs.getBoolean("compact", false)
        val showAvatar = prefs.getBoolean("showAvatar", true)
        val showProjectLinks = prefs.getBoolean("showProjectLinks", true)
        val visibleJson = prefs.getString("visible", "{}") ?: "{}"
        val sectionOrder = prefs.getString("sectionOrder", "") ?: ""

        webView.evaluateJavascript("""
            (function() {
                try {
                    if (typeof settings !== 'undefined') {
                        settings.theme = '$theme';
                        settings.fontSize = '$fontSize';
                        settings.viewMode = '$viewMode';
                        settings.compact = $compact;
                        settings.showAvatar = $showAvatar;
                        settings.showProjectLinks = $showProjectLinks;
                        try { settings.visible = JSON.parse('$visibleJson'); } catch(e) {}
                        if ('$sectionOrder') {
                            try { settings.sectionOrder = JSON.parse('$sectionOrder'); } catch(e) {}
                        }
                        saveSettings();
                        loadCSS(THEMES[settings.theme]?.file || THEMES.dark.file);
                        if (typeof applyFontSize === 'function') applyFontSize(settings.fontSize);
                        if (typeof applyViewMode === 'function') applyViewMode(settings.viewMode);
                        if (typeof applyCompact === 'function') applyCompact(settings.compact);
                        if (typeof applyShowAvatar === 'function') applyShowAvatar(settings.showAvatar);
                        if (typeof applyShowProjectLinks === 'function') applyShowProjectLinks(settings.showProjectLinks);
                        if (typeof reorderSectionsDOM === 'function') reorderSectionsDOM();
                        SECTIONS.forEach(function(s) {
                            if (typeof toggleSection === 'function') toggleSection(s, settings.visible[s] !== false);
                        });
                    }
                } catch(e) { console.log('CvBridge error: ' + e.message); }
            })();
        """.trimIndent())
    }

    @Suppress("unused")
    class CvBridge(private val activity: MainActivity) {
        @JavascriptInterface
        fun saveSettings(theme: String, fontSize: String, viewMode: String,
                         compact: Boolean, showAvatar: Boolean, showProjectLinks: Boolean,
                         visibleJson: String, sectionOrderJson: String) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            prefs.edit().apply {
                putString("theme", theme)
                putString("fontSize", fontSize)
                putString("viewMode", viewMode)
                putBoolean("compact", compact)
                putBoolean("showAvatar", showAvatar)
                putBoolean("showProjectLinks", showProjectLinks)
                putString("visible", visibleJson)
                putString("sectionOrder", sectionOrderJson)
                apply()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST && resultCode == RESULT_OK) {
            applySavedSettings()
        }
    }

    companion object {
        private const val SETTINGS_REQUEST = 1001
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
