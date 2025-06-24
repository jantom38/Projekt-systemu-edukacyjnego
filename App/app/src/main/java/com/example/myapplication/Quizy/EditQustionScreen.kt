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
import com.example.myapplication.QuizQuestion
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * @file EditQustionScreen.kt
 * Ten plik zawiera funkcję kompozycyjną do edycji pojedynczych pytań quizu i związany z nią ViewModel.
 */

/**
 * ViewModel dla EditQuestionScreen.
 *
 * Ten ViewModel obsługuje pobieranie i aktualizowanie konkretnego pytania quizu.
 *
 * @param context Kontekst aplikacji.
 * @param quizId ID quizu, do którego należy pytanie.
 * @param questionId ID pytania do edycji.
 */
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

    /**
     * Ładuje szczegóły konkretnego pytania quizu z API.
     *
     * Najpierw pobiera cały quiz, aby znaleźć konkretne pytanie.
     * Aktualizuje stany [_question], [_isLoading] i [_error] na podstawie odpowiedzi API.
     */
    fun loadQuestion() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Pobierz cały quiz, aby znaleźć konkretne pytanie
                val response = apiService.getQuizForEdit(quizId)
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
                Log.e("EditQuestionViewModel", "Błąd HTTP", e)
            } catch (e: Exception) {
                _error.value = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("EditQuestionViewModel", "Błąd sieciowy", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Aktualizuje istniejące pytanie quizu.
     *
     * @param question Obiekt [QuizQuestion] ze zaktualizowanymi szczegółami.
     * @param onSuccess Callback, który zostanie wywołany po pomyślnym zaktualizowaniu pytania.
     * @param onError Callback, który zostanie wywołany, gdy wystąpi błąd podczas aktualizacji pytania, dostarczając komunikat o błędzie.
     */
    fun updateQuestion(question: QuizQuestion, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("EditQuestionViewModel", "Updating question ID: $questionId")
                // Zakładając, że masz endpoint updateQuizQuestion w ApiService
                // Jeśli nie, musisz dodać go do ApiClient.kt i MainControllers.java
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
                    404 -> "Pytanie nie znaleziono"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("EditQuestionViewModel", "Błąd HTTP podczas aktualizacji pytania ID: $questionId", e)
                onError(errorMessage)
            } catch (e: Exception) {
                val errorMessage = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("EditQuestionViewModel", "Błąd sieciowy podczas aktualizacji pytania ID: $questionId", e)
                onError(errorMessage)
            }
        }
    }
}

/**
 * Funkcja kompozycyjna dla ekranu edycji pytania.
 *
 * Ten ekran umożliwia użytkownikom modyfikowanie szczegółów konkretnego pytania quizu,
 * w tym jego tekstu, typu, opcji (dla pytań wielokrotnego wyboru/prawda-fałsz) i poprawnej odpowiedzi(-i).
 *
 * @param navController NavHostController do nawigacji między ekranami.
 * @param quizId ID quizu, do którego należy pytanie.
 * @param questionId ID pytania do edycji.
 */
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
    var expanded by remember { mutableStateOf(false) } // Dla rozwijanej listy typu pytania
    var tfExpanded by remember { mutableStateOf(false) } // Dla rozwijanej listy Prawda/Fałsz

    // Lista typów pytań dla rozwijanej listy
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
            // Specjalna obsługa dla true_false, aby upewnić się, że opcje są poprawnie ustawione
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
                viewModel.question.value != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                label = { Text("Treść pytania") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = when (questionType) {
                                        "multiple_choice" -> "Wielokrotnego wyboru"
                                        "true_false" -> "Prawda/Fałsz"
                                        else -> "Otwarte"
                                    },
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
                                                // Zresetuj opcje i poprawną odpowiedź w zależności od nowego typu
                                                when (questionType) {
                                                    "multiple_choice" -> {
                                                        options = mapOf("A" to "", "B" to "")
                                                        correctAnswer = ""
                                                    }
                                                    "true_false" -> {
                                                        options = mapOf("True" to "Prawda", "False" to "Fałsz")
                                                        correctAnswer = "True" // Domyślna dla Prawda/Fałsz
                                                    }
                                                    "open_ended" -> {
                                                        options = emptyMap()
                                                        correctAnswer = ""
                                                    }
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (questionType == "multiple_choice") {
                            items(options.toList()) { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            correctAnswer = correctAnswer.split(",").filter { it != key }.joinToString(",")
                                        },
                                        enabled = options.size > 2
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
                                    enabled = options.size < 10
                                ) {
                                    Text("Dodaj opcję")
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = correctAnswer,
                                    onValueChange = { correctAnswer = it },
                                    label = { Text("Poprawna odpowiedź (np. A,B)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else if (questionType == "true_false") {
                            item {
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
                            }
                        } else { // open_ended
                            item {
                                OutlinedTextField(
                                    value = correctAnswer,
                                    onValueChange = { correctAnswer = it },
                                    label = { Text("Poprawna odpowiedź") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        item {
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
                                                snackbarHostState.showSnackbar("Wymagane co najmniej 2 opcje")
                                            }
                                            return@Button
                                        }
                                        if (options.any { it.value.isBlank() }) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Wszystkie opcje muszą być wypełnione")
                                            }
                                            return@Button
                                        }
                                        if (correctAnswer.isBlank()) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Poprawna odpowiedź jest wymagana")
                                            }
                                            return@Button
                                        }
                                        val correctAnswers = correctAnswer.split(",").map { it.trim() }
                                        if (correctAnswers.any { !options.containsKey(it) }) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Wszystkie poprawne odpowiedzi muszą być kluczami opcji")
                                            }
                                            return@Button
                                        }
                                    } else if (questionType == "true_false") {
                                        if (correctAnswer !in setOf("True", "False")) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Poprawna odpowiedź musi być 'Prawda' lub 'Fałsz'")
                                            }
                                            return@Button
                                        }
                                    } else if (questionType == "open_ended") {
                                        if (correctAnswer.isBlank()) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Poprawna odpowiedź jest wymagana")
                                            }
                                            return@Button
                                        }
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
    }
}