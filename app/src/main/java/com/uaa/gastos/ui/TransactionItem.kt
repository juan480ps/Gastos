package com.uaa.gastos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale


@Composable
fun TransactionItem(title: String,
                    amount: Double,
                    date: String,
                    onDelete: () -> Unit) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val formattedAmount = numberFormat.format(amount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (amount < 0) Color(0xFFFFE5E5) else Color(0xFFE5FFE5)
        )
    ) {
        /*Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title)
                Text(text = date, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "$ $formattedAmount",
                style = MaterialTheme.typography.bodyMedium
            )
        }*/
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$ $formattedAmount",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}