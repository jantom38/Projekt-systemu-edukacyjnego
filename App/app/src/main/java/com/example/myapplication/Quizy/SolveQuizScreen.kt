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
 *  This file contains the composable function for solving a quiz and its associated ViewModel.
 */

/**
 *  Composable function for the Solve Quiz Screen.
 *
 * This screen allows users to take a quiz by displaying questions and collecting their answers.
 * Once completed, it navigates to the quiz result screen.
 *
 * @param navController The NavHostController for navigating between screens.
 * @param quizId The ID of the quiz to be solved.
 * @param courseId The ID of the course to which the quiz belongs.
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
 *  ViewModel for the SolveQuizScreen.
 *
 * This ViewModel manages the state of the quiz, including loading questions,
 * storing user's selected answers, and submitting the answers to the API.
 *
 * @param context The application context.
 * @param quizId The ID of the quiz to be solved.
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
     *  Loads the quiz details and its questions from the API.
     *
     * Updates the [_quiz], [_questions], [_isLoading], [_error], and [_showSubmitButton] states.
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
     *  Updates the selected answers for a given question.
     *
     * @param questionId The ID of the question.
     * @param answers A list of strings representing the selected answers for the question.
     */
    fun onAnswerSelected(questionId: Long, answers: List<String>) {
        _selectedAnswers.value = _selectedAnswers.value.toMutableMap().apply {
            put(questionId, answers)
        }
    }

    /**
     *  Submits the user's answers to the API.
     *
     * @return A [SubmissionResultDTO] object containing the result of the submission.
     * @throws Exception if there is an error during submission.
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