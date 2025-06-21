package com.uaa.gastos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.gastos.Routes
import com.uaa.gastos.ui.viewmodel.TransactionViewModel
import androidx.compose.runtime.getValue
// Comentado TransactionEntity ya que ahora usamos el modelo Transaction
// import com.uaa.gastos.data.TransactionEntity
import java.text.NumberFormat
import java.util.*
// import com.uaa.gastos.model.Transaction // Ya estaba

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: TransactionViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    // val numberFormat = NumberFormat.getNumberInstance(Locale.US) // No se usa aquí directamente

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Gastos") },
                actions = { // Nuevo: Botón para ir a Categorías
                    IconButton(onClick = { navController.navigate(Routes.CATEGORIES_LIST) }) {
                        Icon(Icons.Filled.List, contentDescription = "Gestionar Categorías")
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
                .padding(horizontal = 16.dp, vertical = 8.dp) // Ajustado padding
        ) {
            // val balance = transactions.sumOf { it.amount } // Se calcula en SummaryCard
            // val formattedBalance = numberFormat.format(balance) // Se formatea en SummaryCard
            SummaryCard(balance = transactions.sumOf { it.amount })

            Spacer(modifier = Modifier.height(16.dp)) // Ajustado spacer
            Text("Transacciones recientes", style = MaterialTheme.typography.titleMedium) // Estilo
            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 20.dp), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                    Text("No hay transacciones registradas.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Añadido espaciado
                    items(transactions) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }
}