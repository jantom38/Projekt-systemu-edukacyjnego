package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SolveQuizScreen(navController: NavHostController, quizId: Long) {
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
                            val result = viewModel.submitAnswers()        // <-- DTO
                            navController.navigate("quiz_result/$quizId")                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Zakończ quiz")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
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
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadQuiz() }) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(
                            items = viewModel.questions.value,
                            key = { _, question -> question.id!! }
                        ) { index, question ->
                            QuestionItem(
                                question = question,
                                index = index + 1,
                                selectedAnswers = viewModel.selectedAnswers.value[question.id] ?: emptyList(),
                                onAnswerSelected = { answers ->
                                    viewModel.onAnswerSelected(question.id!!, answers)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

class SolveQuizViewModel(context: Context, val quizId: Long) : ViewModel() {
    private val apiService = RetrofitClient.getInstance(context)
    private val _quiz = mutableStateOf<Quiz?>(null)
    private val _questions = mutableStateOf<List<QuizQuestion>>(emptyList())
    private val _selectedAnswers = mutableStateOf<Map<Long, List<String>>>(emptyMap())
    private val _isLoading = mutableStateOf(true)
    private val _error = mutableStateOf<String?>(null)
    private val _showSubmitButton = mutableStateOf(false)

    val quiz: State<Quiz?> = _quiz
    val questions: State<List<QuizQuestion>> = _questions
    val selectedAnswers: State<Map<Long, List<String>>> = _selectedAnswers
    val isLoading: State<Boolean> = _isLoading
    val error: State<String?> = _error
    val showSubmitButton: State<Boolean> = _showSubmitButton

    init {
        loadQuiz()
    }

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
    fun onAnswerSelected(questionId: Long, answers: List<String>) {
        _selectedAnswers.value = _selectedAnswers.value.toMutableMap().apply {
            put(questionId, answers)
        }
    }

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