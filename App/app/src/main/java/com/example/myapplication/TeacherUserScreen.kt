package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.courses.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// -------------------- TEACHER SCREEN --------------------




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var selectedValidity by remember { mutableStateOf("1_WEEK") }
    var expanded by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    fun loadCourses() {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val fetched = api.getAllCourses()
                withContext(Dispatchers.Main) {
                    courses = fetched
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Błąd pobierania kursów: ${e.message}")
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

    LaunchedEffect(Unit) {
        loadCourses()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("add_course") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Dodaj kurs") },
                text = { Text("Dodaj kurs") }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Moje kursy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
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
    }
}
// -------------------- USER SCREEN --------------------
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val fetched = api.getAllCourses()
                withContext(Dispatchers.Main) {
                    courses = fetched
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Błąd pobierania kursów: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            CourseListScreen(
                navController = navController,
                courses = courses,
                onCourseClick = { course ->
                    navController.navigate("access_key/${course.id}")
                }
            )
        }
    }
}

// -------------------- ADD COURSE SCREEN --------------------
@Composable
fun AddCourseScreen(navController: NavHostController) {
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
                Text("Dodaj kurs", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Anuluj")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                label = { Text("Nazwa kursu") },
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
                            val response = api.createCourse(
                                Course(
                                    courseName = courseName,
                                    description = description,
                                    accessKey = accessKey
                                )
                            )
                            if (response.isSuccessful) {
                                snackbarHostState.showSnackbar("Kurs dodany")
                                navController.popBackStack()
                            } else {
                                snackbarHostState.showSnackbar("Błąd: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Błąd: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zapisz kurs")
            }
        }
    }
}

// -------------------- COURSE LIST SCREEN --------------------
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
                    Spacer(modifier = Modifier.height(8.dp))
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


// -------------------- (jeśli potrzebujesz też) }
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TeacherQuizStatsScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var quizStats by remember { mutableStateOf<List<QuizStat>>(emptyList()) }

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

// Teacher Quiz Detailed Results Screen
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TeacherQuizResultsScreen(navController: NavHostController, quizId: Long) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<QuizDetailedResult>>(emptyList()) }
    var quizTitle by remember { mutableStateOf("") }

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

    LaunchedEffect(Unit) {
        loadQuizResults()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Wyniki quizu: $quizTitle") },
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
            items(results) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Użytkownik: ${result.username}", style = MaterialTheme.typography.titleMedium)
                        Text("Wynik: ${result.correctAnswers}/${result.totalQuestions} (${String.format("%.1f%%", result.score)})")
                        Text("Data: ${result.completionDate}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Odpowiedzi:", style = MaterialTheme.typography.titleSmall)
                        result.answers.forEach { answer ->
                            val isCorrect = answer["isCorrect"] as Boolean
                            Text(
                                "${answer["questionText"]}: ${answer["userAnswer"]} (${if (isCorrect) "Poprawna" else "Niepoprawna"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}