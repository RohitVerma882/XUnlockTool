package com.xut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xut.domain.repository.UserRepository
import com.xut.ui.screen.LoginScreen
import com.xut.ui.screen.UnlockScreen
import com.xut.ui.theme.XUTTheme
import com.xut.ui.viewmodel.UnlockViewModel
import com.xut.ui.viewmodel.UnlockViewModelFactory
import com.xut.ui.viewmodel.UserState

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = (application as XUTApplication).appContainer
        userRepository = appContainer.userRepository

        setContent {
            val viewModel = viewModel<UnlockViewModel>(
                factory = UnlockViewModelFactory(
                    userRepository = userRepository
                )
            )

            val userState by viewModel.userState.collectAsStateWithLifecycle()
            val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()

            XUTTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(userState) { userState ->
                        when (userState) {
                            is UserState.HasUser -> {
                                UnlockScreen(
                                    user = userState.user,
                                    unlockState = unlockState,
                                    onClearUserClick = viewModel::clearUser,
                                    onUnlockClick = viewModel::startUnlock
                                )
                            }

                            is UserState.NoUser -> {
                                LoginScreen(
                                    onSaveUser = viewModel::saveUser
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}