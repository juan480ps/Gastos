// AddTRansactionScreen

package com.uaa.misgastosapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.viewmodel.BudgetViewModel
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val categories by categoryViewModel.categories.collectAsState()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    val currentExpenseAmount = if (parsedAmount < 0) parsedAmount * -1 else parsedAmount

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
                            transactionViewModel.addTransaction(
                                title = title,
                                amount = parsedAmount,
                                date = LocalDate.now().toString(),
                                categoryId = selectedCategory?.id
                            )

                            if (parsedAmount < 0 && selectedCategory != null) {
                                coroutineScope.launch {
                                    val monthYearStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                    val budgetEntity = budgetViewModel.getBudgetForCategory(selectedCategory!!.id, monthYearStr).firstOrNull()

                                    if (budgetEntity != null && budgetEntity.amount > 0) {

                                        val previousTransactions = transactionViewModel.transactions.firstOrNull() ?: emptyList()
                                        val spentBeforeThisTransaction = previousTransactions
                                            .filter {
                                                it.categoryId == selectedCategory!!.id &&
                                                        it.date.startsWith(monthYearStr) &&
                                                        it.amount < 0
                                            }
                                            .sumOf { it.amount * -1 }

                                        val totalSpentAfterThisTransaction = spentBeforeThisTransaction + currentExpenseAmount

                                        if (totalSpentAfterThisTransaction > budgetEntity.amount) {
                                            val exceededBy = totalSpentAfterThisTransaction - budgetEntity.amount
                                            Toast.makeText(
                                                context,
                                                "¡Alerta! Has excedido el presupuesto de ${selectedCategory!!.name} en ${currencyFormat.format(exceededBy)}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else if (totalSpentAfterThisTransaction > budgetEntity.amount * 0.85) {
                                            Toast.makeText(
                                                context,
                                                "¡Cuidado! Te estás acercando al límite del presupuesto de ${selectedCategory!!.name}.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                            navController.popBackStack()
                        }
                    }
                }) {
                Text("Guardar")
            }
        }
    }
}