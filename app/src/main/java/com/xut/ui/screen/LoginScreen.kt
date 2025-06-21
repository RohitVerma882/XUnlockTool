package com.xut.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.webkit.WebViewClientCompat

import com.xut.Constants
import com.xut.domain.model.User

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onSaveUser: (User) -> Unit
) {
    val currentOnSaveUser by rememberUpdatedState(onSaveUser)

    LaunchedEffect(Unit) {
        CookieManager.getInstance().apply {
            removeSessionCookies(null)
            removeAllCookies(null)
            flush()
        }
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    @Suppress("unused")
    class WebAppInterface {
        @JavascriptInterface
        fun onLoginSuccess(passToken: String, userId: String, deviceId: String) {
            val user = User(passToken, userId, deviceId)
            if (user.hasValues) {
                currentOnSaveUser(user)
            }
        }
    }

    val webViewClient = object : WebViewClientCompat() {
        override fun onPageStarted(
            view: WebView?, url: String?, favicon: Bitmap?
        ) {
            super.onPageStarted(view, url, favicon)
            isLoading = true
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            canGoBack = view?.canGoBack() == true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            isLoading = false

            val jsCode = """
                    javascript:(function() {
                        var passToken = '';
                        var userId = '';
                        var deviceId = '';

                        document.cookie.split(';').forEach(function(cookie) {
                            var parts = cookie.trim().split('=', 2);
                            if (parts.length === 2) {
                                if (parts[0] === 'passToken') passToken = parts[1];
                                if (parts[0] === 'userId') userId = parts[1];
                                if (parts[0] === 'deviceId') deviceId = parts[1];
                            }
                        });

                        if (passToken && userId && deviceId) {
                            AndroidInterface.onLoginSuccess(passToken, userId, deviceId);
                        }
                    })();
                """.trimIndent()
            view?.evaluateJavascript(jsCode, null)

//            CookieManager.getInstance()
//                .getCookie(Constants.LOGIN_URL)
//                ?.split(";")
//                ?.map { it.trim() }
//                ?.forEach { cookie ->
//                    val parts = cookie.split("=", limit = 2)
//                    if (parts.size == 2) {
//                        val name = parts.getOrNull(0)
//                        val value = parts.getOrNull(1)
//
//                        when (name) {
//                            "passToken" -> {
//                                currentUser.passToken = value!!
//                            }
//
//                            "userId" -> {
//                                currentUser.userId = value!!
//                            }
//
//                            "deviceId" -> {
//                                currentUser.deviceId = value!!
//                            }
//                        }
//
//                        if (currentUser.hasValues) {
//                            currentOnSaveUser(currentUser)
//                        }
//                    }
//                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    this.webViewClient = webViewClient

                    addJavascriptInterface(WebAppInterface(), "AndroidInterface")

                    loadUrl(Constants.LOGIN_URL)
                }.also { webView = it }
            })

        if (isLoading) {
            webView?.isVisible = false
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            webView?.isVisible = true
        }
    }
}