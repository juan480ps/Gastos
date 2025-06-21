package com.uaa.gastos

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType // Nuevo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument // Nuevo
import com.uaa.gastos.ui.* // Importar todo de ui para simplificar

object Routes {
    const val HOME = "home"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_CATEGORY = "add_category"
    const val CATEGORIES_LIST = "categories_list"
    const val MANAGE_BUDGETS = "manage_budgets"
    const val MANAGE_RECURRING_TRANSACTIONS = "manage_recurring_transactions" // Nuevo
    // Ruta para añadir/editar con argumento opcional
    const val ADD_EDIT_RECURRING_TRANSACTION = "add_edit_recurring_transaction" // Nuevo
    const val ARG_RECURRING_TRANSACTION_ID = "recurringTransactionId" // Nuevo
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.ADD_TRANSACTION) { AddTransactionScreen(navController) }
        composable(Routes.ADD_CATEGORY) { AddCategoryScreen(navController) }
        composable(Routes.CATEGORIES_LIST) { CategoriesListScreen(navController) }
        composable(Routes.MANAGE_BUDGETS) { ManageBudgetsScreen(navController) }
        composable(Routes.MANAGE_RECURRING_TRANSACTIONS) { // Nuevo
            ManageRecurringTransactionsScreen(navController)
        }
        // Ruta para añadir/editar con argumento opcional
        composable(
            route = "${Routes.ADD_EDIT_RECURRING_TRANSACTION}/{${Routes.ARG_RECURRING_TRANSACTION_ID}}",
            arguments = listOf(navArgument(Routes.ARG_RECURRING_TRANSACTION_ID) {
                type = NavType.IntType
                defaultValue = -1 // Usar -1 o 0 para indicar "añadir nuevo" si no se pasa ID
            })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt(Routes.ARG_RECURRING_TRANSACTION_ID)
            AddEditRecurringTransactionScreen(
                navController = navController,
                // Pasar null si es -1, sino el ID
                recurringTransactionId = if (transactionId == -1) null else transactionId
            )
        }
        // Ruta alternativa para añadir nuevo sin pasar ID explícitamente en la URL
        composable(Routes.ADD_EDIT_RECURRING_TRANSACTION) {
            AddEditRecurringTransactionScreen(navController = navController, recurringTransactionId = null)
        }
    }
}