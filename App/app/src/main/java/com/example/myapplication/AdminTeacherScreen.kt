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
import com.example.myapplication.courses.Course
import com.example.myapplication.admin.SystemUsersViewModel
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
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var users by remember { mutableStateOf<List<UserCourseInfo>>(emptyList()) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var selectedValidity by remember { mutableStateOf("1_WEEK") }
    var expanded by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var showUsers by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var selectedUserIdForDeletion by remember { mutableStateOf<Long?>(null) }
    var selectedUsernameForDeletion by remember { mutableStateOf<String?>(null) }

    // Initialize ViewModel for user deletion
    val viewModel: SystemUsersViewModel = viewModel(factory = SystemUsersViewModel.Factory(context))

    fun loadCourses() {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val fetched = api.getAllCourses()
                withContext(Dispatchers.Main) {
                    courses = fetched
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Błąd pobierania kursów: ${e.message}")
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

    fun formatExpiresAt(isoDateTime: String?): String {
        if (isoDateTime == null) return "Nieznana data"
        return try {
            val dateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val formatter = DateTimeFormatter
                .ofPattern("d MMMM yyyy, HH:mm")
                .withLocale(Locale("pl", "PL"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            isoDateTime // Fallback na oryginalny ciąg w razie błędu
        }
    }

    fun generateStudentCode() {
        isGenerating = true
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val request = GenerateCodeRequest(validity = selectedValidity)
                val response = api.generateStudentCode(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        generatedCode = response.body()?.code
                        expiresAt = response.body()?.expiresAt
                    } else {
                        snackbarHostState.showSnackbar(
                            response.body()?.message ?: "Błąd generowania kodu"
                        )
                    }
                    isGenerating = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Błąd: ${e.message}")
                    isGenerating = false
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
        loadCourses()
        loadUsers()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!showUsers) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("add_course") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Dodaj kurs") },
                    text = { Text("Dodaj kurs") }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(if (showUsers) "Zarządzanie użytkownikami" else "Moje kursy") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showUsers) {
                            showUsers = false
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    if (!showUsers) {
                        TextButton(onClick = { showCodeDialog = true }) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountBox,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generuj kod rejestracyjny")
                            }
                        }
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
                                        if (user.role != "ADMIN") {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    selectedUserIdForDeletion = user.id
                                                    selectedUsernameForDeletion = user.username
                                                    showDeleteConfirmationDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
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
            } else {
                CourseListScreen(
                    navController = navController,
                    courses = courses,
                    onCourseClick = { course ->
                        navController.navigate("course_details/${course.id}")
                    },
                    onDeleteCourseClick = { course ->
                        scope.launch {
                            try {
                                val api = RetrofitClient.getInstance(context)
                                val response = api.deleteCourse(course.id)
                                if (response.isSuccessful) {
                                    snackbarHostState.showSnackbar("Kurs usunięty")
                                    loadCourses()
                                } else {
                                    snackbarHostState.showSnackbar("Błąd usuwania kursu: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Błąd: ${e.message}")
                            }
                        }
                    },
                    onViewStatsClick = { course ->
                        navController.navigate("quiz_stats/${course.id}")
                    }
                )
            }
        }

        if (showCodeDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCodeDialog = false
                    generatedCode = null
                    expiresAt = null
                },
                title = { Text("Generuj kod rejestracyjny") },
                text = {
                    Column {
                        if (generatedCode == null) {
                            Text("Wybierz czas ważności kodu:")
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = when (selectedValidity) {
                                        "1_HOUR" -> "1 godzina"
                                        "2_HOURS" -> "2 godziny"
                                        "1_DAY" -> "1 dzień"
                                        else -> "1 tydzień"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("1 godzina") },
                                        onClick = {
                                            selectedValidity = "1_HOUR"
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("2 godziny") },
                                        onClick = {
                                            selectedValidity = "2_HOURS"
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("1 dzień") },
                                        onClick = {
                                            selectedValidity = "1_DAY"
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("1 tydzień") },
                                        onClick = {
                                            selectedValidity = "1_WEEK"
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        } else {
                            Column {
                                Text(
                                    text = "Wygenerowany kod:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kod: $generatedCode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Text(
                                    text = "Ważny do: ${formatExpiresAt(expiresAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    if (generatedCode == null) {
                        Button(
                            onClick = { generateStudentCode() },
                            enabled = !isGenerating
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Generuj")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                showCodeDialog = false
                                generatedCode = null
                                expiresAt = null
                            }
                        ) {
                            Text("Zamknij")
                        }
                    }
                },
                dismissButton = {
                    if (generatedCode == null) {
                        TextButton(
                            onClick = {
                                showCodeDialog = false
                                generatedCode = null
                                expiresAt = null
                            }
                        ) {
                            Text("Anuluj")
                        }
                    }
                }
            )
        }

        if (showDeleteConfirmationDialog && selectedUserIdForDeletion != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmationDialog = false
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
                                        loadUsers()
                                    },
                                    onError = { errorMessage ->
                                        scope.launch { snackbarHostState.showSnackbar("Błąd: $errorMessage") }
                                    }
                                )
                            }
                            showDeleteConfirmationDialog = false
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