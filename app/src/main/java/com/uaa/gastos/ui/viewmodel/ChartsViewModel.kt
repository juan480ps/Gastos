package com.uaa.gastos.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color // Usamos el Color de Compose
// No necesitamos toArgb aquí, se usará en el Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Ya no necesitamos los imports de Vico para el pie chart aquí
// import com.patrykandpatrick.vico.core.entry.ChartEntry
// import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
// import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.uaa.gastos.data.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch // launch sigue siendo útil para otras cosas si las hubiera
import java.time.YearMonth
import java.time.format.DateTimeFormatter
// kotlin.random.Random no se usa si los colores son predefinidos

// Helper data class para datos del gráfico de pastel. Esta sigue siendo útil.
data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color // androidx.compose.ui.graphics.Color
)

@RequiresApi(Build.VERSION_CODES.O)
class ChartsViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getInstance(application).transactionDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()

    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    private val currentMonthYearString: Flow<String> = _currentMonthYear.map {
        it.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    fun setCurrentMonthYear(yearMonth: YearMonth) {
        _currentMonthYear.value = yearMonth
    }

    // Ya NO necesitamos el Vico ModelProducer para el gráfico de pastel
    // val expensePieChartModelProducer = ChartEntryModelProducer()

    // Datos procesados para el gráfico de pastel. Esto sigue siendo correcto y útil.
    @OptIn(ExperimentalCoroutinesApi::class)
    val processedExpensePieData: StateFlow<List<PieChartData>> = currentMonthYearString.flatMapLatest { monthStr ->
        combine(
            transactionDao.getAll(),
            categoryDao.getAll()
        ) { transactions, categories ->
            val expensesInMonth = transactions
                .filter { it.date.startsWith(monthStr) && it.amount < 0 }

            if (expensesInMonth.isEmpty()) {
                return@combine emptyList<PieChartData>()
            }

            val expensesByCategory = expensesInMonth
                .groupBy { it.categoryId }
                .mapValues { entry -> entry.value.sumOf { it.amount * -1 } } // Sumar como positivo

            val chartDataList = mutableListOf<PieChartData>()
            var colorIndex = 0
            // Estos colores son androidx.compose.ui.graphics.Color
            val predefinedColors = listOf(
                Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
                Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
                Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548)
            )

            expensesByCategory.entries
                .sortedByDescending { it.value } // Opcional: ordenar por valor
                .forEach { (categoryId, totalAmount) ->
                    val categoryName = categoryId?.let { id ->
                        categories.find { it.id == id }?.name
                    } ?: "Sin Categoría"
                    chartDataList.add(
                        PieChartData(
                            label = categoryName,
                            value = totalAmount.toFloat(),
                            color = predefinedColors[colorIndex % predefinedColors.size]
                        )
                    )
                    colorIndex++
                }
            // La ordenación se puede hacer antes del forEach para asignar colores consistentemente si eso se desea.
            // O dejarla como estaba: chartDataList.sortedByDescending { it.value } al final
            chartDataList // Devuelve la lista ya construida
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    // El bloque init que actualizaba el Vico ModelProducer ya NO es necesario para el gráfico de pastel.
    /*
    init {
        viewModelScope.launch {
            processedExpensePieData.collect { pieDataList ->
                if (pieDataList.isNotEmpty()) {
                    val entries = pieDataList.mapIndexed { index, data ->
                        FloatEntry(x = index.toFloat(), y = data.value)
                    }
                    expensePieChartModelProducer.setEntries(entries)
                } else {
                    expensePieChartModelProducer.setEntries(emptyList<ChartEntry>())
                }
            }
        }
    }
    */
}