package com.example.myapplication.ui

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
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

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

// Obiekt Retrofit z interceptorem dla tokenu JWT
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/" // dla emulatora

    fun getInstance(context: Context): CourseApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain: Interceptor.Chain ->
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("jwt_token", null)
                val request = chain.request().newBuilder().apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                        Log.d("RetrofitClient", "Added token to request: $token")
                    } else {
                        Log.w("RetrofitClient", "No token found in SharedPreferences")
                    }
                    addHeader("Accept", "application/json")
                }.build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CourseApiService::class.java)
    }
}

// ViewModel do zarządzania kursami
class CourseViewModel(context: Context) : ViewModel() {
    private val _courses = mutableStateOf<List<Course>>(emptyList())
    val courses: State<List<Course>> = _courses

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _courses.value = apiService.getAllCourses()
                _error.value = null
                Log.d("CourseViewModel", "Courses loaded successfully: ${_courses.value}")
            } catch (e: retrofit2.HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji. Zaloguj się ponownie."
                    403 -> "Brak uprawnień do kursów."
                    else -> "Błąd serwera: ${e.code()} - ${e.message()}"
                }
                Log.e("CourseViewModel", "HTTP error: ${_error.value}")
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("CourseViewModel", "Connection error: ${_error.value}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Ekran listy kursów
@Composable
fun CourseListScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: CourseViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseViewModel(context) as T
        }
    })
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
                                    navController.navigate("course/${course.id}")
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
