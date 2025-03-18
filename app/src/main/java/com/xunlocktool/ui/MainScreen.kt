package com.xunlocktool.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.xunlocktool.ui.login.LoginRoute
import com.xunlocktool.ui.login.LoginScreen
import com.xunlocktool.ui.unlock.UnlockRoute
import com.xunlocktool.ui.unlock.UnlockScreen
import com.xunlocktool.ui.unlock.UnlockViewModel

@Composable
fun MainScreen(viewModel: UnlockViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(viewModel = viewModel, onLoggedIn = {
                navController.navigate(UnlockRoute) {
                    popUpTo(LoginRoute) {
                        inclusive = true
                    }
                }
            })
        }

        composable<UnlockRoute> {
            UnlockScreen(viewModel = viewModel, onLoggedOut = {
                navController.navigate(LoginRoute) {
                    popUpTo(UnlockRoute) {
                        inclusive = true
                    }
                }
            })
        }
    }
}