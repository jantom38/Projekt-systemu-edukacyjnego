package com.example.myapplication.Quizy

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.NumberFormat
@Composable
fun QuizResultScreen(
    quizId: Long,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: QuizResultViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuizResultViewModel(context, quizId) as T
            }
        }
    )
    val quizResult by viewModel.result
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Scaffold { padding ->
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "Nieznany błąd",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                }
            }
            else -> {
                quizResult?.let { result ->
                    val progress = if (result.totalQuestions > 0)
                        result.correctAnswers.toFloat() / result.totalQuestions
                    else
                        0f

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(120.dp),
                            strokeWidth = 8.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = NumberFormat.getPercentInstance().format(progress),
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${result.correctAnswers} / ${result.totalQuestions} poprawnych odpowiedzi",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Wyświetlenie szczegółów pytań
                        if (result.questions.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(result.questions) { question ->
                                    QuestionResultItem(question)
                                }
                            }
                        } else {
                            Text(
                                text = "Brak szczegółowych wyników dla tego quizu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            navController.popBackStack("course_details/$quizId", false)
                        }) {
                            Text("Powrót do kursu")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } ?: run {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Brak wyników dla tego quizu",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            navController.popBackStack("course_details/$quizId", false)
                        }) {
                            Text("Powrót do kursu")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionResultItem(question: QuestionResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Twoja odpowiedź: ${question.userAnswer}",
                color = if (question.isCorrect) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Poprawna odpowiedź: ${question.correctAnswer}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


class QuizResultViewModel(context: Context, private val quizId: Long) : ViewModel() {
    private val apiService = RetrofitClient.getInstance(context)
    private val _result = mutableStateOf<QuizResult?>(null)
    private val _isLoading = mutableStateOf(true)
    private val _error = mutableStateOf<String?>(null)

    val result: State<QuizResult?> = _result
    val isLoading: State<Boolean> = _isLoading
    val error: State<String?> = _error

    init {
        loadResult()
    }


    fun loadResult() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.getQuizResult(quizId)
                if (response.isSuccessful) {
                    val quizResult = response.body()
                    if (quizResult != null && quizResult.totalQuestions > 0) {
                        _result.value = quizResult
                    } else {
                        _error.value = "Brak wyników dla tego quizu"
                    }
                } else {
                    _error.value = "Błąd ładowania wyników: ${response.code()}"
                }

            } catch (e: IOException) {
                _error.value = "Błąd połączenia: ${e.localizedMessage}"
            } catch (e: Exception) {
                _error.value = "Nieznany błąd: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Modele danych
data class QuizResult(
    val quizId: Long,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val score: Double,
    val questions: List<QuestionResult>
)

data class QuestionResult(
    val questionId: Long,
    val questionText: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean
)
