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
    // Usar formato de moneda para PYG, sin decimales
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    val formattedBalance = numberFormat.format(balance)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), // Puedes ajustar la altura
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Color un poco diferente
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Balance Total",
                style = MaterialTheme.typography.titleMedium, // Un poco más grande
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                // "PYG $formattedBalance", // El formato de moneda ya incluye "₲"
                formattedBalance,
                style = MaterialTheme.typography.headlineMedium, // Un poco más grande
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}