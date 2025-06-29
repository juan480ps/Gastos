// ChartsScreen

package com.uaa.misgastosapp.ui

import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.uaa.misgastosapp.ui.viewmodel.ChartsViewModel
import com.uaa.misgastosapp.ui.viewmodel.PieChartData
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    MonthNavigator_(
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
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                ) {
                    MPAndroidPieChart(pieChartDataList = processedPieData)
                }

                Spacer(modifier = Modifier.height(16.dp))
                processedPieData.forEach { data ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .aspectRatio(1f)
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
    val chartTextColor = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.Black
    }
    AndroidView(
        factory = { ctx ->
            PieChart(ctx).apply {
                this.description.isEnabled = false
                this.isDrawHoleEnabled = true
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
                this.legend.isEnabled = false
                this.setUsePercentValues(true)
                this.setEntryLabelColor(chartTextColor.toArgb())
                this.setEntryLabelTextSize(12f)
            }
        },
        update = { pieChart ->
            val entries = ArrayList<PieEntry>()
            for (data in pieChartDataList) {
                entries.add(PieEntry(data.value, data.label))
            }
            val dataSet = PieDataSet(entries, "")
            dataSet.sliceSpace = 2f
            dataSet.selectionShift = 5f
            val colors = ArrayList<Int>()
            for (data in pieChartDataList) {
                colors.add(data.color.toArgb())
            }
            dataSet.colors = colors
            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(pieChart))
            data.setValueTextSize(11f)
            data.setValueTextColor(chartTextColor.toArgb())
            pieChart.data = data
            pieChart.invalidate()
            pieChart.animateY(1000)
        },
        modifier = Modifier.fillMaxSize()
    )
}

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