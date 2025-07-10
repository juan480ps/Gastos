// HomeScreen

package com.uaa.misgastosapp.ui
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.model.Budget
import com.uaa.misgastosapp.model.Transaction
import com.uaa.misgastosapp.ui.viewmodel.AuthViewModel
import com.uaa.misgastosapp.ui.viewmodel.BudgetViewModel
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import com.uaa.misgastosapp.ui.viewmodel.TransactionViewModel
import com.uaa.misgastosapp.utils.Result
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)
@SuppressLint("SuspiciousIndentation")
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
    val operationStatus by transactionViewModel.operationStatus.collectAsState()
    val budgetsWithSpending by budgetViewModel.budgetsWithSpendingForCurrentMonth.collectAsState(initial = emptyList())
    val currentYearMonth by budgetViewModel.currentMonthYear.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    val monthHeaderFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("es", "ES"))
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var collapsedMonths by rememberSaveable { mutableStateOf(emptySet<YearMonth>()) }


    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteTransactionDialog by rememberSaveable { mutableStateOf(false) }

    val userName = authViewModel.getCurrentUserName() ?: "Usuario"
    val isOnline by authViewModel.isOnlineMode.collectAsState()
    val context = LocalContext.current
    val navigationItems = listOf(
        BottomNavItem("Gráficos", Icons.Default.PieChart, Routes.CHARTS_SCREEN),
        BottomNavItem("Recurrentes", Icons.Default.Autorenew, Routes.MANAGE_RECURRING_TRANSACTIONS),
        BottomNavItem("Presupuestos", Icons.Default.Assessment, Routes.MANAGE_BUDGETS),
        BottomNavItem("Categorías", Icons.Default.Category, Routes.CATEGORIES_LIST),
    )

    LaunchedEffect(operationStatus) {
        val status = operationStatus
        when (status) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                transactionViewModel.clearOperationStatus()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                transactionViewModel.clearOperationStatus()
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        recurringTransactionViewModel.processDueRecurringTransactions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, $userName") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isOnline) MaterialTheme.colorScheme.primary else Color.Gray,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    if (!isOnline) {
                        Text(
                            "Modo Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = false,
                            onClick = { navController.navigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Routes.ADD_TRANSACTION)
                },
                containerColor = Color(android.graphics.Color.parseColor("#c2b1f0")),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Gasto")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                SummaryCard(balance = transactions.sumOf { it.amount })
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Resumen de Presupuestos (${currentYearMonth.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (budgetsWithSpending.any { it.amount > 0 }) {
                items(budgetsWithSpending.filter { it.amount > 0 }) { budgetItem ->
                    BudgetStatusItem(budgetItem, currencyFormat)
                }
            } else {
                item {
                    Text(
                        "No hay presupuestos configurados para este mes. Ve a 'Gestionar Presupuestos'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Transacciones Recientes", style = MaterialTheme.typography.titleMedium)
            }

            if (operationStatus is Result.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay transacciones registradas.")
                    }
                }
            } else {
                val dateParser = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                val groupedTransactions = transactions
                    .groupBy {
                        try {
                            YearMonth.from(LocalDate.parse(it.date, dateParser))
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .filterKeys { it != null }
                    .toSortedMap(compareByDescending { it!! })

                groupedTransactions.forEach { (yearMonth, monthTransactions) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp)
                                .clickable {
                                    collapsedMonths = if (yearMonth!! in collapsedMonths) {
                                        collapsedMonths - yearMonth
                                    } else {
                                        collapsedMonths + yearMonth
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = yearMonth!!.format(monthHeaderFormatter)
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value - 1).sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.weight(1f)
                            )
                            Divider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                            val isCollapsed = yearMonth in collapsedMonths
                            Icon(
                                imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isCollapsed) "Expandir mes" else "Minimizar mes",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    if (yearMonth !in collapsedMonths) {
                        items(monthTransactions, key = { it.id }) { tx ->
                            TransactionItem(
                                transaction = tx,
                                onDelete = {

                                    transactionToDelete = tx
                                    showDeleteTransactionDialog = true
                                }
                            )
                        }
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
                        showLogoutDialog = false
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

    if (showDeleteTransactionDialog && transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteTransactionDialog = false
                transactionToDelete = null
            },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar la transacción '${transactionToDelete?.title}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { transactionViewModel.deleteTransaction(it.id) }
                        showDeleteTransactionDialog = false
                        transactionToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteTransactionDialog = false
                        transactionToDelete = null
                    }
                ) {
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
        budget.progress == 1f -> Color(0xFFFFA000)
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
        } else if (budget.progress > 0.85f && budget.progress < 1f) {
            Text(
                "¡Cuidado! Cercano al límite.",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                modifier = Modifier.align(Alignment.End)
            )
        } else if (budget.progress == 1f) {
            Text(
                "¡Cuidado! Límite alcanzado.",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}