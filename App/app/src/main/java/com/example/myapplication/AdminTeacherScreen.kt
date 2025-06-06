package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
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

    // Stany widoku
    var showUsersView by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Stany dla grup kursów
    var courseGroups by remember { mutableStateOf<List<CourseGroup>>(emptyList()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showDuplicateCourseDialog by remember { mutableStateOf<Pair<Long, Course>?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf<CourseGroup?>(null) }

    // Stany dla zarządzania użytkownikami
    var users by remember { mutableStateOf<List<UserCourseInfo>>(emptyList()) }
    var showDeleteUserDialog by remember { mutableStateOf<UserCourseInfo?>(null) }

    // Stany dla generowania kodu
    var showCodeDialog by remember { mutableStateOf(false) }
    var selectedValidity by remember { mutableStateOf("1_WEEK") }
    var expanded by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    // --- LOGIKA ---

    fun loadData() {
        if (showUsersView) {
            // Ładowanie użytkowników
            scope.launch {
                isLoading = true
                try {
                    val response = RetrofitClient.getInstance(context).getAllUsers()
                    if (response.success) users = response.users
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar("Błąd: ${e.message}") }
                } finally {
                    isLoading = false
                }
            }
        } else {
            // Ładowanie grup kursów
            scope.launch {
                isLoading = true
                try {
                    val response = RetrofitClient.getInstance(context).getCourseGroups()
                    if (response.isSuccessful) courseGroups = response.body() ?: emptyList()
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar("Błąd: ${e.message}") }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // --- Uruchomienie ładowania danych ---
    LaunchedEffect(showUsersView, navController.currentBackStackEntry) {
        loadData()
    }

    // --- UI ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (showUsersView) "Zarządzaj Użytkownikami" else "Zarządzaj Kursami") },
                actions = {
                    if (showUsersView) {
                        // Przycisk powrotu do widoku kursów
                        TextButton(onClick = { showUsersView = false }) { Text("Pokaż kursy") }
                    } else {
                        // Przyciski dla widoku kursów
                        TextButton(onClick = { showCodeDialog = true }) { Text("Generuj kod") }
                        TextButton(onClick = { showUsersView = true }) { Text("Pokaż użytkowników") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showUsersView) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    icon = { Icon(Icons.Default.Add, "Nowa grupa") },
                    text = { Text("Nowa grupa") }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (showUsersView) {
            // --- WIDOK ZARZĄDZANIA UŻYTKOWNIKAMI ---
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users) { user ->
                    AdminUserCard(
                        user = user,
                        onPromote = { /* Logika promote */ },
                        onDemote = { /* Logika demote */ },
                        onDelete = { showDeleteUserDialog = user }
                    )
                }
            }
        } else {
            // --- WIDOK ZARZĄDZANIA GRUPAMI KURSÓW ---
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(courseGroups) { group ->
                    CourseGroupCard(
                        group = group,
                        navController = navController,
                        onAddCourseClick = { navController.navigate("add_course/${group.id}") },
                        onDuplicateCourseClick = { course -> showDuplicateCourseDialog = Pair(group.id, course) },
                        onDeleteCourseClick = { /* Logika usuwania kursu */ },
                        onDeleteGroupClick = { showDeleteGroupDialog = group }
                    )
                }
            }
        }
    }

    // --- Wszystkie okna dialogowe ---
    // (Tu wklej kod wszystkich potrzebnych okien dialogowych: CreateGroup, DuplicateCourse, DeleteGroup, DeleteUser, GenerateCode)
}

// Pomocniczy Composable dla karty użytkownika w widoku admina
@Composable
fun AdminUserCard(user: UserCourseInfo, onPromote: () -> Unit, onDemote: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(user.username, style = MaterialTheme.typography.titleMedium)
            Text("Rola: ${user.role}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (user.role == "STUDENT") {
                    Button(onClick = onPromote) { Text("Promuj") }
                }
                if (user.role == "TEACHER") {
                    Button(onClick = onDemote, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Degraduj") }
                }
                if (user.role != "ADMIN") {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

// Należy również skopiować tutaj pozostałe pomocnicze Composable z TeacherUserScreen:
// - CourseGroupCard
// - CourseItemRow
// - CreateCourseGroupDialog
// - DuplicateCourseDialog
// - Oraz zaimplementować logikę dla okien dialogowych i akcji admina, która została pominięta dla zwięzłości