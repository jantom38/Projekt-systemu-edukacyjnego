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

/**
 * @file QuizResultScreen.kt
 *  This file contains the composable function for displaying quiz results and its associated ViewModel.
 */

/**
 *  Composable function for the Quiz Result Screen.
 *
 * This screen displays the user's performance on a completed quiz, including the score,
 * the number of correct answers, total questions, and a detailed breakdown of each question
 * with the correct answer and the user's selected answer.
 *
 * @param quizId The ID of the quiz for which to display results.
 * @param courseId The ID of the course to which the quiz belongs (not directly used in this screen's logic but passed for navigation context).
 * @param navController The NavHostController for navigating between screens.
 */
@Composable
fun QuizResultScreen(
    quizId: Long,
    courseId: Long, // Dodajemy courseId
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
                    val progress = if (result.totalQuestions > 0) result.correctAnswers.toFloat() / result.totalQuestions else 0f
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
                            text = "Wynik: ${NumberFormat.getPercentInstance().format(progress)}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Poprawne odpowiedzi: ${result.correctAnswers} / ${result.totalQuestions}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(result.questions) { questionResult ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (questionResult.isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Pytanie: ${questionResult.questionText}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Twoja odpowiedź: ${questionResult.userAnswer}",
                                            color = if (questionResult.isCorrect) Color.Unspecified else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Poprawna odpowiedź: ${questionResult.correctAnswer}",
                                            color = Color.Green
                                        )
                                        if (!questionResult.isCorrect && questionResult.explanation != null) {
                                            Text(
                                                text = "Wyjaśnienie: ${questionResult.explanation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 *  ViewModel for the QuizResultScreen.
 *
 * This ViewModel handles fetching the quiz results from the API.
 *
 * @param context The application context.
 * @param quizId The ID of the quiz for which to fetch results.
 */
class QuizResultViewModel(context: Context, private val quizId: Long) : ViewModel() {
    private val apiService = RetrofitClient.getInstance(context)

    private val _result = mutableStateOf<QuizResult?>(null)
    val result: State<QuizResult?> = _result

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        loadResult()
    }

    /**
     *  Loads the quiz results from the API.
     *
     * Updates the [_result], [_isLoading], and [_error] states based on the API response.
     */
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

/**
 *  Data class representing the overall result of a quiz.
 *
 * @param quizId The ID of the quiz.
 * @param correctAnswers The number of correctly answered questions.
 * @param totalQuestions The total number of questions in the quiz.
 * @param score The calculated score for the quiz.
 * @param questions A list of [QuestionResult] objects, providing details for each question.
 */
data class QuizResult(
    val quizId: Long,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val score: Double,
    val questions: List<QuestionResult>
)

/**
 *  Data class representing the result for a single question within a quiz.
 *
 * @param questionId The ID of the question.
 * @param questionText The text of the question.
 * @param userAnswer The answer provided by the user for this question.
 * @param correctAnswer The correct answer(s) for this question.
 * @param isCorrect A boolean indicating whether the user's answer was correct.
 * @param explanation An optional explanation for the correct answer, if available.
 */
data class QuestionResult(
    val questionId: Long,
    val questionText: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val explanation: String?
)