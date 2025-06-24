package com.example.myapplication.courses

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
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * @file CourseListScreen.kt
 * Ten plik definiuje struktury danych dla kursów, ViewModel do zarządzania listą kursów
 * oraz kompozycyjny ekran wyświetlający listę dostępnych kursów.
 */

/**
 * Klasa danych reprezentująca kurs.
 *
 * @param id Unikalny identyfikator kursu. Domyślnie 0.
 * @param courseName Nazwa kursu.
 * @param description Opis kursu.
 * @param accessKey Klucz dostępu do kursu.
 */
data class Course(
    val id: Long = 0,
    val courseName: String,
    val description: String,
    val accessKey: String
)


/**
 * ViewModel dla ekranu listy kursów.
 * Odpowiedzialny za pobieranie i zarządzanie listą kursów.
 *
 * @param context Kontekst aplikacji, używany do inicjalizacji RetrofitClient.
 */
class CourseViewModel(context: Context) : ViewModel() {
    /** Lista kursów.*/
    private val _courses = mutableStateOf<List<Course>>(emptyList())
    val courses: State<List<Course>> = _courses

    /** Stan ładowania danych. False domyślnie.*/
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    /** Wiadomość o błędzie, jeśli wystąpi problem podczas ładowania danych. Null domyślnie.*/
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    /** Instancja RetrofitClient do komunikacji z API.*/
    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadCourses()
    }

    /**
     * Ładuje listę kursów z API.
     * Obsługuje stany ładowania i błędy, w tym błędy HTTP i sieci.
     */
    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _courses.value = apiService.getAllCourses()
                _error.value = null
                Log.d("CourseViewModel", "Kursy załadowane: ${_courses.value}")
            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji. Zaloguj się ponownie."
                    403 -> "Brak uprawnień do kursów."
                    else -> "Błąd serwera: ${e.code()} - ${e.message()}"
                }
                Log.e("CourseViewModel", "Błąd HTTP: ${_error.value}")
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("CourseViewModel", "Błąd połączenia: ${_error.value}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * Kompozycyjna funkcja ekranu listy kursów.
 * Wyświetla listę dostępnych kursów i umożliwia nawigację do szczegółów kursu.
 *
 * @param navController Kontroler nawigacji do obsługi przejść między ekranami.
 * @param onCourseClick Funkcja wywoływana po kliknięciu na kurs, przyjmująca obiekt [Course].
 */
@Composable
fun CourseListScreen(
    navController: NavHostController,
    onCourseClick: (Course) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CourseViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseViewModel(context) as T
        }
    })
    /** Efekt uruchamiany, gdy zmienia się element na stosie powrotnym nawigacji, odświeżający listę kursów.*/
    LaunchedEffect(key1 = navController.currentBackStackEntry) {
        viewModel.loadCourses()
    }
    val courses by viewModel.courses
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Dostępne kursy",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        /** Obsługa stanów ładowania, błędów i pustej listy kursów.*/
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
                    Button(onClick = { viewModel.loadCourses() }) {
                        Text("Spróbuj ponownie")
                    }
                }
            }

            courses.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak dostępnych kursów",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                LazyColumn {
                    items(courses.size) { index ->
                        val course = courses[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    Log.d("CourseList", "Kliknięto kurs: ${course.id}")
                                    onCourseClick(course)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = course.courseName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = course.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Klucz dostępu: ${course.accessKey}",
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
}