package com.uaa.gastos

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Para obtener ViewModels a nivel de Activity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope // Para lanzar coroutines
import androidx.navigation.compose.rememberNavController
import com.uaa.gastos.ui.theme.GastosTheme
import com.uaa.gastos.ui.viewmodel.RecurringTransactionViewModel // Importar ViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Obtener el ViewModel a nivel de Activity
    private val recurringTransactionViewModel: RecurringTransactionViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Procesar transacciones recurrentes al inicio
        lifecycleScope.launch { // Usar lifecycleScope para coroutines ligadas al ciclo de vida de la Activity
            recurringTransactionViewModel.processDueRecurringTransactions()
        }

        setContent {
            GastosTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}