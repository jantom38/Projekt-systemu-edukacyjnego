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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
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
import java.util.UUID

/**
 * @file AdminTeacherScreen.kt
 *  This file contains the Composable function for the Admin/Teacher screen, allowing management of users, course groups, and registration codes.
 */

/**
 *  Composable function for the Admin and Teacher functionalities screen.
 * This screen allows administrators and teachers to manage users (promote/demote, delete),
 * manage course groups (create, delete, duplicate courses within groups), and generate student registration codes.
 * @param navController The NavHostController used for navigating between screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserCourseInfo>>(emptyList()) }
    var showUsers by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var selectedValidity by remember { mutableStateOf("1_WEEK") }
    var expanded by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var courseGroups by remember { mutableStateOf<List<CourseGroup>>(emptyList()) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var selectedUserIdForDeletion by remember { mutableStateOf<Long?>(null) }
    var selectedUsernameForDeletion by remember { mutableStateOf<String?>(null) }
    var showDuplicateCourseDialog by remember { mutableStateOf<Pair<Long, Course>?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf<CourseGroup?>(null) }

    val viewModel: SystemUsersViewModel = viewModel(factory = SystemUsersViewModel.Factory(context))

    /**
     *  Formats an ISO date-time string into a more readable localized format.
     * @param isoDateTime The ISO date-time string to format.
     * @return A formatted date-time string, or "Nieznana data" if the input is null or invalid.
     */
    fun formatExpiresAt(isoDateTime: String?): String {
        if (isoDateTime == null) return "Nieznana data"
        return try {
            val dateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val formatter = DateTimeFormatter
                .ofPattern("d MMMM yyyy, HH:mm")
                .withLocale(Locale("pl", "PL"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            isoDateTime
        }
    }

    /**
     *  Generates a student registration code by calling the API.
     * Updates the [generatedCode] and [expiresAt] states upon success, or shows a Snackbar on error.
     */
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

    /**
     *  Loads the list of course groups from the API.
     * Updates the [courseGroups] state upon success, or shows a Snackbar on error.
     */
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

    /**
     *  Loads the list of all users from the API.
     * Updates the [users] and [isLoading] states based on the API response.
     */
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

    /**
     *  Promotes a user to a teacher role via the API.
     * Reloads the user list on success, or shows a Snackbar on error.
     * @param userId The ID of the user to promote.
     * @param username The username of the user to promote (for display purposes).
     */
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

    /**
     *  Demotes a user to a student role via the API.
     * Reloads the user list on success, or shows a Snackbar on error.
     * @param userId The ID of the user to demote.
     * @param username The username of the user to demote (for display purposes).
     */
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
                        TextButton(onClick = { showCodeDialog = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
        },
        floatingActionButton = {
            if (!showUsers) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nowa grupa") },
                    text = { Text("Nowa grupa") }
                )
            }
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
                            onAddCourseClick = { navController.navigate("add_course/${group.id}") },
                            onDuplicateCourseClick = { course -> showDuplicateCourseDialog = Pair(group.id, course) },
                            onDeleteCourseClick = { course ->
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.getInstance(context).deleteCourse(course.id)
                                        if (response.isSuccessful) {
                                            snackbarHostState.showSnackbar("Kurs usunięty")
                                            loadCourseGroups()
                                        } else {
                                            snackbarHostState.showSnackbar("Błąd usuwania kursu: ${response.code()}")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Błąd: ${e.message}")
                                    }
                                }
                            },
                            onDeleteGroupClick = { showDeleteGroupDialog = group }
                        )
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateCourseGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, description ->
                scope.launch {
                    try {
                        val response = RetrofitClient.getInstance(context)
                            .createCourseGroup(mapOf("name" to name, "description" to description))
                        if (response.isSuccessful) {
                            snackbarHostState.showSnackbar("Grupa utworzona")
                            showCreateGroupDialog = false
                            loadCourseGroups()
                        } else {
                            snackbarHostState.showSnackbar("Błąd: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Błąd: ${e.message}")
                    }
                }
            }
        )
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
            title = { Text("Potwierdzenie usunięcia") },
            text = { Text("Czy na pewno chcesz usunąć użytkownika '${selectedUsernameForDeletion}'? Operacja jest nieodwracalna.") },
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

    showDuplicateCourseDialog?.let { (groupId, course) ->
        DuplicateCourseDialog(
            originalCourseName = course.courseName,
            onDismiss = { showDuplicateCourseDialog = null },
            onDuplicate = { newName, newKey ->
                scope.launch {
                    try {
                        val request = mapOf("newCourseName" to newName, "newAccessKey" to newKey)
                        val response = RetrofitClient.getInstance(context).duplicateCourse(
                            groupId = groupId,
                            courseId = course.id,
                            request = request
                        )
                        if (response.isSuccessful && response.body()?.success == true) {
                            snackbarHostState.showSnackbar(response.body()?.message ?: "Kurs zduplikowany")
                            loadCourseGroups()
                        } else {
                            snackbarHostState.showSnackbar(response.body()?.message ?: "Błąd duplikowania")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Błąd: ${e.message}")
                    } finally {
                        showDuplicateCourseDialog = null
                    }
                }
            }
        )
    }

    if (showDeleteGroupDialog != null) {
        val groupToDelete = showDeleteGroupDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = null },
            title = { Text("Potwierdź usunięcie") },
            text = { Text("Czy na pewno chcesz usunąć grupę '${groupToDelete.name}' wraz z wszystkimi kursami w niej zawartymi?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val response = RetrofitClient.getInstance(context).deleteCourseGroup(groupToDelete.id)
                                if (response.isSuccessful && response.body()?.success == true) {
                                    snackbarHostState.showSnackbar(response.body()!!.message)
                                    loadCourseGroups()
                                } else {
                                    snackbarHostState.showSnackbar(response.body()?.message ?: "Błąd usuwania grupy")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Błąd: ${e.message}")
                            } finally {
                                showDeleteGroupDialog = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tak, usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = null }) { Text("Anuluj") }
            }
        )
    }
}