package com.example.myapplication.admin // Lub inny odpowiedni pakiet

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.CourseApiService // Upewnij się, że importy są poprawne
import com.example.myapplication.GenericResponse
import com.example.myapplication.RetrofitClient
import com.example.myapplication.UserCourseInfo
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

/**
 * @file SystemUsersScreen.kt
 *  Zawiera ViewModel oraz komponenty kompozycyjne dla ekranu zarządzania użytkownikami systemu.
 *
 * Ten plik definiuje logikę i interfejs użytkownika do przeglądania wszystkich użytkowników
 * w systemie oraz do usuwania użytkowników (z wyjątkiem administratorów).
 */

// --- ViewModel ---
/**
 *  ViewModel dla ekranu zarządzania użytkownikami systemu.
 *
 * Odpowiada za pobieranie listy wszystkich użytkowników systemu,
 * a także za obsługę operacji usuwania użytkowników. Zarządza stanami ładowania i błędów.
 *
 * @param context Kontekst aplikacji, wymagany do inicjalizacji RetrofitClient.
 */
class SystemUsersViewModel(private val context: Context) : ViewModel() {
    private val api: CourseApiService = RetrofitClient.getInstance(context)

    /**
     *  Lista użytkowników systemu.
     */
    var users by mutableStateOf<List<UserCourseInfo>>(emptyList())
        private set
    /**
     *  Stan ładowania danych. True, jeśli dane są aktualnie ładowane.
     */
    var isLoading by mutableStateOf(false)
        private set
    /**
     *  Komunikat o błędzie, jeśli wystąpił problem podczas operacji. Null, jeśli brak błędu.
     */
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadAllUsers()
    }

    /**
     *  Ładuje wszystkich użytkowników z systemu.
     *
     * Wykonuje asynchroniczne żądanie do API w celu pobrania listy użytkowników.
     * Aktualizuje stany [isLoading] i [error] w zależności od wyniku operacji.
     */
    fun loadAllUsers() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = api.getAllUsers()
                if (response.success) {
                    users = response.users
                } else {
                    error = "Nie udało się pobrać listy użytkowników systemu."
                }
            } catch (e: HttpException) {
                error = "Błąd HTTP: ${e.code()} - ${e.message()}"
            } catch (e: Exception) {
                error = "Błąd sieci: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     *  Usuwa użytkownika z systemu.
     *
     * Wysyła żądanie do API w celu usunięcia użytkownika o podanym identyfikatorze.
     * Po udanym usunięciu odświeża listę użytkowników.
     *
     * @param userId Identyfikator użytkownika do usunięcia.
     * @param onSuccess Callback wywoływany po udanym usunięciu, z komunikatem sukcesu.
     * @param onError Callback wywoływany w przypadku błędu, z komunikatem błędu.
     */
    fun deleteUserFromSystem(
        userId: Long,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: Response<GenericResponse> = api.deleteUserFromSystem(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess(response.body()?.message ?: "Użytkownik usunięty pomyślnie.")
                    loadAllUsers() // Odśwież listę po usunięciu
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.body()?.message
                    onError(errorBody ?: "Nie udało się usunąć użytkownika z systemu.")
                }
            } catch (e: HttpException) {
                onError("Błąd HTTP: ${e.code()} - ${e.message()}")
            } catch (e: Exception) {
                onError("Błąd sieci: ${e.localizedMessage}")
            }
        }
    }

    /**
     *  Fabryka do tworzenia instancji [SystemUsersViewModel].
     *
     * Wymagana do wstrzykiwania kontekstu do ViewModelu.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        /**
         *  Tworzy nową instancję ViewModelu.
         * @param modelClass Klasa ViewModelu do utworzenia.
         * @return Nowa instancja [SystemUsersViewModel].
         * @throws IllegalArgumentException Jeśli podana klasa ViewModelu nie jest [SystemUsersViewModel].
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SystemUsersViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SystemUsersViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// --- Screen Composable ---
/**
 *  Komponent kompozycyjny ekranu zarządzania użytkownikami systemu.
 *
 * Wyświetla listę wszystkich użytkowników systemu, umożliwiając administratorowi
 * ich usunięcie (z wyłączeniem innych administratorów). Obsługuje stany ładowania,
 * błędów i wyświetla dialog potwierdzenia usunięcia.
 *
 * @param navController Kontroler nawigacji do obsługi przejść wstecz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemUsersScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: SystemUsersViewModel = viewModel(factory = SystemUsersViewModel.Factory(context))
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var selectedUserIdForDeletion by remember { mutableStateOf<Long?>(null) }
    var selectedUsernameForDeletion by remember { mutableStateOf<String?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zarządzanie Użytkownikami Systemu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Błąd: ${viewModel.error}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadAllUsers() }) {
                        Text("Spróbuj ponownie")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.users) { user ->
                        UserSystemItem(
                            user = user,
                            onDeleteClick = {
                                selectedUserIdForDeletion = user.id
                                selectedUsernameForDeletion = user.username
                                showConfirmationDialog = true
                            }
                        )
                    }
                }
            }

            if (showConfirmationDialog && selectedUserIdForDeletion != null) {
                AlertDialog(
                    onDismissRequest = {
                        showConfirmationDialog = false
                        selectedUserIdForDeletion = null
                        selectedUsernameForDeletion = null
                    },
                    title = { Text("Potwierdzenie Usunięcia") },
                    text = { Text("Czy na pewno chcesz całkowicie usunąć użytkownika '${selectedUsernameForDeletion ?: "tego użytkownika"}' z systemu? Ta operacja jest nieodwracalna i usunie wszystkie dane powiązane z tym użytkownikiem.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                selectedUserIdForDeletion?.let { userId ->
                                    viewModel.deleteUserFromSystem(
                                        userId = userId,
                                        onSuccess = { message ->
                                            scope.launch { snackbarHostState.showSnackbar(message) }
                                        },
                                        onError = { errorMessage ->
                                            scope.launch { snackbarHostState.showSnackbar("Błąd: $errorMessage") }
                                        }
                                    )
                                }
                                showConfirmationDialog = false
                                selectedUserIdForDeletion = null
                                selectedUsernameForDeletion = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Tak, usuń")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showConfirmationDialog = false
                            selectedUserIdForDeletion = null
                            selectedUsernameForDeletion = null
                        }) {
                            Text("Anuluj")
                        }
                    }
                )
            }
        }
    }
}

// --- Helper Composable for User Item ---
/**
 *  Komponent kompozycyjny reprezentujący pojedynczego użytkownika w systemie.
 *
 * Wyświetla nazwę użytkownika, ID i rolę. Umożliwia usunięcie użytkownika
 * (z wyjątkiem roli "ADMIN").
 *
 * @param user Obiekt [UserCourseInfo] zawierający dane użytkownika.
 * @param onDeleteClick Lambda wywoływana, gdy użytkownik kliknie przycisk usuwania.
 */
@Composable
fun UserSystemItem(user: UserCourseInfo, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.titleMedium)
                Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall)
                Text("Rola: ${user.role}", style = MaterialTheme.typography.bodySmall)
            }
            if (user.role.uppercase() != "ADMIN") {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Usuń użytkownika z systemu",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}