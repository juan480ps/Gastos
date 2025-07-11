// AddEditRecurringTransactionScreen

package com.uaa.misgastosapp.ui

import android.app.DatePickerDialog
import android.os.Build
import android.widget.DatePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.data.RecurrenceType
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.text.NumberFormat

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringTransactionScreen(
    navController: NavController,
    recurringTransactionId: Int? = null,
    recurringViewModel: RecurringTransactionViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }

    // Estados para el monto con formato
    var rawAmount by remember { mutableStateOf("") }
    var formattedAmount by remember { mutableStateOf("") }

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories by categoryViewModel.categories.collectAsState()
    var recurrenceType by remember { mutableStateOf(RecurrenceType.MONTHLY) }
    var dayOfMonth by remember { mutableStateOf(LocalDate.now().dayOfMonth.toString()) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var isActive by remember { mutableStateOf(true) }
    var screenTitle by remember { mutableStateOf("Añadir Recurrente") }
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale("es", "ES"))
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

    LaunchedEffect(key1 = recurringTransactionId) {
        if (recurringTransactionId != null) {
            screenTitle = "Editar Recurrente"
            recurringViewModel.getRecurringTransactionById(recurringTransactionId) { entity ->
                entity?.let {
                    title = it.title

                    // Configurar el monto con formato
                    val amountLong = it.amount.toLong()
                    rawAmount = amountLong.toString()
                    formattedAmount = numberFormatter.format(amountLong)

                    it.categoryId?.let { catId -> selectedCategory = categories.find { c -> c.id == catId } }
                    recurrenceType = it.recurrenceType
                    dayOfMonth = it.dayOfMonth.toString()
                    startDate = LocalDate.parse(it.startDate)
                    endDate = it.endDate?.let { LocalDate.parse(it) }
                    isActive = it.isActive
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = formattedAmount,
                onValueChange = { input ->
                    // Remover todo lo que no sea dígito
                    val digitsOnly = input.replace(",", "").filter { it.isDigit() }

                    if (digitsOnly.isEmpty()) {
                        rawAmount = ""
                        formattedAmount = ""
                    } else {
                        // Limitar a un máximo razonable (999,999,999,999)
                        val numericValue = digitsOnly.take(12).toLongOrNull() ?: 0L
                        rawAmount = numericValue.toString()

                        // Formatear con separadores de miles
                        formattedAmount = numberFormatter.format(numericValue)
                    }
                },
                label = { Text("Monto (PYG)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text("₲ ") },
                placeholder = { Text("0") }
            )

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Seleccionar Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Sin Categoría") }, onClick = {
                        selectedCategory = null
                        categoryDropdownExpanded = false
                    })
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = {
                            selectedCategory = category
                            categoryDropdownExpanded = false
                        })
                    }
                }
            }

            Text("Tipo de Recurrencia: Mensual", style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = dayOfMonth,
                onValueChange = { dayOfMonth = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Día del Mes (1-31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            DatePickerField(
                label = "Fecha de Inicio",
                selectedDate = startDate,
                onDateSelected = { startDate = it },
                dateFormat = dateFormat
            )

            DatePickerField(
                label = "Fecha de Fin (Opcional)",
                selectedDate = endDate,
                onDateSelected = { endDate = it },
                dateFormat = dateFormat,
                isOptional = true,
                onClearDate = { endDate = null}
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                Text("Activa")
            }

            Button(
                onClick = {
                    val finalAmount = rawAmount.toDoubleOrNull()
                    val finalDayOfMonth = dayOfMonth.toIntOrNull()

                    when {
                        title.isBlank() -> {
                            Toast.makeText(context, "El título no puede estar vacío.", Toast.LENGTH_SHORT).show()
                        }
                        rawAmount.isEmpty() -> {
                            Toast.makeText(context, "Por favor, ingrese un monto.", Toast.LENGTH_SHORT).show()
                        }
                        finalAmount == null || finalAmount <= 0 -> {
                            Toast.makeText(context, "El monto debe ser mayor a cero.", Toast.LENGTH_SHORT).show()
                        }
                        finalAmount > 999999999999 -> {
                            Toast.makeText(context, "El monto es demasiado grande.", Toast.LENGTH_SHORT).show()
                        }
                        finalDayOfMonth == null || finalDayOfMonth !in 1..31 -> {
                            Toast.makeText(context, "Día del mes inválido.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            recurringViewModel.addOrUpdateRecurringTransaction(
                                id = recurringTransactionId,
                                title = title,
                                amount = finalAmount,
                                categoryId = selectedCategory?.id,
                                recurrenceType = recurrenceType,
                                dayOfMonth = finalDayOfMonth,
                                startDate = startDate,
                                endDate = endDate,
                                isActive = isActive,
                                onSuccess = {
                                    Toast.makeText(context, "Guardado correctamente.", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (recurringTransactionId == null) "Añadir Recurrente" else "Actualizar Recurrente")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    dateFormat: DateTimeFormatter,
    isOptional: Boolean = false,
    onClearDate: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    selectedDate?.let {
        calendar.set(it.year, it.monthValue -1, it.dayOfMonth)
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            onDateSelected(LocalDate.of(year, month + 1, day))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = selectedDate?.format(dateFormat) ?: if(isOptional) "Sin fecha" else "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
        trailingIcon = {
            Row {
                if (isOptional && selectedDate != null && onClearDate != null) {
                    IconButton(onClick = {
                        onClearDate()
                        datePickerDialog.dismiss()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Limpiar fecha")
                    }
                }
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(Icons.Filled.DateRange, "Seleccionar fecha")
                }
            }
        }
    )
}