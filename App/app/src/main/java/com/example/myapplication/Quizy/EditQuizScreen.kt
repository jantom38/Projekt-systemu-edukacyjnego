package com.example.myapplication.Quizy

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.example.myapplication.QuizQuestion
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * @file EditQuizScreen.kt
 *  This file contains the ViewModel and Composable for the Edit Quiz screen.
 */

/**
 *  ViewModel for the EditQuizScreen.
 * Handles fetching, updating, and deleting quiz data and questions.
 * @param context The Android context.
 * @param quizId The ID of the quiz to be edited.
 */
class EditQuizViewModel(context: Context, private val quizId: Long) : ViewModel() {
    private val _quiz = mutableStateOf<Quiz?>(null)
    val quiz: State<Quiz?> = _quiz

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadQuiz()
    }

    /**
     *  Loads the quiz data from the API.
     * Updates [_quiz], [_isLoading], and [_error] states based on the API response.
     */
    fun loadQuiz() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.getQuizForEdit(quizId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _quiz.value = response.body()?.quiz
                    Log.d("EditQuizViewModel", "Załadowano quiz: ${_quiz.value}")
                } else {
                    _error.value = response.body()?.message ?: "Błąd ładowania quizu"
                    Log.e("EditQuizViewModel", "Nie udało się załadować quizu: success=false")
                }
            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak dostępu do quizu"
                    404 -> "Quiz nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("EditQuizViewModel", "Błąd HTTP", e)
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("EditQuizViewModel", "Błąd sieciowy", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     *  Updates the quiz details via the API.
     * @param quiz The updated Quiz object.
     * @param onSuccess Callback function to be invoked on successful update.
     * @param onError Callback function to be invoked with an error message on failure.
     */
    fun updateQuiz(quiz: Quiz, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("EditQuizViewModel", "Aktualizacja quizu ID: $quizId")
                val response = apiService.updateQuiz(quizId, quiz)
                if (response.isSuccessful && response.body()?.success == true) {
                    _quiz.value = response.body()?.quiz
                    Log.d("EditQuizViewModel", "Quiz ID: $quizId zaktualizowano pomyślnie")
                    onSuccess()
                } else {
                    val errorMessage = response.body()?.message ?: "Błąd serwera: ${response.code()}"
                    Log.e("EditQuizViewModel", "Nie udało się zaktualizować quizu: $errorMessage")
                    onError(errorMessage)
                }
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak uprawnień do edycji quizu"
                    404 -> "Quiz nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("EditQuizViewModel", "Błąd HTTP podczas aktualizacji quizu ID: $quizId", e)
                onError(errorMessage)
            } catch (e: Exception) {
                val errorMessage = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("EditQuizViewModel", "Błąd sieciowy podczas aktualizacji quizu ID: $quizId", e)
                onError(errorMessage)
            }
        }
    }

    /**
     *  Deletes a question from the quiz via the API.
     * After successful deletion, it reloads the quiz data.
     * @param questionId The ID of the question to delete.
     * @param onSuccess Callback function to be invoked on successful deletion.
     * @param onError Callback function to be invoked with an error message on failure.
     */
    fun deleteQuestion(questionId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("EditQuizViewModel", "Usuwanie pytania ID: $questionId")
                val response = apiService.deleteQuizQuestion(quizId, questionId)
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("EditQuizViewModel", "Pytanie ID: $questionId usunięto pomyślnie")
                    onSuccess()
                    loadQuiz() // Odświeżyć dane quizu
                } else {
                    val errorMessage = response.body()?.message ?: "Błąd serwera: ${response.code()}"
                    Log.e("EditQuizViewModel", "Nie udało się usunąć pytania ID: $questionId, kod: ${response.code()}")
                    onError(errorMessage)
                }
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak uprawnień do usunięcia pytania"
                    404 -> "Pytanie nie znaleziono"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("EditQuizViewModel", "Błąd HTTP podczas usuwania pytania ID: $questionId", e)
                onError(errorMessage)
            } catch (e: Exception) {
                val errorMessage = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("EditQuizViewModel", "Błąd sieciowy podczas usuwania pytania ID: $questionId", e)
                onError(errorMessage)
            }
        }
    }
}

/**
 *  Composable function for the Edit Quiz screen.
 * Allows users to edit quiz details and manage its questions.
 * @param navController The NavHostController for navigation.
 * @param quizId The ID of the quiz to be edited.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuizScreen(navController: NavHostController, quizId: Long) {
    val context = LocalContext.current
    val viewModel: EditQuizViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditQuizViewModel(context, quizId) as T
        }
    })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var numberOfQuestions by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.quiz.value) {
        viewModel.quiz.value?.let { quiz ->
            title = quiz.title
            description = quiz.description ?: ""
            numberOfQuestions = quiz.numberOfQuestionsToDisplay?.toString() ?: ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edytuj quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.error.value!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadQuiz() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tytuł quizu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = numberOfQuestions,
                        onValueChange = { numberOfQuestions = it },
                        label = { Text("Liczba pytań do wyświetlenia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val numQuestionsInt = numberOfQuestions.toIntOrNull()
                            if (numQuestionsInt == null || numQuestionsInt <= 0) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Liczba pytań musi być większa niż 0")
                                }
                                return@Button
                            }
                            viewModel.updateQuiz(
                                Quiz(
                                    id = quizId,
                                    title = title,
                                    description = description,
                                    numberOfQuestionsToDisplay = numQuestionsInt
                                ),
                                onSuccess = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Quiz zaktualizowany pomyślnie")
                                        navController.popBackStack()
                                    }
                                },
                                onError = { error ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zapisz zmiany")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pytania",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(viewModel.quiz.value?.questions ?: emptyList()) { question ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        question.id?.let { id ->
                                            navController.navigate("edit_question/$quizId/$id")
                                        }
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = question.questionText,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Typ: ${question.questionType}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            question.id?.let { id ->
                                                viewModel.deleteQuestion(
                                                    id,
                                                    onSuccess = {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar("Pytanie usunięte pomyślnie")
                                                        }
                                                    },
                                                    onError = { error ->
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(error)
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        enabled = question.id != null
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Usuń pytanie",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = { navController.navigate("add_question/$quizId") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("Dodaj pytanie")
                            }
                        }
                    }
                }
            }
        }
    }
}