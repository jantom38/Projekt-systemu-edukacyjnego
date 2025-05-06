package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.URLConnection

class CourseDetailsViewModel(context: Context, private val courseId: Long) : ViewModel() {
    private val _files = mutableStateOf<List<CourseFile>>(emptyList())
    val files: State<List<CourseFile>> = _files

    private val _quizzes = mutableStateOf<List<Quiz>>(emptyList())
    val quizzes: State<List<Quiz>> = _quizzes

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load files
                _files.value = apiService.getCourseFiles(courseId)
                Log.d("CourseDetails", "Loaded files: ${_files.value}")

                // Load quizzes
                val quizResponse = apiService.getCourseQuizzes(courseId)
                if (quizResponse.success) {
                    _quizzes.value = quizResponse.quizzes
                    Log.d("CourseDetails", "Loaded quizzes: ${_quizzes.value}")
                } else {
                    _error.value = "Błąd ładowania quizów"
                    Log.e("CourseDetails", "Failed to load quizzes: success=false")
                }
            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak dostępu do kursu"
                    404 -> "Kurs nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("CourseDetails", "HTTP error", e)
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("CourseDetails", "Network error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteQuiz(quizId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("CourseDetails", "Próba usunięcia quizu ID: $quizId")
                val response = apiService.deleteQuiz(quizId)
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("CourseDetails", "Quiz ID: $quizId usunięty pomyślnie")
                    onSuccess()
                    loadContent() // Odśwież listę quizów
                } else {
                    val errorMessage = response.body()?.message ?: "Błąd serwera: ${response.code()}"
                    Log.e("CourseDetails", "Błąd usuwania quizu ID: $quizId, kod: ${response.code()}, wiadomość: $errorMessage")
                    onError(errorMessage)
                }
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak uprawnień do usunięcia quizu"
                    404 -> "Quiz nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("CourseDetails", "HTTP error przy usuwaniu quizu ID: $quizId", e)
                onError(errorMessage)
            } catch (e: Exception) {
                val errorMessage = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("CourseDetails", "Network error przy usuwaniu quizu ID: $quizId", e)
                onError(errorMessage)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val viewModel: CourseDetailsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseDetailsViewModel(context, courseId) as T
        }
    })
    LaunchedEffect(key1 = navController.currentBackStackEntry) {
        viewModel.loadContent()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilePicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val fileUploadViewModel: FileUploadViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FileUploadViewModel(context) as T
        }
    })

    LaunchedEffect(fileUploadViewModel.uploadState) {
        fileUploadViewModel.uploadState.collect { state ->
            when (state) {
                is FileUploadState.Success -> {
                    snackbarHostState.showSnackbar(state.message)
                    viewModel.loadContent()
                }
                is FileUploadState.Error -> {
                    snackbarHostState.showSnackbar(state.error)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły kursu") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showFilePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dodaj plik")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { navController.navigate("manage_files/$courseId") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Zarządzaj plikami")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { navController.navigate("add_quiz/$courseId") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dodaj quiz")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                viewModel.isLoading.value -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                viewModel.error.value != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.error.value!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadContent() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }

                else -> {
                    TabRow(selectedTabIndex = 0) {
                        Tab(
                            selected = true,
                            onClick = {},
                            text = { Text("Pliki") }
                        )
                        Tab(
                            selected = false,
                            onClick = {},
                            text = { Text("Quizy") }
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Pliki",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        if (viewModel.files.value.isEmpty()) {
                            item {
                                Text("Brak plików dla tego kursu")
                            }
                        } else {
                            items(viewModel.files.value) { file ->
                                FileCard(file = file, context = context)
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Quizy",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        if (viewModel.quizzes.value.isEmpty()) {
                            item {
                                Text("Brak quizów dla tego kursu")
                            }
                        } else {
                            items(viewModel.quizzes.value) { quiz ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = quiz.title,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            quiz.description?.let {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                quiz.id?.let { quizId ->
                                                    viewModel.deleteQuiz(
                                                        quizId = quizId,
                                                        onSuccess = {
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("Quiz usunięty pomyślnie")
                                                            }
                                                        },
                                                        onError = { error ->
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar(error)
                                                            }
                                                        }
                                                    )
                                                }
                                            },
                                            enabled = quiz.id != null
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Usuń quiz",
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
        }

        if (showFilePicker) {
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { uriNonNull ->
                    fileUploadViewModel.uploadFile(courseId, uriNonNull, context)
                }
                showFilePicker = false
            }
            LaunchedEffect(Unit) {
                filePickerLauncher.launch("*/*")
            }
        }
    }
}