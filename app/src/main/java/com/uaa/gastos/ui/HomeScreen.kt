package com.uaa.gastos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.gastos.Routes
import com.uaa.gastos.ui.viewmodel.TransactionViewModel
import androidx.compose.runtime.getValue
import com.uaa.gastos.data.TransactionEntity
import java.text.NumberFormat
import java.util.*
import com.uaa.gastos.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: TransactionViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Gastos") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Routes.ADD_TRANSACTION)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            val balance = transactions.sumOf { it.amount }
            val formattedBalance = numberFormat.format(balance)
            SummaryCard(balance = balance)
//            SummaryCard(balance = balance)

            Spacer(modifier = Modifier.height(24.dp))
            Text("Transacciones recientes")

            /*LazyColumn {
                items(transactions) { tx ->
                    TransactionItem(
                        title = tx.title,
                        amount = tx.amount,
                        date = tx.date
                    )
                }
            }*/

            LazyColumn {
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

