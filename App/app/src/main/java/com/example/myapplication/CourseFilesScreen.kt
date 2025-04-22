package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch

// Model danych dla pliku kursu
data class CourseFile(
    val id: Long,
    val fileName: String,
    val fileUrl: String
)

// ViewModel dla plików kursu
class CourseFilesViewModel(context: Context, private val courseId: Long) : ViewModel() {
    private val _files = mutableStateOf<List<CourseFile>>(emptyList())
    val files: State<List<CourseFile>> = _files

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _files.value = apiService.getCourseFiles(courseId)
                _error.value = null
                Log.d("CourseFilesViewModel", "Files loaded: ${_files.value}")
            } catch (e: retrofit2.HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji. Zaloguj się ponownie."
                    404 -> "Kurs lub pliki nie znalezione."
                    else -> "Błąd serwera: ${e.code()} - ${e.message()}"
                }
                Log.e("CourseFilesViewModel", "HTTP error: ${_error.value}")
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("CourseFilesViewModel", "Connection error: ${_error.value}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@Composable
fun CourseFilesScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val viewModel: CourseFilesViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseFilesViewModel(context, courseId) as T
        }
    })
    val files by viewModel.files
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Pliki kursu",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadFiles() }) {
                        Text("Spróbuj ponownie")
                    }
                }
            }
            files.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak dostępnych plików",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn {
                    items(files.size) { index ->
                        val file = files[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    // TODO: Otwórz fileUrl w przeglądarce lub aplikacji
                                    Log.d("CourseFilesScreen", "Clicked file: ${file.fileUrl}")
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = file.fileName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "URL: ${file.fileUrl}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Wylogowanie przy błędzie 401
    LaunchedEffect(error) {
        if (error != null && error!!.contains("Brak autoryzacji")) {
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }
}