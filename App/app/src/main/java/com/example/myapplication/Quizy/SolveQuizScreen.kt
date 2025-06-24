package com.example.myapplication.Quizy

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.myapplication.Quiz
import com.example.myapplication.QuizAnswerDTO
import com.example.myapplication.QuizQuestion
import com.example.myapplication.RetrofitClient
import com.example.myapplication.SubmissionResultDTO
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.get

/**
 * @file SolveQuizScreen.kt
 * Ten plik zawiera funkcję kompozycyjną do rozwiązywania quizu oraz związany z nią ViewModel.
 */

/**
 * Funkcja kompozycyjna dla ekranu rozwiązywania quizu.
 *
 * Ten ekran umożliwia użytkownikom rozwiązywanie quizu poprzez wyświetlanie pytań i zbieranie ich odpowiedzi.
 * Po zakończeniu, nawiguje do ekranu wyników quizu.
 *
 * @param navController NavHostController do nawigacji między ekranami.
 * @param quizId ID quizu do rozwiązania.
 * @param courseId ID kursu, do którego należy quiz.
 */
@Composable
fun SolveQuizScreen(navController: NavHostController, quizId: Long, courseId: Long) {
    val context = LocalContext.current
    val viewModel: SolveQuizViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SolveQuizViewModel(context, quizId) as T
        }
    })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            if (viewModel.showSubmitButton.value) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = viewModel.submitAnswers()
                            navController.navigate("quiz_result/$quizId/$courseId") // Zaktualizowana trasa
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Zakończ quiz")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    Text(
                        text = "Błąd: ${viewModel.error.value}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(viewModel.questions.value) { index, question ->
                            val selectedAnswers = viewModel.selectedAnswers.value[question.id] ?: emptyList()
                            QuestionItem(
                                question = question,
                                index = index + 1,
                                selectedAnswers = selectedAnswers,
                                onAnswerSelected = { answers ->
                                    question.id?.let {
                                        viewModel.onAnswerSelected(it, answers)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ViewModel dla SolveQuizScreen.
 *
 * Ten ViewModel zarządza stanem quizu, w tym ładowaniem pytań,
 * przechowywaniem wybranych odpowiedzi użytkownika i wysyłaniem odpowiedzi do API.
 *
 * @param context Kontekst aplikacji.
 * @param quizId ID quizu do rozwiązania.
 */
class SolveQuizViewModel(context: Context, private val quizId: Long) : ViewModel() {
    private val apiService = RetrofitClient.getInstance(context)

    private val _quiz = mutableStateOf<Quiz?>(null)
    val quiz: State<Quiz?> = _quiz

    private val _questions = mutableStateOf<List<QuizQuestion>>(emptyList())
    val questions: State<List<QuizQuestion>> = _questions

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _selectedAnswers = mutableStateOf<Map<Long, List<String>>>(emptyMap())
    val selectedAnswers: State<Map<Long, List<String>>> = _selectedAnswers

    private val _showSubmitButton = mutableStateOf(false)
    val showSubmitButton: State<Boolean> = _showSubmitButton

    init {
        loadQuiz()
    }

    /**
     * Ładuje szczegóły quizu i jego pytania z API.
     *
     * Aktualizuje stany [_quiz], [_questions], [_isLoading], [_error] i [_showSubmitButton].
     */
    fun loadQuiz() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = apiService.getQuiz(quizId)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _quiz.value = body.quiz
                    _questions.value = body.quiz.questions
                    _showSubmitButton.value = body.quiz.questions.isNotEmpty()
                } else {
                    _error.value = "Błąd: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Aktualizuje wybrane odpowiedzi dla danego pytania.
     *
     * @param questionId ID pytania.
     * @param answers Lista stringów reprezentujących wybrane odpowiedzi dla pytania.
     */
    fun onAnswerSelected(questionId: Long, answers: List<String>) {
        _selectedAnswers.value = _selectedAnswers.value.toMutableMap().apply {
            put(questionId, answers)
        }
    }

    /**
     * Wysyła odpowiedzi użytkownika do API.
     *
     * @return Obiekt [SubmissionResultDTO] zawierający wynik wysłania.
     * @throws Exception jeśli wystąpi błąd podczas wysyłania.
     */
    suspend fun submitAnswers(): SubmissionResultDTO {
        return withContext(Dispatchers.IO) {
            val response = apiService.submitQuizAnswers(
                quizId,
                _selectedAnswers.value.map {
                    QuizAnswerDTO(it.key, it.value.joinToString(","))
                }
            )
            if (!response.isSuccessful) {
                throw Exception("Błąd wysyłania odpowiedzi")
            }
            response.body()!!
        }
    }
}