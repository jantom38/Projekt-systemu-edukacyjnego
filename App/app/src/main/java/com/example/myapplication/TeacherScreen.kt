package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Główny ekran dla nauczyciela: lista kursów z przyciskami
 * „Dodaj plik” i „Usuń plik”, plus fab do dodawania kursu.
 */
@Composable
fun TeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Stany do pickera plików
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var showFilePicker by remember { mutableStateOf(false) }

    // ViewModel do uploadu
    val fileUploadViewModel: FileUploadViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FileUploadViewModel(context) as T
        }
    })

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("add_course") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Dodaj kurs") },
                text = { Text("Dodaj kurs") }
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
                onAddFileClick = { course ->
                    selectedCourseId = course.id
                    showFilePicker = true
                },
                onDeleteFileClick = { course ->
                    navController.navigate("manage_files/${course.id}")
                }
            )
        }

        // Picker plików do uploadu
        if (showFilePicker) {
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { uriNonNull ->
                    selectedCourseId?.let { courseId ->
                        scope.launch {
                            fileUploadViewModel.uploadFile(courseId, uriNonNull, context)
                        }
                    }
                }
                showFilePicker = false
            }
            LaunchedEffect(Unit) {
                filePickerLauncher.launch("*/*")
            }
        }
    }
}


/**
 * Lista kursów z przyciskami "Dodaj plik" i "Usuń plik".
 * Używa Twojego API: getAllCourses() zwraca od razu List<Course>.
 */
@Composable
fun CourseListScreen(
    onAddFileClick: (Course) -> Unit,
    onDeleteFileClick: (Course) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }

    // Przy starcie pobierz listę kursów
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val fetched = api.getAllCourses()       // <<< tu poprawka!
                withContext(Dispatchers.Main) {
                    courses = fetched
                }
            } catch (e: Exception) {
                Log.e("CourseList", "Błąd pobierania kursów: ${e.message}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(courses) { course ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(course.courseName, style = MaterialTheme.typography.titleMedium)
                    course.description?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = { onAddFileClick(course) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dodaj plik")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onDeleteFileClick(course) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("Usuń plik", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }
}


/**
 * Ekran do zarządzania plikami kursu (lista + usuwanie).
 */
@Composable
fun ManageFilesScreen(
    navController: NavHostController,
    courseId: Long
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<CourseFile>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Funkcja do odświeżania listy plików
    fun loadFiles() {
        scope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(context)
                val fetchedFiles = api.getCourseFiles(courseId)
                files = fetchedFiles
            } catch (e: Exception) {
                Log.e("ManageFiles", "Błąd pobierania plików: ${e.message}")
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
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(
                    items = files,
                    key = { it.id } // Dodaj klucze dla lepszego śledzenia elementów
                ) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
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
                                                // Optymistyczna aktualizacja UI
                                                files = files.filterNot { it.id == file.id }
                                                snackbarHostState.showSnackbar("Plik usunięty")

                                                // Wymuś ponowne załadowanie danych z serwera
                                                loadFiles()
                                            } else {
                                                snackbarHostState.showSnackbar("Błąd usuwania pliku")
                                                // Przywróć plik jeśli serwer zwrócił błąd
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

    // Pobierz pliki przy starcie
    LaunchedEffect(Unit) {
        loadFiles()
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CourseListScreen(
                navController = navController,
                onCourseClick = { course ->
                    // Przekierowanie do ekranu z kluczem dostępu
                    navController.navigate("access_key/${course.id}")
                }
            )
        }
    }
}

