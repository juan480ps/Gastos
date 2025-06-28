// AppNavigation

package com.uaa.misgastosapp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uaa.misgastosapp.ui.*
import com.uaa.misgastosapp.ui.viewmodel.AuthViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_CATEGORY = "add_category"
    const val CATEGORIES_LIST = "categories_list"
    const val MANAGE_BUDGETS = "manage_budgets"
    const val MANAGE_RECURRING_TRANSACTIONS = "manage_recurring_transactions"
    const val ADD_EDIT_RECURRING_TRANSACTION = "add_edit_recurring_transaction"
    const val ARG_RECURRING_TRANSACTION_ID = "recurringTransactionId"
    const val CHARTS_SCREEN = "charts_screen"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    val startDestination = if (isLoggedIn) Routes.HOME else Routes.LOGIN

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && navController.currentDestination?.route != Routes.LOGIN &&
            navController.currentDestination?.route != Routes.REGISTER) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }

        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.ADD_TRANSACTION) { AddTransactionScreen(navController) }
        composable(Routes.ADD_CATEGORY) { AddCategoryScreen(navController) }
        composable(Routes.CATEGORIES_LIST) { CategoriesListScreen(navController) }
        composable(Routes.MANAGE_BUDGETS) { ManageBudgetsScreen(navController) }
        composable(Routes.MANAGE_RECURRING_TRANSACTIONS) {
            ManageRecurringTransactionsScreen(navController)
        }

        composable(
            route = "${Routes.ADD_EDIT_RECURRING_TRANSACTION}/{${Routes.ARG_RECURRING_TRANSACTION_ID}}",
            arguments = listOf(navArgument(Routes.ARG_RECURRING_TRANSACTION_ID) {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt(Routes.ARG_RECURRING_TRANSACTION_ID)
            AddEditRecurringTransactionScreen(
                navController = navController,
                recurringTransactionId = if (transactionId == -1) null else transactionId
            )
        }

        composable(Routes.ADD_EDIT_RECURRING_TRANSACTION) {
            AddEditRecurringTransactionScreen(navController = navController, recurringTransactionId = null)
        }
        composable(Routes.CHARTS_SCREEN) {
            ChartsScreen(navController)
        }
    }
}