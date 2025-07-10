==================================================================================
|                                                                                |
|          DIAGRAMA DE ARQUITECTURA DETALLADO - "MIS GASTOS APP"                   |
|                                                                                |
==================================================================================
|
|   +----------------------------------------------------------------------------+
|   |                                 UI LAYER                                   |
|   +----------------------------------------------------------------------------+
|   |
|   |  [ A. Screens / Composables (en com.uaa.misgastosapp.ui) ]
|   |    - HomeScreen.kt
|   |    - LoginScreen.kt, RegisterScreen.kt
|   |    - AddTransactionScreen.kt
|   |    - CategoriesListScreen.kt, AddCategoryScreen.kt
|   |    - ManageBudgetsScreen.kt
|   |    - ManageRecurringTransactionsScreen.kt, AddEditRecurringTransactionScreen.kt
|   |    - ChartsScreen.kt
|   |    - TransactionItem.kt, SummaryCard.kt (Componentes reutilizables)
|   |
|   |                                  ^     |
|   |           (Observa StateFlow)    |     | (Llama a funciones / Notifica eventos)
|   |                                  |     v
|   |
|   |  [ B. ViewModels (en com.uaa.misgastosapp.ui.viewmodel) ]
|   |    - AuthViewModel.kt
|   |    - TransactionViewModel.kt
|   |    - CategoryViewModel.kt
|   |    - BudgetViewModel.kt
|   |    - RecurringTransactionViewModel.kt
|   |    - ChartsViewModel.kt
|   |
|
|             ^                                     |
|             | (Expone datos como StateFlow)       | (Depende de / Llama a)
|             |                                     v
|
|   +----------------------------------------------------------------------------+
|   |                                 DATA LAYER                                 |
|   +----------------------------------------------------------------------------+
|   |
|   |  [ C. Repositories (en com.uaa.misgastosapp.data.repository) ]
|   |    - AuthRepository.kt
|   |    - TransactionRepository.kt
|   |    - CategoryRepository.kt
|   |    - BudgetRepository.kt
|   |    - RecurringTransactionRepository.kt
|   |
|   |      /                                            \
|   |     / (Accede a)                                     \ (Accede a)
|   |    v                                                  v
|   |
|   |  [ D. Local DataSource ]                            [ E. Remote DataSource ]
|   |  (Paquete: com.uaa.misgastosapp.data)               (Paquete: com.uaa.misgastosapp.network)
|   |
|   |    [ D1. DAOs (Interfaces) ]                          [ E1. API Service (Interfaz) ]
|   |      - UserDao.kt                                       - GastosApiService.kt (con Retrofit)
|   |      - TransactionDao.kt
|   |      - CategoryDao.kt                                           |
|   |      - BudgetDao.kt                                             | (Realiza llamadas HTTP/S)
|   |      - RecurringTransactionDao.kt                               |
|   |                                                                 v
|   |            |                                            [ E2. Backend Server ]
|   |            | (Operaciones CRUD)                         - (Flask/Python, Node.js, etc.)
|   |            v                                            - Aloja la lógica de negocio remota.
|   |
|   |    [ D2. Room Database ]
|   |      - AppDatabase.kt
|   |
|   |            ^
|   |            | (Define las tablas)
|   |            |
|   |
|   |    [ D3. Entities (Data Classes) ]
|   |      - UserEntity.kt
|   |      - TransactionEntity.kt
|   |      - CategoryEntity.kt
|   |      - BudgetEntity.kt
|   |      - RecurringTransactionEntity.kt
|   |
|
|   +----------------------------------------------------------------------------+
|   |                            MODEL & UTILS                                   |
|   +----------------------------------------------------------------------------+
|   |
|   |  [ F. Model (en com.uaa.misgastosapp.model) ]
|   |    - Clases de datos limpias usadas por la UI y los ViewModels.
|   |    - Transaction.kt, Category.kt, Budget.kt, RecurringTransaction.kt
|   |
|   |  [ G. Utils & Support ]
|   |    - AppNavigation.kt: Define el grafo de navegación.
|   |    - NetworkModule.kt: Configura y provee la instancia de Retrofit.
|   |    - SecureSessionManager.kt: Gestiona la sesión de usuario de forma segura.
|   |    - Result.kt: Sealed class para manejar estados de operaciones.
|   |
+--------------------------------------------------------------------------------->