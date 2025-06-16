package com.uaa.gastos

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uaa.gastos.ui.HomeScreen
import com.uaa.gastos.ui.AddTransactionScreen

object Routes {
    const val HOME = "home"
    const val ADD_TRANSACTION = "add_transaction"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(Routes.ADD_TRANSACTION) {
            AddTransactionScreen(navController)
        }
    }
}
