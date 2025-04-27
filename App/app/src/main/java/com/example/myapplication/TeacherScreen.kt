package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun TeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Stany dla formularza dodawania kursu
    var showAddCourseDialog by remember { mutableStateOf(false) } // Dodajemy tę deklarację
    var courseName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }

    // Stany dla uploadu plików
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var showFilePicker by remember { mutableStateOf(false) }

    val fileUploadViewModel: FileUploadViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FileUploadViewModel(context) as T
        }
    })

    // Funkcja do dodawania kursu
    suspend fun addCourse(name: String, description: String, accessKey: String) {
        withContext(Dispatchers.IO) {
            try {
                val apiService = RetrofitClient.getInstance(context)
                val response = apiService.createCourse(
                    Course(
                        id = 0,
                        courseName = name,
                        description = description,
                        accessKey = accessKey
                    )
                )

                if (!response.isSuccessful) {
                    throw Exception("Błąd serwera: ${response.code()}")
                }
            } catch (e: Exception) {
                throw Exception("Błąd dodawania kursu: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCourseDialog = true },
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
                navController = navController,
                onCourseClick = { course ->
                    selectedCourseId = course.id
                    showFilePicker = true
                }
            )
        }

        // Dialog dodawania kursu
        if (showAddCourseDialog) {
            AlertDialog(
                onDismissRequest = { showAddCourseDialog = false },
                title = { Text("Dodaj nowy kurs") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = { courseName = it },
                            label = { Text("Nazwa kursu*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Opis*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = accessKey,
                            onValueChange = { accessKey = it },
                            label = { Text("Klucz dostępu*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (courseName.isBlank() || description.isBlank() || accessKey.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Wszystkie pola są wymagane")
                                }
                                return@Button
                            }

                            scope.launch {
                                try {
                                    addCourse(courseName, description, accessKey)
                                    snackbarHostState.showSnackbar("Kurs dodany pomyślnie")
                                    showAddCourseDialog = false
                                    courseName = ""
                                    description = ""
                                    accessKey = ""
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.message ?: "Błąd dodawania kursu")
                                }
                            }
                        }
                    ) {
                        Text("Dodaj")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCourseDialog = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }

        // Picker plików
        if (showFilePicker) {
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    selectedCourseId?.let { courseId ->
                        scope.launch {
                            fileUploadViewModel.uploadFile(courseId, it, context)
                        }
                    }
                }
                showFilePicker = false
            }

            LaunchedEffect(showFilePicker) {
                filePickerLauncher.launch("*/*")
            }
        }
    }
}

@Composable
private fun AddCourseDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, desc: String, key: String) -> Unit // Typy parametrów w funkcji
) {
    var courseName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj nowy kurs") },
        text = {
            Column {
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Nazwa kursu*") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis*") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = accessKey,
                    onValueChange = { accessKey = it },
                    label = { Text("Klucz dostępu*") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseName.isBlank() || description.isBlank() || accessKey.isBlank()) {
                        errorMessage = "Wszystkie pola są wymagane"
                        return@Button
                    }
                    onSubmit(courseName, description, accessKey) // Tutaj typy są już znane
                }
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(navController: NavHostController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Moje kursy") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
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
