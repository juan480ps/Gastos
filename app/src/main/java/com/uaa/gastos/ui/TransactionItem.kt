package com.uaa.gastos.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uaa.gastos.model.Transaction
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val formattedAmount = numberFormat.format(transaction.amount)
    var expanded by remember { mutableStateOf(false) }

    val isIncome = transaction.amount >= 0

    val incomeColor = Color(0xFF1B5E20)
    val expenseColor = Color(0xFFB71C1C)
    val amountColor = if (isIncome) incomeColor else expenseColor

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrowRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isIncome) Color(0xFFE5FFE5) else Color(0xFFFFE5E5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = (if (isIncome) "Ingreso" else "Egreso"), // transaction.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = amountColor
                    )
                    Text(
                        text = "PYG $formattedAmount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = amountColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expandir",
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Descripción: ${transaction.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = amountColor
                    )
                    Text(
                        text = "Fecha: ${transaction.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = amountColor
                    )
                }
            }
        }
    }
}
