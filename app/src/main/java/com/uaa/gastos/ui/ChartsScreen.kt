package com.uaa.gastos.ui

import android.graphics.Color as AndroidColor // Para MPAndroidChart
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Color de Compose para la leyenda si la haces manual
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
// MPAndroidChart Imports
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
// Tus imports
import com.uaa.gastos.ui.viewmodel.ChartsViewModel
import com.uaa.gastos.ui.viewmodel.PieChartData // Tu data class PieChartData
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.* // Para Locale
import androidx.compose.foundation.isSystemInDarkTheme // Importante para una alternativa a isLight
import androidx.compose.material3.MaterialTheme // Asegúrate de tener este import

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    navController: NavController,
    chartsViewModel: ChartsViewModel = viewModel()
) {
    val currentYearMonth by chartsViewModel.currentMonthYear.collectAsState()
    val processedPieData by chartsViewModel.processedExpensePieData.collectAsState(initial = emptyList())

    val monthDisplayFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES")) }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen Gráfico") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    MonthNavigator_( // Reutilizamos el MonthNavigator
                        currentYearMonth = currentYearMonth,
                        onPreviousMonth = { chartsViewModel.setCurrentMonthYear(currentYearMonth.minusMonths(1)) },
                        onNextMonth = { chartsViewModel.setCurrentMonthYear(currentYearMonth.plusMonths(1)) }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gastos por Categoría: ${currentYearMonth.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (processedPieData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay datos de gastos para mostrar en este período.")
                }
            } else {
                // Contenedor para el gráfico de MPAndroidChart
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp) // Ajusta la altura como necesites
                ) {
                    MPAndroidPieChart(pieChartDataList = processedPieData)
                }

                // Leyenda manual (opcional, MPAndroidChart puede tener su propia leyenda)
                Spacer(modifier = Modifier.height(16.dp))
                processedPieData.forEach { data ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .aspectRatio(1f) // Asegura que sea un círculo si usas CircleShape
                                .background(data.color, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${data.label}: ${currencyFormat.format(data.value)} (${String.format("%.1f", (data.value / processedPieData.sumOf { it.value.toDouble() } * 100))}%)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MPAndroidPieChart(pieChartDataList: List<PieChartData>) {
    val context = LocalContext.current
    val currentColorScheme = MaterialTheme.colorScheme
    val chartTextColor = if (isSystemInDarkTheme()) {
        Color.White // Si el tema es oscuro, el texto es blanco
    } else {
        Color.Black // Si el tema es claro, el texto es negro
    }
    AndroidView(
        factory = { ctx ->
            PieChart(ctx).apply {
                // Configuraciones iniciales del gráfico
                this.description.isEnabled = false
                this.isDrawHoleEnabled = true // Para hacerlo un Donut Chart
                this.setHoleColor(AndroidColor.TRANSPARENT)
                this.setTransparentCircleColor(AndroidColor.TRANSPARENT)
                this.setTransparentCircleAlpha(0)
                this.holeRadius = 50f
                this.transparentCircleRadius = 55f
                this.setDrawCenterText(true)
                this.centerText = "Gastos"
                this.setCenterTextSize(16f)
                this.setCenterTextColor(chartTextColor.toArgb())


                this.rotationAngle = 0f
                this.isRotationEnabled = true
                this.isHighlightPerTapEnabled = true

                // Leyenda de MPAndroidChart (puedes configurarla o deshabilitarla si haces una manual en Compose)
                this.legend.isEnabled = false // Deshabilitada para usar leyenda manual en Compose
                // val l = this.legend
                // l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
                // l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                // l.orientation = Legend.LegendOrientation.VERTICAL
                // l.setDrawInside(false)
                // l.xEntrySpace = 7f
                // l.yEntrySpace = 0f
                // l.yOffset = 0f

                // Formateador para los valores en el gráfico (porcentajes)
                this.setUsePercentValues(true)
                this.setEntryLabelColor(chartTextColor.toArgb())
                this.setEntryLabelTextSize(12f)
            }
        },
        update = { pieChart ->
            // Actualizar los datos del gráfico
            val entries = ArrayList<PieEntry>()
            for (data in pieChartDataList) {
                entries.add(PieEntry(data.value, data.label))
            }

            val dataSet = PieDataSet(entries, "") // El segundo argumento es la etiqueta del dataset, no necesaria para pie
            dataSet.sliceSpace = 2f // Espacio entre slices
            dataSet.selectionShift = 5f

            // Colores (usa los colores que ya tienes en PieChartData)
            val colors = ArrayList<Int>()
            for (data in pieChartDataList) {
                colors.add(data.color.toArgb()) // Convertir Color de Compose a Int ARGB
            }
            // O usar plantillas de color de MPAndroidChart:
            // colors.addAll(ColorTemplate.MATERIAL_COLORS.toList())
            // colors.addAll(ColorTemplate.VORDIPLOM_COLORS.toList())
            dataSet.colors = colors

            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(pieChart)) // Usar pieChart como contexto para el formateador
            data.setValueTextSize(11f)
            data.setValueTextColor(chartTextColor.toArgb())


            pieChart.data = data
            pieChart.invalidate() // Redibujar el gráfico
            pieChart.animateY(1000) // Animación
        },
        modifier = Modifier.fillMaxSize()
    )
}

// Reutilizar MonthNavigator_
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthNavigator_(
    currentYearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousMonth) {
            Text("<", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onNextMonth) {
            Text(">", style = MaterialTheme.typography.titleMedium)
        }
    }
}