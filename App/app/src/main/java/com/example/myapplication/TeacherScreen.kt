package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// -------------------- TEACHER SCREEN --------------------
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }

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
                                Course(courseName = courseName, description = description, accessKey = accessKey)
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
    onAddFileClick: (Course) -> Unit = {},
    onDeleteFileClick: (Course) -> Unit = {},
    onDeleteCourseClick: ((Course) -> Unit)? = null
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

                    if (onDeleteCourseClick != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
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

// -------------------- (jeśli potrzebujesz też) MANAGE FILES SCREEN --------------------
@Composable
fun ManageFilesScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<CourseFile>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadFiles() {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val fetched = api.getCourseFiles(courseId)
                files = fetched
            } catch (e: Exception) {
                Log.e("ManageFiles", "Błąd pobierania plików: ${e.message}")
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                items(items = files, key = { it.id }) { file ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(file.fileName, style = MaterialTheme.typography.bodyLarge)

                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val api = RetrofitClient.getInstance(context)
                                            val resp = api.deleteCourseFile(courseId, file.id)
                                            if (resp.isSuccessful) {
                                                snackbarHostState.showSnackbar("Plik usunięty")
                                                loadFiles()
                                            } else {
                                                snackbarHostState.showSnackbar("Błąd usuwania pliku")
                                                loadFiles()
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Wyjątek: ${e.message}")
                                            loadFiles()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text("Usuń", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFiles()
    }
}
