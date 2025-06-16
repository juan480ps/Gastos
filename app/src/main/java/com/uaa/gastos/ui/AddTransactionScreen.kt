package com.uaa.gastos.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.gastos.Routes
import com.uaa.gastos.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, viewModel: TransactionViewModel = viewModel()) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") } // Mantiene el número sin formato
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Agregar Gasto") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Descripción") })
//            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Monto") })
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    val cleanedInput = input.replace(",", "").filter { it.isDigit() || it == '.' }
                    rawAmount = cleanedInput

                    val formatted = try {
                        if (cleanedInput.isNotBlank()) {
                            val parsed = cleanedInput.toDouble()
                            numberFormat.format(parsed)
                        } else ""
                    } catch (e: Exception) {
                        cleanedInput
                    }

                    amount = formatted
                },
                label = { Text("Monto") }
            )
            Button(onClick = {
                viewModel.addTransaction(
                    title = title,
//                    amount = amount.toDoubleOrNull() ?: 0.0,
                    amount = rawAmount.toDoubleOrNull() ?: 0.0,
                    date = java.time.LocalDate.now().toString()
                )
                navController.popBackStack()
            }) {
                Text("Guardar")
            }
        }
    }
}

