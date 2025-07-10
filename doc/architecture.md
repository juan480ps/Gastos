graph TD
    subgraph UI Layer
        direction TB
        S[Screens / Composables<br>(HomeScreen, ChartsScreen, etc.)] -- User Events --> VM[ViewModels<br>(TransactionViewModel, AuthViewModel, etc.)]
        VM -- StateFlow (State) --> S
    end

    subgraph Data Layer
        direction TB
        R[Repositories<br>(TransactionRepository, AuthRepository, etc.)]
        
        subgraph Local DataSource
            direction TB
            D[DAOs<br>(TransactionDao, CategoryDao, etc.)]
            DB[(<img src='https://www.vectorlogo.zone/logos/sqlite/sqlite-icon.svg' width='20' /> Room DB<br>AppDatabase)]
            D -- CRUD --> DB
        end
        
        subgraph Remote DataSource
            direction TB
            A[API Service<br>(Retrofit - GastosApiService)]
            BE[(<img src='https://www.vectorlogo.zone/logos/python/python-icon.svg' width='20' /> Backend<br>Server)]
            A -- HTTP Requests --> BE
        end

        R -- Accesses --> D
        R -- Accesses --> A
    end

    VM -- Calls Methods --> R

    style S fill:#cde4ff,stroke:#6495ED,stroke-width:2px
    style VM fill:#cde4ff,stroke:#6495ED,stroke-width:2px
    
    style R fill:#d5e8d4,stroke:#82b366,stroke-width:2px
    style D fill:#f8cecc,stroke:#b85450,stroke-width:2px
    style DB fill:#f8cecc,stroke:#b85450,stroke-width:2px
    style A fill:#fff2cc,stroke:#d6b656,stroke-width:2px
    style BE fill:#fff2cc,stroke:#d6b656,stroke-width:2px