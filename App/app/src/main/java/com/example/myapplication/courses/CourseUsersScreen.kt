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

// Modele odpowiedzi API



// ViewModel do zarządzania użytkownikami kursu
class CourseUsersViewModel(
    context: Context,
    private val courseId: Long
) : ViewModel() {
    private val api = RetrofitClient.getInstance(context)

    var users by mutableStateOf<List<UserCourseInfo>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadUsers()
    }

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

    class Factory(
        private val context: Context,
        private val courseId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CourseUsersViewModel(context, courseId) as T
        }
    }
}

// Composable do wyświetlania i zarządzania użytkownikami
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
        Box(Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
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
                                        viewModel.removeUser(
                                            user.id,
                                            onSuccess = {
                                                scope.launch { snackbarHostState.showSnackbar("Usunięto użytkownika") }
                                            },
                                            onError = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        )
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
        }
    }
}
