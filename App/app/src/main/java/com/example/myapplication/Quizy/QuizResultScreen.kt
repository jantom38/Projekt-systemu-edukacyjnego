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
 * Ten plik zawiera funkcję kompozycyjną do wyświetlania wyników quizu i związany z nią ViewModel.
 */

/**
 * Funkcja kompozycyjna dla ekranu wyników quizu.
 *
 * Ten ekran wyświetla wyniki użytkownika z ukończonego quizu, w tym wynik punktowy,
 * liczbę poprawnych odpowiedzi, łączną liczbę pytań oraz szczegółowe zestawienie każdego pytania
 * z poprawną odpowiedzią i odpowiedzią wybraną przez użytkownika.
 *
 * @param quizId ID quizu, dla którego mają być wyświetlone wyniki.
 * @param courseId ID kursu, do którego należy quiz (nie jest bezpośrednio używane w logice tego ekranu, ale jest przekazywane w kontekście nawigacji).
 * @param navController NavHostController do nawigacji między ekranami.
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
 * ViewModel dla QuizResultScreen.
 *
 * Ten ViewModel obsługuje pobieranie wyników quizu z API.
 *
 * @param context Kontekst aplikacji.
 * @param quizId ID quizu, dla którego mają być pobrane wyniki.
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
     * Ładuje wyniki quizu z API.
     *
     * Aktualizuje stany [_result], [_isLoading] i [_error] na podstawie odpowiedzi API.
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
 * Klasa danych reprezentująca ogólny wynik quizu.
 *
 * @param quizId ID quizu.
 * @param correctAnswers Liczba poprawnie udzielonych odpowiedzi.
 * @param totalQuestions Całkowita liczba pytań w quizie.
 * @param score Obliczony wynik dla quizu.
 * @param questions Lista obiektów [QuestionResult], zawierająca szczegóły dla każdego pytania.
 */
data class QuizResult(
    val quizId: Long,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val score: Double,
    val questions: List<QuestionResult>
)

/**
 * Klasa danych reprezentująca wynik dla pojedynczego pytania w quizie.
 *
 * @param questionId ID pytania.
 * @param questionText Tekst pytania.
 * @param userAnswer Odpowiedź udzielona przez użytkownika na to pytanie.
 * @param correctAnswer Poprawna odpowiedź(-i) na to pytanie.
 * @param isCorrect Wartość logiczna wskazująca, czy odpowiedź użytkownika była poprawna.
 * @param explanation Opcjonalne wyjaśnienie poprawnej odpowiedzi, jeśli jest dostępne.
 */
data class QuestionResult(
    val questionId: Long,
    val questionText: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val explanation: String?
)