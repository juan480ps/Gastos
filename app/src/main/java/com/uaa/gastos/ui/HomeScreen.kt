// HomeScreen

package com.uaa.gastos.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.gastos.Routes
import com.uaa.gastos.model.Budget
import com.uaa.gastos.ui.viewmodel.AuthViewModel
import com.uaa.gastos.ui.viewmodel.BudgetViewModel
import com.uaa.gastos.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.runtime.LaunchedEffect
import com.uaa.gastos.ui.viewmodel.RecurringTransactionViewModel
import androidx.compose.material.icons.filled.PieChart

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel(),
    recurringTransactionViewModel: RecurringTransactionViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val transactions by transactionViewModel.transactions.collectAsState()
    val budgetsWithSpending by budgetViewModel.budgetsWithSpendingForCurrentMonth.collectAsState(initial = emptyList())
    val currentYearMonth by budgetViewModel.currentMonthYear.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    var showLogoutDialog by remember { mutableStateOf(false) }
    val userName = authViewModel.getCurrentUserName() ?: "Usuario"

    LaunchedEffect(Unit) {
        recurringTransactionViewModel.processDueRecurringTransactions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, $userName") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.CHARTS_SCREEN) }) {
                        Icon(Icons.Filled.PieChart, contentDescription = "Ver Gráficos")
                    }
                    IconButton(onClick = { navController.navigate(Routes.MANAGE_RECURRING_TRANSACTIONS) }) {
                        Icon(Icons.Filled.Autorenew, contentDescription = "Gestionar Recurrentes")
                    }
                    IconButton(onClick = { navController.navigate(Routes.MANAGE_BUDGETS) }) {
                        Icon(Icons.Filled.Assessment, contentDescription = "Gestionar Presupuestos")
                    }
                    IconButton(onClick = { navController.navigate(Routes.CATEGORIES_LIST) }) {
                        Icon(Icons.Filled.Category, contentDescription = "Gestionar Categorías")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Routes.ADD_TRANSACTION)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Gasto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SummaryCard(balance = transactions.sumOf { it.amount })
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Resumen de Presupuestos (${currentYearMonth.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (budgetsWithSpending.any { it.amount > 0 }) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(budgetsWithSpending.filter { it.amount > 0 }) { budgetItem ->
                        BudgetStatusItem(budgetItem, currencyFormat)
                    }
                }
            } else {
                Text(
                    "No hay presupuestos configurados para este mes. Ve a 'Gestionar Presupuestos'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Transacciones Recientes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay transacciones registradas.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(transactions.take(10)) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onDelete = { transactionViewModel.deleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BudgetStatusItem(budget: Budget, currencyFormat: NumberFormat) {
    val progressColor = when {
        budget.progress > 1f -> MaterialTheme.colorScheme.error
        budget.progress > 0.85f -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(budget.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${currencyFormat.format(budget.spentAmount)} / ${currencyFormat.format(budget.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (budget.spentAmount > budget.amount) progressColor else LocalContentColor.current
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { budget.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.3f)
        )
        if (budget.progress > 1f) {
            Text(
                "Excedido en ${currencyFormat.format(budget.spentAmount - budget.amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                modifier = Modifier.align(Alignment.End)
            )
        } else if (budget.progress > 0.85f) {
            Text(
                "¡Cuidado! Cercano al límite.",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}