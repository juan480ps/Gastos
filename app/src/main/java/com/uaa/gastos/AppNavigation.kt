package com.uaa.gastos

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uaa.gastos.ui.HomeScreen
import com.uaa.gastos.ui.AddTransactionScreen
import com.uaa.gastos.ui.AddCategoryScreen // Nuevo
import com.uaa.gastos.ui.CategoriesListScreen // Nuevo

object Routes {
    const val HOME = "home"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_CATEGORY = "add_category"           // Nuevo
    const val CATEGORIES_LIST = "categories_list"     // Nuevo
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
        composable(Routes.ADD_CATEGORY) {           // Nuevo
            AddCategoryScreen(navController)
        }
        composable(Routes.CATEGORIES_LIST) {        // Nuevo
            CategoriesListScreen(navController)
        }
    }
}