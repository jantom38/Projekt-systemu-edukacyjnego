package com.example.myapplication.files

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
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * @file ManageFilesScreen.kt
 * Plik zawiera funkcję composable dla ekranu zarządzania plikami kursu oraz powiązany ViewModel.
 */

/**
 * ViewModel dla ekranu ManageFilesScreen.
 *
 * Ten ViewModel odpowiada za pobieranie, wyświetlanie i usuwanie plików związanych z kursem.
 *
 * @param context Kontekst aplikacji.
 * @param courseId Identyfikator kursu, dla którego zarządzane są pliki.
 */
class ManageFilesViewModel(context: Context, private val courseId: Long) : ViewModel() {
    private val _files = mutableStateOf<List<CourseFile>>(emptyList())
    /** Lista plików kursu. */
    val files: State<List<CourseFile>> = _files

    private val _isLoading = mutableStateOf(true)
    /** Flaga wskazująca, czy trwa ładowanie danych. */
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    /** Komunikat błędu, jeśli wystąpił problem podczas ładowania danych. */
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadFiles()
    }

    /**
     * Pobiera listę plików dla bieżącego kursu z API.
     *
     * Aktualizuje stany [_files], [_isLoading] i [_error] na podstawie odpowiedzi z API.
     */
    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _files.value = apiService.getCourseFiles(courseId)
                Log.d("ManageFiles", "Załadowano pliki: ${_files.value}")
            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak dostępu do kursu"
                    404 -> "Kurs nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("ManageFiles", "Błąd HTTP", e)
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("ManageFiles", "Błąd sieciowy", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Usuwa określony plik z kursu.
     *
     * @param fileId Identyfikator pliku do usunięcia.
     * @param onSuccess Funkcja zwrotna wywoływana po pomyślnym usunięciu pliku.
     * @param onError Funkcja zwrotna wywoływana w przypadku błędu podczas usuwania pliku, z komunikatem błędu.
     */
    fun deleteFile(fileId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("ManageFiles", "Próba usunięcia pliku ID: $fileId")
                val response = apiService.deleteCourseFile(courseId, fileId)
                if (response.isSuccessful) {
                    Log.d("ManageFiles", "Plik ID: $fileId usunięty pomyślnie")
                    onSuccess()
                    loadFiles() // Odśwież listę plików
                } else {
                    val errorMessage = "Błąd serwera: ${response.code()}"
                    Log.e("ManageFiles", "Błąd usuwania pliku ID: $fileId, kod: ${response.code()}")
                    onError(errorMessage)
                }
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak uprawnień do usunięcia pliku"
                    404 -> "Plik nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("ManageFiles", "Błąd HTTP przy usuwaniu pliku ID: $fileId", e)
                onError(errorMessage)
            } catch (e: Exception) {
                val errorMessage = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("ManageFiles", "Błąd sieciowy przy usuwaniu pliku ID: $fileId", e)
                onError(errorMessage)
            }
        }
    }
}

/**
 * Funkcja composable dla ekranu zarządzania plikami.
 *
 * Ten ekran umożliwia użytkownikom przeglądanie, przesyłanie i usuwanie plików związanych z określonym kursem.
 * Wyświetla listę plików, oferuje opcję dodawania nowych plików za pomocą selektora plików
 * oraz umożliwia usuwanie istniejących plików.
 *
 * @param navController Kontroler nawigacji do przechodzenia między ekranami.
 * @param courseId Identyfikator kursu, dla którego zarządzane są pliki.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFilesScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val viewModel: ManageFilesViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManageFilesViewModel(context, courseId) as T
        }
    })
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
                    viewModel.loadFiles()
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
                title = { Text("Zarządzaj plikami") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFilePicker = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj plik", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
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
                            onClick = { viewModel.loadFiles() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (viewModel.files.value.isEmpty()) {
                            item {
                                Text("Brak plików dla tego kursu")
                            }
                        } else {
                            items(viewModel.files.value) { file ->
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
                                        Text(
                                            text = file.fileName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteFile(
                                                    fileId = file.id,
                                                    onSuccess = {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar("Plik usunięty pomyślnie")
                                                        }
                                                    },
                                                    onError = { error ->
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(error)
                                                        }
                                                    }
                                                )
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Usuń plik",
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