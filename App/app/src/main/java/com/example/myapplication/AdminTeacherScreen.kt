package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.admin.SystemUsersViewModel
import com.example.myapplication.courses.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserCourseInfo>>(emptyList()) }
    var showUsers by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var courseGroups by remember { mutableStateOf<List<CourseGroup>>(emptyList()) }

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var selectedUserIdForDeletion by remember { mutableStateOf<Long?>(null) }
    var selectedUsernameForDeletion by remember { mutableStateOf<String?>(null) }

    val viewModel: SystemUsersViewModel = viewModel(factory = SystemUsersViewModel.Factory(context))

    fun loadCourseGroups() {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val response = api.getCourseGroups()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        courseGroups = response.body() ?: emptyList()
                    } else {
                        snackbarHostState.showSnackbar("Błąd pobierania grup: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Błąd: ${e.message}")
                }
            }
        }
    }

    fun loadUsers() {
        scope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                val api = RetrofitClient.getInstance(context)
                val response = api.getAllUsers()
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        users = response.users
                    } else {
                        snackbarHostState.showSnackbar("Błąd pobierania użytkowników")
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Błąd: ${e.message}")
                    isLoading = false
                }
            }
        }
    }

    fun promoteToTeacher(userId: Long, username: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val response = api.promoteToTeacher(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        snackbarHostState.showSnackbar("Użytkownik $username jest teraz nauczycielem")
                        loadUsers()
                    } else {
                        snackbarHostState.showSnackbar(response.body()?.message ?: "Błąd promocji")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Błąd: ${e.message}")
                }
            }
        }
    }

    fun demoteToStudent(userId: Long, username: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val response = api.demoteToStudent(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        snackbarHostState.showSnackbar("Użytkownik $username jest teraz studentem")
                        loadUsers()
                    } else {
                        snackbarHostState.showSnackbar(response.body()?.message ?: "Błąd degradacji")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Błąd: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCourseGroups()
        loadUsers()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (showUsers) "Zarządzanie użytkownikami" else "Moje grupy kursów") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showUsers) showUsers = false else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    if (!showUsers) {
                        TextButton(onClick = { showUsers = true }) {
                            Text("Zarządzaj użytkownikami")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (showUsers) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(users) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Użytkownik: ${user.username}", style = MaterialTheme.typography.titleMedium)
                                    Text("Rola: ${user.role}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (user.role == "STUDENT") {
                                            Button(onClick = { promoteToTeacher(user.id, user.username) }) {
                                                Text("Promuj na nauczyciela")
                                            }
                                        } else if (user.role == "TEACHER") {
                                            Button(
                                                onClick = { demoteToStudent(user.id, user.username) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Degraduj do studenta", color = MaterialTheme.colorScheme.onError)
                                            }
                                        }
                                        if (user.role != "ADMIN") {
                                            IconButton(onClick = {
                                                selectedUserIdForDeletion = user.id
                                                selectedUsernameForDeletion = user.username
                                                showDeleteConfirmationDialog = true
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(courseGroups) { group ->
                        CourseGroupCard(
                            group = group,
                            navController = navController,
                            onAddCourseClick = {},
                            onDuplicateCourseClick = {},
                            onDeleteCourseClick = {},
                            onDeleteGroupClick = {}
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmationDialog && selectedUserIdForDeletion != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmationDialog = false
                    selectedUserIdForDeletion = null
                    selectedUsernameForDeletion = null
                },
                title = { Text("Potwierdzenie usunięcia") },
                text = {
                    Text("Czy na pewno chcesz usunąć użytkownika '${selectedUsernameForDeletion}'? Operacja jest nieodwracalna.")
                },
                confirmButton = {
                    Button(onClick = {
                        selectedUserIdForDeletion?.let { userId ->
                            viewModel.deleteUserFromSystem(
                                userId = userId,
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar(it) }
                                    loadUsers()
                                },
                                onError = {
                                    scope.launch { snackbarHostState.showSnackbar("Błąd: $it") }
                                }
                            )
                        }
                        showDeleteConfirmationDialog = false
                        selectedUserIdForDeletion = null
                        selectedUsernameForDeletion = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Tak, usuń")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirmationDialog = false
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