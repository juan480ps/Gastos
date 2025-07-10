// CategoriesListScreen

package com.uaa.misgastosapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.utils.Result

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesListScreen(navController: NavController, categoryViewModel: CategoryViewModel = viewModel()) {
    val categories by categoryViewModel.categories.collectAsState()

    val operationStatus by categoryViewModel.operationStatus.collectAsState()
    val context = LocalContext.current
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                categoryViewModel.clearOperationStatus()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                categoryViewModel.clearOperationStatus()
            }
            is Result.Loading -> {

            }
            null -> {

            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías de Gastos") },
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Routes.ADD_CATEGORY) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Categoría")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            if (categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay categorías. ¡Añade una!")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        CategoryListItem(
                            category = category,
                            onDelete = {

                                categoryToDelete = category
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                categoryToDelete = null
            },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar la categoría '${categoryToDelete?.name}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { categoryViewModel.deleteCategory(it) }
                        showDeleteDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CategoryListItem(category: Category, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar Categoría")
            }
        }
    }
}