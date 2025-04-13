package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Model danych kursu
data class Course(
    val id: Long,
    val courseName: String,
    val description: String,
    val accessKey: String
)

// Interfejs API Retrofit
interface CourseApiService {
    @GET("/api/courses")
    suspend fun getAllCourses(): List<Course>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/" // dla emulatora

    val instance: CourseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CourseApiService::class.java)
    }
}

// ViewModel
class CourseViewModel : androidx.lifecycle.ViewModel() {
    private val _courses = mutableStateOf<List<Course>>(emptyList())
    val courses: State<List<Course>> = _courses

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _courses.value = RetrofitClient.instance.getAllCourses()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Błąd ładowania kursów: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Zmodyfikowany ekran
@Composable
fun CourseListScreen(navController: NavHostController, viewModel: CourseViewModel = viewModel()) {
    val courses by viewModel.courses
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dostępne kursy", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            courses.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak dostępnych kursów")
                }
            }

            else -> {
                LazyColumn {
                    items(courses.size) { index ->
                        val course = courses[index]
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    navController.navigate("course/${course.id}")
                                }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(course.courseName, style = MaterialTheme.typography.titleMedium)
                                Text(course.description)
                                Spacer(Modifier.height(4.dp))
                                Text("Klucz dostępu: ${course.accessKey}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}