package com.example.myapplication.courses

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.myapplication.files.CourseFile
import com.example.myapplication.files.FileCard
import com.example.myapplication.Quiz
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * @file CourseDeatilsScreen.kt
 * Ten plik zawiera definicje ViewModelu [CourseDetailsViewModel] oraz kompozycyjnej funkcji ekranu [CourseDetailsScreen],
 * odpowiedzialnych za wyświetlanie szczegółów kursu, zarządzanie plikami i quizami,
 * a także nawigację i obsługę błędów w aplikacji.
 */

/**
 * ViewModel dla ekranu szczegółów kursu.
 * Odpowiedzialny za pobieranie i zarządzanie danymi o plikach i quizach dla danego kursu.
 *
 * @param context Kontekst aplikacji, używany do inicjalizacji RetrofitClient.
 * @param courseId Identyfikator kursu, dla którego mają być wyświetlane szczegóły.
 */
class CourseDetailsViewModel(context: Context, private val courseId: Long) : ViewModel() {
    /** Lista plików powiązanych z kursem. */
    private val _files = mutableStateOf<List<CourseFile>>(emptyList())
    val files: State<List<CourseFile>> = _files

    /** Lista quizów powiązanych z kursem. */
    private val _quizzes = mutableStateOf<List<Quiz>>(emptyList())
    val quizzes: State<List<Quiz>> = _quizzes

    /** Stan ładowania danych. True, jeśli dane są aktualnie ładowane, w przeciwnym razie false. */
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    /** Wiadomość o błędzie, jeśli wystąpi problem podczas ładowania danych. Null, jeśli brak błędów. */
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    /** Instancja RetrofitClient do komunikacji z API. */
    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadContent()
    }

    /**
     * Ładuje zawartość kursu, w tym pliki i quizy, z API.
     * Obsługuje stany ładowania i błędy.
     */
    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _files.value = apiService.getCourseFiles(courseId)
                Log.d("CourseDetails", "Loaded files: ${_files.value}")

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

    /**
     * Usuwa quiz o podanym identyfikatorze.
     * Po udanym usunięciu odświeża listę quizów.
     *
     * @param quizId Identyfikator quizu do usunięcia.
     * @param onSuccess Funkcja wywoływana po pomyślnym usunięciu quizu.
     * @param onError Funkcja wywoływana w przypadku błędu podczas usuwania quizu, z komunikatem o błędzie.
     */
    fun deleteQuiz(quizId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("CourseDetails", "Próba usunięcia quizu ID: $quizId")
                val response = apiService.deleteQuiz(quizId)
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("CourseDetails", "Quiz ID: $quizId usunięty pomyślnie")
                    onSuccess()
                    loadContent()
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

/**
 * Kompozycyjna funkcja ekranu szczegółów kursu.
 * Wyświetla informacje o kursie, w tym pliki i quizy, oraz umożliwia nawigację do innych ekranów.
 *
 * @param navController Kontroler nawigacji do obsługi przejść między ekranami.
 * @param courseId Identyfikator kursu, dla którego wyświetlane są szczegóły.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val viewModel: CourseDetailsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseDetailsViewModel(context, courseId) as T
        }
    })
    /**
     * Efekt uruchamiany, gdy zmienia się element na stosie powrotnym nawigacji.
     * Zapewnia odświeżenie zawartości po powrocie do tego ekranu.
     */
    LaunchedEffect(key1 = navController.currentBackStackEntry) {
        viewModel.loadContent()
    }
    /** Stan dla paska snackbar do wyświetlania krótkich wiadomości. */
    val snackbarHostState = remember { SnackbarHostState() }
    /** Zakres korutyn dla operacji asynchronicznych, np. wyświetlania snackbarów. */
    val coroutineScope = rememberCoroutineScope()

    /** Aktualnie wybrana zakładka (0 dla plików, 1 dla quizów). */
    var selectedTabIndex by remember { mutableStateOf(0) }

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
            /** Przyciski akcji dla zarządzania kursem. */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.navigate("quiz_stats/$courseId") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Statystyki", color = MaterialTheme.colorScheme.onSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { navController.navigate("manage_users/$courseId") },
                    modifier = Modifier.weight(1f)
                ) { Text("Zarządzaj użytkownikami") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            /** Obsługa stanów ładowania i błędów. */
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
                    /** Zakładki do przełączania między plikami a quizami. */
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Pliki") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Quizy") }
                        )
                    }

                    /** Wyświetlanie zawartości w zależności od wybranej zakładki. */
                    when (selectedTabIndex) {
                        0 -> {
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
                            }
                        }
                        1 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                item {
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
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { quiz.id?.let { navController.navigate("quiz_results/$it") } }
                                                ) {
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
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            quiz.id?.let { id ->
                                                                navController.navigate("edit_quiz/$id")
                                                            }
                                                        },
                                                        enabled = quiz.id != null
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "Edytuj quiz",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
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
                }
            }
        }
    }
}