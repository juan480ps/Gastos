package com.uaa.gastos.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, viewModel: TransactionViewModel = viewModel()) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Gasto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && title.isBlank()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    val cleanedInput = input.replace(",", "").filterIndexed { index, c ->
                        c.isDigit() || c == '.' || (c == '-' && index == 0)
                    }
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
                label = { Text("Monto") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && (rawAmount.toDoubleOrNull() ?: 0.0) == 0.0
            )

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(onClick = {
                val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0

                when {
                    title.isBlank() -> {
                        errorMessage = "La descripción no puede estar vacía."
                        showError = true
                    }
                    parsedAmount == 0.0 -> {
                        errorMessage = "El monto no puede ser cero."
                        showError = true
                    }
                    else -> {
                        showError = false
                        viewModel.addTransaction(
                            title = title,
                            amount = parsedAmount,
                            date = java.time.LocalDate.now().toString()
                        )
                        navController.popBackStack()
                    }
                }
            }) {
                Text("Guardar")
            }
        }
    }
}

