package com.uaa.gastos

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.uaa.gastos.ui.theme.GastosTheme
import com.uaa.gastos.ui.viewmodel.RecurringTransactionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val recurringTransactionViewModel: RecurringTransactionViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
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