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
import com.uaa.gastos.model.Category
import com.uaa.gastos.ui.viewmodel.CategoryViewModel
import com.uaa.gastos.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel() // Nuevo
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val categories by categoryViewModel.categories.collectAsState() // Nuevo
    var selectedCategory by remember { mutableStateOf<Category?>(null) } // Nuevo
    var categoryDropdownExpanded by remember { mutableStateOf(false) } // Nuevo

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
            verticalArrangement = Arrangement.spacedBy(12.dp) // Aumentado espacio
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && title.isBlank(),
                singleLine = true
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
                isError = showError && (rawAmount.toDoubleOrNull() ?: 0.0) == 0.0,
                singleLine = true
            )

            // Selector de Categoría (Nuevo)
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Seleccionar Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Opción "Sin Categoría"
                    DropdownMenuItem(
                        text = { Text("Sin Categoría") },
                        onClick = {
                            selectedCategory = null
                            categoryDropdownExpanded = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                    // Opción para añadir nueva categoría rápidamente
                    DropdownMenuItem(
                        text = { Text("+ Añadir nueva categoría...", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            categoryDropdownExpanded = false
                            navController.navigate(Routes.ADD_CATEGORY)
                        }
                    )
                }
            }


            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
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
                        // Opcional: Validar que se seleccionó una categoría si es obligatorio
                        // selectedCategory == null && categories.isNotEmpty() -> {
                        //     errorMessage = "Debes seleccionar una categoría."
                        //     showError = true
                        // }
                        else -> {
                            showError = false
                            transactionViewModel.addTransaction( // Modificado
                                title = title,
                                amount = parsedAmount,
                                date = java.time.LocalDate.now().toString(),
                                categoryId = selectedCategory?.id // Nuevo
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