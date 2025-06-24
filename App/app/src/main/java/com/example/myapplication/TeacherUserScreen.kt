/**
 * @file TeacherUserScreen.kt
 *  Plik zawierający komponenty interfejsu użytkownika dla ekranów nauczyciela i studenta w aplikacji mobilnej.
 *
 * Zawiera definicje ekranów takich jak TeacherScreen, UserScreen, AddCourseScreen, CourseListScreen,
 * TeacherQuizStatsScreen oraz TeacherQuizResultsScreen, które obsługują zarządzanie grupami kursów,
 * kursami, quizami oraz ich statystykami i wynikami.
 */
package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import okhttp3.ResponseBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 *  Ekran główny dla nauczyciela, umożliwiający zarządzanie grupami kursów.
 * @param navController Kontroler nawigacji do przechodzenia między ekranami.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Stany dla grup kursów
    var courseGroups by remember { mutableStateOf<List<CourseGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showDuplicateCourseDialog by remember { mutableStateOf<Pair<Long, Course>?>(null) }

    // Stany dla generowania kodu rejestracyjnego
    var showCodeDialog by remember { mutableStateOf(false) }
    var selectedValidity by remember { mutableStateOf("1_WEEK") }
    var expanded by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf<CourseGroup?>(null) }

    /**
     *  Ładuje grupy kursów z serwera.
     */
    fun loadCourseGroups() {
        scope.launch {
            isLoading = true
            try {
                val api = RetrofitClient.getInstance(context)
                val response = api.getCourseGroups()
                if (response.isSuccessful) {
                    courseGroups = response.body() ?: emptyList()
                } else {
                    snackbarHostState.showSnackbar("Błąd pobierania grup: ${response.code()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Błąd: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    /**
     *  Formatuje datę wygaśnięcia kodu w formacie polskim.
     * @param isoDateTime Data w formacie ISO.
     * @return Sformatowana data w formacie "d MMMM yyyy, HH:mm" lub oryginalna wartość, jeśli parsowanie się nie powiedzie.
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
     *  Generuje kod rejestracyjny dla studentów.
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

    LaunchedEffect(Unit, navController.currentBackStackEntry) {
        loadCourseGroups()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Zarządzanie Kursami") },
                actions = {
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
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateGroupDialog = true },
                icon = { Icon(Icons.Default.Add, "Nowa grupa") },
                text = { Text("Nowa grupa") }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (courseGroups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nie masz jeszcze żadnych grup kursów.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(courseGroups) { group ->
                    CourseGroupCard(
                        group = group,
                        navController = navController,
                        onAddCourseClick = { navController.navigate("add_course/${group.id}") },
                        onDuplicateCourseClick = { course -> showDuplicateCourseDialog = Pair(group.id, course) },
                        onDeleteGroupClick = { showDeleteGroupDialog = group },
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
                        }
                    )
                }
            }
        }
    }

    if (showDeleteGroupDialog != null) {
        val groupToDelete = showDeleteGroupDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = null },
            title = { Text("Potwierdź usunięcie") },
            text = { Text("Czy na pewno chcesz usunąć grupę '${groupToDelete.name}'? Kursy wewnątrz tej grupy nie zostaną usunięte, ale stracą powiązanie z grupą.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val api = RetrofitClient.getInstance(context)
                                val response = api.deleteCourseGroup(groupToDelete.id)
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

    if (showCreateGroupDialog) {
        CreateCourseGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, description ->
                scope.launch {
                    try {
                        val response = RetrofitClient.getInstance(context).createCourseGroup(mapOf("name" to name, "description" to description))
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

    showDuplicateCourseDialog?.let { (groupId, course) ->
        DuplicateCourseDialog(
            originalCourseName = course.courseName,
            onDismiss = { showDuplicateCourseDialog = null },
            onDuplicate = { newName, newAccessKey ->
                scope.launch {
                    try {
                        val api = RetrofitClient.getInstance(context)
                        val response = api.duplicateCourse(groupId, course.id, mapOf("newCourseName" to newName, "newAccessKey" to newAccessKey))
                        if (response.isSuccessful) {
                            val responseBody = response.body() as? Map<String, Any>
                            val success = responseBody?.get("success") as? Boolean ?: false
                            val message = responseBody?.get("message") as? String ?: "wykonano"
                            if (success) {
                                snackbarHostState.showSnackbar(message)
                                loadCourseGroups()
                            } else {
                                snackbarHostState.showSnackbar(message)
                            }
                        } else {
                            snackbarHostState.showSnackbar("Błąd duplikowania: ${response.code()}")
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
}

/**
 *  Komponent wyświetlający kartę grupy kursów.
 * @param group Obiekt grupy kursów.
 * @param navController Kontroler nawigacji.
 * @param onAddCourseClick Funkcja wywoływana przy dodawaniu nowego kursu.
 * @param onDuplicateCourseClick Funkcja wywoływana przy duplikowaniu kursu.
 * @param onDeleteGroupClick Funkcja wywoływana przy usuwaniu grupy.
 * @param onDeleteCourseClick Funkcja wywoływana przy usuwaniu kursu.
 */
@Composable
fun CourseGroupCard(
    group: CourseGroup,
    navController: NavHostController,
    onAddCourseClick: () -> Unit,
    onDuplicateCourseClick: (Course) -> Unit,
    onDeleteGroupClick: () -> Unit,
    onDeleteCourseClick: (Course) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            ListItem(
                headlineContent = { Text(group.name, style = MaterialTheme.typography.titleLarge) },
                supportingContent = { group.description?.let { Text(it) } },
                trailingContent = {
                    Row {
                        IconButton(onClick = onDeleteGroupClick) {
                            Icon(Icons.Default.DeleteForever, "Usuń grupę", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Rozwiń")
                        }
                    }
                },
                modifier = Modifier.clickable { expanded = !expanded }
            )
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    group.courses.forEach { course ->
                        Divider()
                        CourseItemRow(
                            course = course,
                            onDetailsClick = { navController.navigate("course_details/${course.id}") },
                            onDuplicateClick = { onDuplicateCourseClick(course) },
                            onDeleteClick = { onDeleteCourseClick(course) }
                        )
                    }
                    Divider()
                    TextButton(onClick = onAddCourseClick, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dodaj nową wersję kursu")
                    }
                }
            }
        }
    }
}

/**
 *  Komponent wyświetlający wiersz kursu w karcie grupy.
 * @param course Obiekt kursu.
 * @param onDetailsClick Funkcja wywoływana przy kliknięciu w szczegóły kursu.
 * @param onDuplicateClick Funkcja wywoływana przy duplikowaniu kursu.
 * @param onDeleteClick Funkcja wywoływana przy usuwaniu kursu.
 */
@Composable
fun CourseItemRow(
    course: Course,
    onDetailsClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = course.courseName,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onDetailsClick)
        )
        Row {
            IconButton(onClick = onDuplicateClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ContentCopy, "Duplikuj", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 *  Dialog do tworzenia nowej grupy kursów.
 * @param onDismiss Funkcja wywoływana przy zamknięciu dialogu.
 * @param onCreate Funkcja wywoływana przy tworzeniu grupy z nazwą i opisem.
 */
@Composable
fun CreateCourseGroupDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa grupa kursów") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa grupy") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Opis (opcjonalnie)") })
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, description) }, enabled = name.isNotBlank()) { Text("Utwórz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

/**
 *  Dialog do duplikowania kursu.
 * @param originalCourseName Oryginalna nazwa kursu.
 * @param onDismiss Funkcja wywoływana przy zamknięciu dialogu.
 * @param onDuplicate Funkcja wywoływana przy duplikowaniu kursu z nową nazwą i kluczem dostępu.
 */
@Composable
fun DuplicateCourseDialog(originalCourseName: String, onDismiss: () -> Unit, onDuplicate: (String, String) -> Unit) {
    var newName by remember { mutableStateOf("$originalCourseName - Kopia") }
    var newAccessKey by remember { mutableStateOf(UUID.randomUUID().toString().take(8).uppercase()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplikuj kurs") },
        text = {
            Column {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nowa nazwa kursu") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = newAccessKey, onValueChange = { newAccessKey = it }, label = { Text("Nowy klucz dostępu") })
            }
        },
        confirmButton = { Button(onClick = { onDuplicate(newName, newAccessKey) }, enabled = newName.isNotBlank() && newAccessKey.isNotBlank()) { Text("Duplikuj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

/**
 *  Ekran dla studenta, wyświetlający dostępne grupy kursów.
 * @param navController Kontroler nawigacji do przechodzenia między ekranami.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var courseGroups by remember { mutableStateOf<List<CourseGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val api = RetrofitClient.getInstance(context)
                val response = api.getCourseGroups()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        courseGroups = response.body() ?: emptyList()
                    } else {
                        snackbarHostState.showSnackbar("Błąd pobierania kursów: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Błąd: ${e.message}")
                }
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dostępne Kursy") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(courseGroups) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("enroll_in_group/${group.id}")
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(group.name, style = MaterialTheme.typography.titleLarge)
                            group.description?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 *  Ekran do dodawania nowego kursu do grupy.
 * @param navController Kontroler nawigacji.
 * @param courseGroupId ID grupy kursów, do której dodawany jest kurs.
 */
@Composable
fun AddCourseScreen(navController: NavHostController, courseGroupId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var courseName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dodaj nową wersję kursu", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Anuluj")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                label = { Text("Nazwa kursu (np. Edycja letnia 2025)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = accessKey,
                onValueChange = { accessKey = it },
                label = { Text("Klucz dostępu") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val api = RetrofitClient.getInstance(context)
                            val courseData = mapOf(
                                "courseName" to courseName,
                                "description" to description,
                                "accessKey" to accessKey,
                                "courseGroupId" to courseGroupId
                            )
                            val response = api.createCourse(courseData)
                            if (response.isSuccessful) {
                                snackbarHostState.showSnackbar("Kurs dodany")
                                navController.popBackStack()
                            } else {
                                snackbarHostState.showSnackbar("Błąd: ${response.code()} - ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Błąd: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = courseName.isNotBlank() && accessKey.isNotBlank()
            ) {
                Text("Zapisz kurs")
            }
        }
    }
}

/**
 *  Ekran wyświetlający listę kursów dla nauczyciela.
 * @param navController Kontroler nawigacji.
 * @param courses Lista kursów do wyświetlenia.
 * @param onCourseClick Funkcja wywoływana przy kliknięciu w kurs.
 * @param onDeleteCourseClick Funkcja wywoływana przy usuwaniu kursu (opcjonalna).
 * @param onViewStatsClick Funkcja wywoływana przy przeglądaniu statystyk (opcjonalna).
 */
@Composable
fun CourseListScreen(
    navController: NavHostController,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    onDeleteCourseClick: ((Course) -> Unit)? = null,
    onViewStatsClick: ((Course) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(courses) { course ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { onCourseClick(course) },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(course.courseName, style = MaterialTheme.typography.titleMedium)
                    course.description?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (onDeleteCourseClick != null) {
                            Button(
                                onClick = { onDeleteCourseClick(course) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Usuń kurs", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 *  Ekran wyświetlający statystyki quizów dla kursu.
 * @param navController Kontroler nawigacji.
 * @param courseId ID kursu.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TeacherQuizStatsScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var quizStats by remember { mutableStateOf<List<QuizStat>>(emptyList()) }

    /**
     *  Ładuje statystyki quizów z serwera.
     */
    fun loadQuizStats() {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val response = api.getCourseQuizStats(courseId)
                if (response.isSuccessful) {
                    val statsResponse = response.body()
                    withContext(Dispatchers.Main) {
                        quizStats = statsResponse?.stats ?: emptyList()
                    }
                } else {
                    snackbarHostState.showSnackbar("Błąd pobierania statystyk: ${response.code()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Błąd: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadQuizStats()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Statystyki quizów") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(quizStats) { stat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { navController.navigate("quiz_results/${stat.quizId}") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stat.quizTitle, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Liczba prób: ${stat.attempts}", style = MaterialTheme.typography.bodyMedium)
                        Text("Średni wynik: ${String.format("%.1f%%", stat.averageScore)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/**
 *  Ekran wyświetlający szczegółowe wyniki quizu dla nauczyciela.
 * @param navController Kontroler nawigacji.
 * @param quizId ID quizu.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TeacherQuizResultsScreen(navController: NavHostController, quizId: Long) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<QuizDetailedResult>>(emptyList()) }
    var quizTitle by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<QuizDetailedResult?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    /**
     *  Ładuje wyniki quizów z serwera.
     */
    fun loadQuizResults() {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val response = api.getQuizDetailedResults(quizId)
                if (response.isSuccessful) {
                    val resultsResponse = response.body()
                    withContext(Dispatchers.Main) {
                        quizTitle = resultsResponse?.quizTitle ?: ""
                        results = resultsResponse?.results ?: emptyList()
                    }
                } else {
                    snackbarHostState.showSnackbar("Błąd pobierania wyników: ${response.code()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Błąd: ${e.message}")
            }
        }
    }

    /**
     *  Usuwa wynik quizu.
     * @param result Obiekt wyniku quizu do usunięcia.
     */
    fun deleteResult(result: QuizDetailedResult) {
        scope.launch {
            try {
                val response = RetrofitClient.getInstance(context).deleteQuizResult(result.resultId)
                if (response.isSuccessful) {
                    snackbarHostState.showSnackbar("Wynik usunięty pomyślnie")
                    loadQuizResults()
                } else {
                    snackbarHostState.showSnackbar("Błąd usuwania: ${response.code()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Błąd: ${e.message}")
            } finally {
                showDeleteDialog = null
            }
        }
    }

    /**
     *  Pobiera raport wyników quizu w formacie PDF.
     */
    fun downloadPdf() {
        isDownloading = true
        scope.launch {
            try {
                val response = RetrofitClient.getInstance(context).downloadQuizResultsPdf(quizId)
                if (response.isSuccessful && response.body() != null) {
                    val body: ResponseBody = response.body()!!
                    val (isSuccess, errorMessage) = savePdfToDownloads(context, body, "wyniki_quizu_${quizId}.pdf")
                    if (isSuccess) {
                        snackbarHostState.showSnackbar("Raport PDF zapisany w Pobranych")
                    } else {
                        snackbarHostState.showSnackbar("Błąd zapisu pliku: $errorMessage")
                    }
                } else {
                    snackbarHostState.showSnackbar("Błąd pobierania PDF: ${response.code()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Błąd sieci: ${e.message ?: "Nieznany błąd"}")
            } finally {
                isDownloading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadQuizResults() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Wyniki: $quizTitle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wstecz")
                    }
                },
                actions = {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { downloadPdf() }) {
                            Icon(Icons.Default.Download, contentDescription = "Pobierz PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(results) { result ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Użytkownik: ${result.username}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showDeleteDialog = result }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Usuń wynik",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text("Wynik: ${result.correctAnswers}/${result.totalQuestions} (${String.format("%.1f%%", result.score)})")
                        Text("Data: ${result.completionDate}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Potwierdź usunięcie") },
            text = { Text("Czy na pewno chcesz trwale usunąć ten wynik? Ta operacja jest nieodwracalna.") },
            confirmButton = {
                Button(
                    onClick = { deleteResult(showDeleteDialog!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Anuluj") }
            }
        )
    }
}