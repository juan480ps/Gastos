package com.uaa.gastos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale


@Composable
fun TransactionItem(title: String, amount: Double, date: String) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val formattedAmount = numberFormat.format(amount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (amount < 0) Color(0xFFFFE5E5) else Color(0xFFE5FFE5) // ✅ Usa Double
        )
    ) {
        Row(
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
        }
    }
}

/*@Composable
fun TransactionItem(title: String, amount: String, date: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (amount < 0) Color(0xFFFFE5E5) else Color(0xFFE5FFE5)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                text = (if (amount >= 0) "+" else "") + "$${"%.2f".format(amount)}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (amount < 0) Color.Red else Color(0xFF2E7D32)
            )
        }
    }
}*/


/*@Composable
fun TransactionItem(tx: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tx.amount < 0) Color(0xFFFFE5E5) else Color(0xFFE5FFE5)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(tx.title, style = MaterialTheme.typography.bodyLarge)
                Text(tx.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                (if (tx.amount > 0) "+" else "") + "$${"%.2f".format(tx.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (tx.amount < 0) Color.Red else Color(0xFF2E7D32)
            )
        }
    }
}*/
