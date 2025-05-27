package com.example.myapplication.Quizy

import android.content.Context
import android.util.Log
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
import com.example.myapplication.QuizQuestion
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

class EditQuestionViewModel(context: Context, private val quizId: Long, private val questionId: Long) : ViewModel() {
    private val _question = mutableStateOf<QuizQuestion?>(null)
    val question: State<QuizQuestion?> = _question

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val apiService = RetrofitClient.getInstance(context)

    init {
        loadQuestion()
    }

    fun loadQuestion() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch the entire quiz to find the specific question
                val response = apiService.getQuiz(quizId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val quiz = response.body()?.quiz
                    _question.value = quiz?.questions?.find { it.id == questionId }
                    if (_question.value == null) {
                        _error.value = "Pytanie nie znalezione"
                        Log.e("EditQuestionViewModel", "Question ID: $questionId not found in quiz ID: $quizId")
                    } else {
                        Log.d("EditQuestionViewModel", "Loaded question: ${_question.value}")
                    }
                } else {
                    _error.value = response.body()?.message ?: "Błąd ładowania pytania"
                    Log.e("EditQuestionViewModel", "Failed to load quiz: success=false")
                }
            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak dostępu do quizu"
                    404 -> "Quiz lub pytanie nie znalezione"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("EditQuestionViewModel", "HTTP error", e)
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("EditQuestionViewModel", "Network error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateQuestion(question: QuizQuestion, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("EditQuestionViewModel", "Updating question ID: $questionId")
                // Assuming you have an updateQuizQuestion endpoint in ApiService
                // If not, you'll need to add it to ApiClient.kt and MainControllers.java
                val response = apiService.updateQuizQuestion(quizId, questionId, question)
                if (response.isSuccessful && response.body()?.success == true) {
                    _question.value = response.body()?.question
                    Log.d("EditQuestionViewModel", "Question ID: $questionId updated successfully")
                    onSuccess()
                } else {
                    val errorMessage = response.body()?.message ?: "Błąd serwera: ${response.code()}"
                    Log.e("EditQuestionViewModel", "Failed to update question: $errorMessage")
                    onError(errorMessage)
                }
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak uprawnień do edycji pytania"
                    404 -> "Pytanie nie znalezione"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("EditQuestionViewModel", "HTTP error updating question ID: $questionId", e)
                onError(errorMessage)
            } catch (e: Exception) {
                val errorMessage = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("EditQuestionViewModel", "Network error updating question ID: $questionId", e)
                onError(errorMessage)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuestionScreen(navController: NavHostController, quizId: Long, questionId: Long) {
    val context = LocalContext.current
    val viewModel: EditQuestionViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditQuestionViewModel(context, quizId, questionId) as T
        }
    })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var questionText by remember { mutableStateOf("") }
    var questionType by remember { mutableStateOf("multiple_choice") }
    var correctAnswer by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(mapOf<String, String>()) }
    var expanded by remember { mutableStateOf(false) } // For question type dropdown
    var tfExpanded by remember { mutableStateOf(false) } // For True/False dropdown

    // List of question types for the dropdown
    val questionTypes = listOf(
        "multiple_choice" to "Wielokrotnego wyboru",
        "true_false" to "Prawda/Fałsz",
        "open_ended" to "Otwarte"
    )

    LaunchedEffect(viewModel.question.value) {
        viewModel.question.value?.let { question ->
            questionText = question.questionText
            questionType = question.questionType
            correctAnswer = question.correctAnswer
            options = question.options ?: emptyMap()

            // Special handling for true_false to ensure options are correctly set
            if (question.questionType == "true_false" && options.isEmpty()) {
                options = mapOf("True" to "Prawda", "False" to "Fałsz")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edytuj pytanie") },
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
                            onClick = { viewModel.loadQuestion() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Treść pytania") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Question Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = questionTypes.find { it.first == questionType }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Typ pytania") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            questionTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.second) },
                                    onClick = {
                                        questionType = type.first
                                        // Reset options and correct answer based on new type
                                        when (type.first) {
                                            "multiple_choice" -> {
                                                options = mapOf("A" to "", "B" to "")
                                                correctAnswer = ""
                                            }
                                            "true_false" -> {
                                                options = mapOf("True" to "Prawda", "False" to "Fałsz")
                                                correctAnswer = "" // Or set a default like "True"
                                            }
                                            "open_ended" -> {
                                                options = emptyMap()
                                                correctAnswer = ""
                                            }
                                        }
                                        expanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Conditional rendering based on question type
                    if (questionType == "multiple_choice") {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(options.toList()) { (key, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { newValue ->
                                            options = options.toMutableMap().apply { put(key, newValue) }
                                        },
                                        label = { Text("Opcja $key") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            options = options.toMutableMap().apply { remove(key) }
                                            // Also remove from correctAnswer if it was selected
                                            correctAnswer = correctAnswer.split(",").filter { it != key }.joinToString(",")
                                        },
                                        enabled = options.size > 2 // Minimum 2 options
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Usuń opcję")
                                    }
                                }
                            }
                            item {
                                Button(
                                    onClick = {
                                        val newKey = ('A' + options.size).toString()
                                        options = options.toMutableMap().apply { put(newKey, "") }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = options.size < 10 // Max 10 options
                                ) {
                                    Text("Dodaj opcję")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = correctAnswer,
                            onValueChange = { correctAnswer = it },
                            label = { Text("Poprawna odpowiedź (np. A,B)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (questionType == "true_false") {
                        // True/False Dropdown
                        ExposedDropdownMenuBox(
                            expanded = tfExpanded,
                            onExpandedChange = { tfExpanded = !tfExpanded }
                        ) {
                            OutlinedTextField(
                                value = when (correctAnswer) {
                                    "True" -> "Prawda"
                                    "False" -> "Fałsz"
                                    else -> ""
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Poprawna odpowiedź") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tfExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = tfExpanded,
                                onDismissRequest = { tfExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Prawda") },
                                    onClick = {
                                        correctAnswer = "True"
                                        tfExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fałsz") },
                                    onClick = {
                                        correctAnswer = "False"
                                        tfExpanded = false
                                    }
                                )
                            }
                        }
                    } else { // open_ended
                        OutlinedTextField(
                            value = correctAnswer,
                            onValueChange = { correctAnswer = it },
                            label = { Text("Poprawna odpowiedź") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (questionText.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Treść pytania jest wymagana")
                                }
                                return@Button
                            }
                            if (questionType == "multiple_choice") {
                                if (options.size < 2) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Wymagane co najmniej 2 opcje dla pytania wielokrotnego wyboru")
                                    }
                                    return@Button
                                }
                                if (options.values.any { it.isBlank() }) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Wszystkie opcje muszą być wypełnione")
                                    }
                                    return@Button
                                }
                                if (correctAnswer.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Poprawna odpowiedź jest wymagana dla pytania wielokrotnego wyboru")
                                    }
                                    return@Button
                                }
                                val correctKeys = correctAnswer.split(",").map { it.trim() }
                                if (!correctKeys.all { options.containsKey(it) }) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Poprawne odpowiedzi muszą odpowiadać istniejącym opcjom (np. A, B)")
                                    }
                                    return@Button
                                }
                            }
                            if (questionType == "true_false" && correctAnswer !in setOf("True", "False")) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Poprawna odpowiedź musi być 'Prawda' lub 'Fałsz'")
                                }
                                return@Button
                            }
                            if (questionType == "open_ended" && correctAnswer.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Poprawna odpowiedź jest wymagana dla pytania otwartego")
                                }
                                return@Button
                            }
                            viewModel.updateQuestion(
                                QuizQuestion(
                                    id = questionId,
                                    questionText = questionText,
                                    questionType = questionType,
                                    options = options,
                                    correctAnswer = correctAnswer,
                                    quizId = quizId
                                ),
                                onSuccess = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Pytanie zaktualizowane pomyślnie")
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
                }
            }
        }
    }
}