package com.uaa.gastos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import java.text.NumberFormat
import java.util.*

@Composable
fun SummaryCard(balance: Double) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val formattedBalance = numberFormat.format(balance)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Balance Total", style = MaterialTheme.typography.titleSmall)
            Text(
//                "$${"%.2f".format(balance)}",
                "PYG $formattedBalance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
