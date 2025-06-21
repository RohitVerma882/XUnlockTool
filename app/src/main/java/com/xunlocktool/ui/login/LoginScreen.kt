package com.xunlocktool.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewClientCompat

import com.xunlocktool.Constants
import com.xunlocktool.ui.unlock.UnlockViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(viewModel: UnlockViewModel, onLoggedIn: () -> Unit) {
    val cookieManager = CookieManager.getInstance()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf("") }
    var passToken by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }

    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    LaunchedEffect(loginState) {
        if (loginState.isLoggedIn) {
            currentOnLoggedIn()
        } else {
            cookieManager.apply {
                removeSessionCookies(null)
                removeAllCookies(null)
                flush()
            }
            webView?.loadUrl(Constants.LOGIN_URL)
        }
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (!loginState.isLoggedIn) {
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClientCompat()
                        webView = this
                    }
                }, update = { webView ->
                    webView.webViewClient = object : WebViewClientCompat() {
                        override fun onPageStarted(
                            view: WebView?, url: String?, favicon: Bitmap?
                        ) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            canGoBack = webView.canGoBack()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false

                            val cookies = cookieManager.getCookie(Constants.LOGIN_URL)
                            if (cookieManager.hasCookies() && cookies != null) {
                                cookies.split(";").forEach { cookie ->
                                    val parts = cookie.split("=", limit = 2)
                                    if (parts.size < 2) return@forEach
                                    val name = parts[0].trim()
                                    val value = parts[1].trim()

                                    if ("passToken" == name) {
                                        passToken = value
                                    } else if ("userId" == name) {
                                        userId = value
                                    } else if ("deviceId" == name) {
                                        deviceId = value
                                    }

                                    if (userId.isNotEmpty() && passToken.isNotEmpty() && deviceId.isNotEmpty()) {
                                        viewModel.saveAuth(userId, passToken, deviceId)
                                    }
                                }
                            }
                        }
                    }
                    webView.loadUrl(Constants.LOGIN_URL)
                })
            }

            if (isLoading) {
                webView?.isVisible = false
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                webView?.isVisible = true
            }
        }
    }
}