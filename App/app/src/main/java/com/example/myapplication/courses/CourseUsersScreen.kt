package com.example.myapplication.courses

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
import com.example.myapplication.GenericResponse
import com.example.myapplication.RetrofitClient
import com.example.myapplication.UserCourseInfo
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

/**
 * @file CourseUsersScreen.kt
 * Ten plik zawiera ViewModel [CourseUsersViewModel] do zarządzania użytkownikami kursu
 * oraz kompozycyjną funkcję ekranu [CourseUsersScreen] do wyświetlania i usuwania użytkowników z kursu.
 */

/**
 * ViewModel do zarządzania użytkownikami kursu.
 * Udostępnia funkcje do ładowania listy użytkowników kursu oraz ich usuwania.
 *
 * @param context Kontekst aplikacji, wymagany do inicjalizacji RetrofitClient.
 * @param courseId Identyfikator kursu, dla którego zarządzani są użytkownicy.
 */
class CourseUsersViewModel(
    context: Context,
    private val courseId: Long
) : ViewModel() {
    /** Instancja RetrofitClient do komunikacji z API.*/
    private val api = RetrofitClient.getInstance(context)

    /** Lista użytkowników przypisanych do kursu.*/
    var users by mutableStateOf<List<UserCourseInfo>>(emptyList())
        private set
    /** Stan ładowania danych. True, jeśli dane są aktualnie ładowane, w przeciwnym razie false.*/
    var isLoading by mutableStateOf(false)
        private set
    /** Wiadomość o błędzie, jeśli wystąpi problem podczas ładowania danych. Null, jeśli brak błędów.*/
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadUsers()
    }

    /**
     * Ładuje listę użytkowników przypisanych do danego kursu z API.
     * Obsługuje stany ładowania i błędy.
     */
    fun loadUsers() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val resp = api.getCourseUsers(courseId)
                if (resp.success) {
                    users = resp.users
                } else {
                    error = "Błąd serwera"
                }
            } catch (e: HttpException) {
                error = "Błąd HTTP: ${e.code()}"
            } catch (e: Exception) {
                error = "Błąd sieci: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Usuwa użytkownika z kursu.
     * Po pomyślnym usunięciu odświeża listę użytkowników.
     *
     * @param userId Identyfikator użytkownika do usunięcia.
     * @param onSuccess Funkcja wywoływana po pomyślnym usunięciu użytkownika.
     * @param onError Funkcja wywoływana w przypadku błędu, z komunikatem o błędzie.
     */
    fun removeUser(
        userId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res: Response<GenericResponse> = api.removeUserFromCourse(courseId, userId)
                if (res.isSuccessful && res.body()?.success == true) {
                    onSuccess()
                    loadUsers()
                } else {
                    onError(res.body()?.message ?: "Nie udało się usunąć użytkownika")
                }
            } catch (e: HttpException) {
                onError("Błąd HTTP: ${e.code()}")
            } catch (e: Exception) {
                onError("Błąd sieci: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Fabryka do tworzenia instancji [CourseUsersViewModel].
     * Wymagana do przekazywania argumentów do ViewModelu.
     */
    class Factory(
        private val context: Context,
        private val courseId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseUsersViewModel(context, courseId) as T
        }
    }
}

/**
 * Kompozycyjna funkcja ekranu zarządzania użytkownikami kursu.
 * Wyświetla listę użytkowników kursu i umożliwia ich usuwanie.
 *
 * @param navController Kontroler nawigacji do obsługi przejść między ekranami.
 * @param courseId Identyfikator kursu, dla którego wyświetlani są użytkownicy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseUsersScreen(
    navController: NavHostController,
    courseId: Long
) {
    val context = LocalContext.current
    val viewModel: CourseUsersViewModel = viewModel(
        factory = CourseUsersViewModel.Factory(context, courseId)
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    /** Stan do zarządzania widocznością okna dialogowego potwierdzenia.*/
    var showConfirmationDialog by remember { mutableStateOf(false) }
    /** Identyfikator użytkownika do usunięcia, null jeśli żaden nie jest wybrany.*/
    var userIdToRemove by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Użytkownicy kursu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            /** Obsługa stanów ładowania i błędów.*/
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                viewModel.error != null -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(viewModel.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadUsers() }) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.users) { user ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(user.username, style = MaterialTheme.typography.titleMedium)
                                        Text("Rola: ${user.role}", style = MaterialTheme.typography.bodySmall)
                                        Text("Dołączył: ${user.joinedAt}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = {
                                        userIdToRemove = user.id
                                        showConfirmationDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Usuń użytkownika",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            /** Okno dialogowe potwierdzenia usunięcia użytkownika.*/
            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showConfirmationDialog = false
                        userIdToRemove = null
                    },
                    title = { Text("Potwierdzenie usunięcia") },
                    text = { Text("Czy na pewno chcesz usunąć tego użytkownika z kursu?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                userIdToRemove?.let { id ->
                                    viewModel.removeUser(
                                        userId = id,
                                        onSuccess = {
                                            scope.launch { snackbarHostState.showSnackbar("Użytkownik został usunięty.") }
                                        },
                                        onError = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar("Błąd: $msg") }
                                        }
                                    )
                                }
                                showConfirmationDialog = false
                                userIdToRemove = null
                            }
                        ) {
                            Text("Tak, usuń")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showConfirmationDialog = false
                                userIdToRemove = null
                            }
                        ) {
                            Text("Anuluj")
                        }
                    }
                )
            }
        }
    }
}