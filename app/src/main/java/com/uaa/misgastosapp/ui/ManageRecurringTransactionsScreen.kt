// ManageRecurringTransactionScreen

package com.uaa.misgastosapp.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.model.RecurringTransaction
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRecurringTransactionsScreen(
    navController: NavController,
    recurringViewModel: RecurringTransactionViewModel = viewModel()
) {
    val recurringTransactions by recurringViewModel.recurringTransactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transacciones Recurrentes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Routes.ADD_EDIT_RECURRING_TRANSACTION) // Navegar sin ID para añadir
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Recurrente")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            if (recurringTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay transacciones recurrentes configuradas.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recurringTransactions) { item ->
                        RecurringTransactionListItem(
                            item = item,
                            onEdit = {
                                navController.navigate("${Routes.ADD_EDIT_RECURRING_TRANSACTION}/${item.id}")
                            },
                            onDelete = { recurringViewModel.deleteRecurringTransaction(item) }
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecurringTransactionListItem(
    item: RecurringTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${if (item.amount < 0) "Ingreso" else "Gasto"}: ${currencyFormat.format(item.amount.let { if(it < 0) it * -1 else it })}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Categoría: ${item.categoryName}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Próximo: ${LocalDate.parse(item.nextDueDate).format(dateFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                if (!item.isActive) {
                    Text("INACTIVA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}