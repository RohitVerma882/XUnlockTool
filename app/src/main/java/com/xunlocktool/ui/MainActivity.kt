package com.xunlocktool.ui

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel

import com.xunlocktool.AppContainer
import com.xunlocktool.XUnlockToolApplication
import com.xunlocktool.ui.theme.XUnlockToolTheme
import com.xunlocktool.ui.unlock.UnlockViewModel
import com.xunlocktool.ui.unlock.UnlockViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer = (application as XUnlockToolApplication).appContainer

        setContent {
            XUnlockToolTheme {
                val viewModel = viewModel<UnlockViewModel>(
                    factory = UnlockViewModelFactory(
                        appContainer.authDataStore,
                        appContainer.workManager
                    )
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}