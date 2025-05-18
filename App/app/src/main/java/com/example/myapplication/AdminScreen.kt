package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserCourseInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

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
                        loadUsers() // Odśwież listę
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
                        loadUsers() // Odśwież listę
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
        loadUsers()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Panel administracyjny") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(users) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Użytkownik: ${user.username}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Rola: ${user.role}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (user.role == "STUDENT") {
                                        Button(
                                            onClick = { promoteToTeacher(user.id, user.username) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Promuj na nauczyciela")
                                        }
                                    } else if (user.role == "TEACHER") {
                                        Button(
                                            onClick = { demoteToStudent(user.id, user.username) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(
                                                "Degraduj do studenta",
                                                color = MaterialTheme.colorScheme.onError
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
}