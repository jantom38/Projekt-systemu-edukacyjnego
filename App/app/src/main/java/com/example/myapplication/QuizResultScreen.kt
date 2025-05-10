package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
@Composable
fun QuizResultScreen(
    quizId: Long,
    correctAnswers: Int,
    totalQuestions: Int,
    navController: NavHostController
) {
    val percentage = correctAnswers.toFloat() / totalQuestions

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                progress = percentage,
                modifier = Modifier.size(120.dp),
                strokeWidth = 8.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = NumberFormat.getPercentInstance().format(percentage),
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$correctAnswers / $totalQuestions poprawnych odpowiedzi",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { navController.popBackStack("course_details/$quizId", false) }) {
                Text("Powrót do kursu")
            }
        }
    }
}


@Composable
fun QuestionResultItem(question: QuestionResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
                color = if(question.isCorrect) Color.Green else Color.Red
            )
            Text(
                text = "Poprawna odpowiedź: ${question.correctAnswer}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.getQuizResult(quizId)
                if (response.isSuccessful) {
                    _result.value = response.body()
                } else {
                    _error.value = "Błąd ładowania wyników: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.localizedMessage}"
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