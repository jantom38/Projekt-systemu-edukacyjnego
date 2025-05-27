package com.example.myapplication.Quizy

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.Quiz
import com.example.myapplication.QuizQuestion
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuizScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    // New state for the number of questions to display
    var numberOfQuestionsToDisplay by remember { mutableStateOf("1") } // Default to 1

    // Model danych dla pytania w trakcie edycji
    data class QuizQuestionInput(
        val questionText: String = "",
        val questionType: String = "multiple_choice", // Domyślny typ: wielokrotnego wyboru
        val options: MutableMap<String, String> = mutableMapOf("A" to "", "B" to ""),
        val correctAnswers: List<String> = emptyList() // Lista poprawnych odpowiedzi
    )

    var questions by remember { mutableStateOf<MutableList<QuizQuestionInput>>(mutableListOf()) }

    // Funkcja dodająca nowe pytanie
    fun addQuestion() {
        questions = (questions + QuizQuestionInput()).toMutableList()
    }

    // Funkcja aktualizująca pytanie z resetowaniem pól przy zmianie typu
    fun updateQuestion(index: Int, update: QuizQuestionInput.() -> QuizQuestionInput) {
        questions = questions.mapIndexed { i, q ->
            if (i == index) {
                val updated = q.update()
                if (updated.questionType != q.questionType) {
                    when (updated.questionType) {
                        "multiple_choice" -> updated.copy(
                            options = mutableMapOf("A" to "", "B" to ""),
                            correctAnswers = emptyList()
                        )
                        "true_false" -> updated.copy(
                            options = mutableMapOf("True" to "Prawda", "False" to "Fałsz"),
                            correctAnswers = listOf("True")
                        )
                        else -> updated.copy(options = mutableMapOf(), correctAnswers = emptyList())
                    }
                } else {
                    updated
                }
            } else q
        }.toMutableList()
    }

    // Funkcja dodająca nową opcję
    fun addOption(index: Int) {
        updateQuestion(index) {
            val newOptions = options.toMutableMap()
            val newKey = ('A' + options.size).toString()
            newOptions[newKey] = ""
            copy(options = newOptions)
        }
    }

    // Funkcja usuwająca opcję
    fun removeOption(index: Int, key: String) {
        updateQuestion(index) {
            val newOptions = options.toMutableMap()
            newOptions.remove(key)
            val reIndexedOptions = newOptions.entries.mapIndexed { i, entry ->
                ('A' + i).toString() to entry.value
            }.toMap().toMutableMap()
            val newCorrectAnswers = correctAnswers.filter { it != key && reIndexedOptions.keys.contains(it) }
            copy(options = reIndexedOptions, correctAnswers = newCorrectAnswers)
        }
    }

    // Funkcja przełączająca poprawną odpowiedź
    fun toggleCorrectAnswer(index: Int, key: String) {
        updateQuestion(index) {
            val newCorrectAnswers = if (correctAnswers.contains(key)) {
                correctAnswers - key
            } else {
                correctAnswers + key
            }
            copy(correctAnswers = newCorrectAnswers)
        }
    }

    // Lista typów pytań do wyboru
    val questionTypes = listOf(
        "multiple_choice" to "Wielokrotnego wyboru",
        "true_false" to "Prawda/Fałsz",
        "open_ended" to "Otwarte"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dodaj quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addQuestion() }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj pytanie")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł quizu") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis quizu") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = numberOfQuestionsToDisplay,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            numberOfQuestionsToDisplay = newValue
                        }
                    },
                    label = { Text("Liczba pytań do wyświetlenia (losowo)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Corrected KeyboardOptions usage
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    text = "Pytania",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            items(questions.size) { index ->
                val question = questions[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = question.questionText,
                            onValueChange = {
                                updateQuestion(index) { copy(questionText = it) }
                            },
                            label = { Text("Treść pytania") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Wybór typu pytania
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = questionTypes.find { it.first == question.questionType }?.second ?: "",
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
                                            updateQuestion(index) { copy(questionType = type.first) }
                                            expanded = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Warunkowe renderowanie pól w zależności od typu pytania
                        when (question.questionType) {
                            "multiple_choice" -> {
                                // Dynamiczne pola dla opcji z checkboxami
                                question.options.forEach { (key, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = question.correctAnswers.contains(key),
                                            onCheckedChange = { toggleCorrectAnswer(index, key) }
                                        )
                                        OutlinedTextField(
                                            value = value,
                                            onValueChange = { newValue ->
                                                updateQuestion(index) {
                                                    val newOptions = options.toMutableMap()
                                                    newOptions[key] = newValue
                                                    copy(options = newOptions)
                                                }
                                            },
                                            label = { Text("Opcja $key") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { removeOption(index, key) },
                                            enabled = question.options.size > 2 // Minimum 2 opcje
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Usuń opcję")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                // Przycisk do dodawania nowej opcji
                                Button(
                                    onClick = { addOption(index) },
                                    enabled = question.options.size < 10 // Maksimum 10 opcji
                                ) {
                                    Text("Dodaj opcję")
                                }
                            }
                            "true_false" -> {
                                // Stałe opcje dla prawda/fałsz z checkboxami
                                question.options.forEach { (key, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = question.correctAnswers.contains(key),
                                            onCheckedChange = { toggleCorrectAnswer(index, key) }
                                        )
                                        OutlinedTextField(
                                            value = value,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Opcja $key") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            "open_ended" -> {
                                // Pytanie otwarte
                                OutlinedTextField(
                                    value = question.correctAnswers.joinToString(", "),
                                    onValueChange = { newValue ->
                                        updateQuestion(index) {
                                            copy(correctAnswers = newValue.split(",").map { it.trim() }.filter { it.isNotBlank() })
                                        }
                                    },
                                    label = { Text("Poprawne odpowiedzi (oddzielone przecinkami)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Tytuł quizu jest wymagany")
                            }
                            return@Button
                        }
                        // Validate numberOfQuestionsToDisplay
                        val numQuestions = numberOfQuestionsToDisplay.toIntOrNull()
                        if (numQuestions == null || numQuestions <= 0) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Liczba pytań do wyświetlenia musi być liczbą całkowitą większą od 0")
                            }
                            return@Button
                        }

                        coroutineScope.launch {
                            try {
                                val api = RetrofitClient.getInstance(context)
                                val quiz = Quiz(
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                    numberOfQuestionsToDisplay = numQuestions // Pass the new field
                                )
                                Log.d("AddQuiz", "Wysyłanie quizu: $quiz dla kursu ID: $courseId")
                                val quizResponse = api.createQuiz(courseId, quiz)
                                Log.d("AddQuiz", "Odpowiedź serwera dla quizu: ${quizResponse.code()}, body: ${quizResponse.body()}")
                                if (quizResponse.isSuccessful) {
                                    val quizResponseBody = quizResponse.body()
                                    if (quizResponseBody?.success == true && quizResponseBody.quiz.id != null) {
                                        val quizId = quizResponseBody.quiz.id!!
                                        Log.d("AddQuiz", "Quiz zapisany, ID: $quizId, zapisuję pytania...")
                                        if (questions.isEmpty()) {
                                            Log.d("AddQuiz", "Brak pytań do zapisania")
                                            snackbarHostState.showSnackbar("Quiz zapisany pomyślnie (bez pytań)")
                                            navController.popBackStack()
                                            return@launch
                                        }
                                        questions.forEachIndexed { index, q ->
                                            if (q.questionText.isBlank()) {
                                                snackbarHostState.showSnackbar("Pytanie ${index + 1} ma pustą treść")
                                                return@launch
                                            }
                                            if (q.questionType == "multiple_choice") {
                                                if (q.options.values.any { it.isBlank() }) {
                                                    snackbarHostState.showSnackbar("Pytanie ${index + 1} ma puste opcje")
                                                    return@launch
                                                }
                                                if (q.correctAnswers.isEmpty()) {
                                                    snackbarHostState.showSnackbar("Pytanie ${index + 1} musi mieć co najmniej jedną poprawną odpowiedź")
                                                    return@launch
                                                }
                                            }
                                            if (q.questionType == "true_false" && q.correctAnswers.isEmpty()) {
                                                snackbarHostState.showSnackbar("Pytanie ${index + 1} musi mieć co najmniej jedną poprawną odpowiedź")
                                                return@launch
                                            }
                                            if (q.questionType == "open_ended" && q.correctAnswers.isEmpty()) {
                                                snackbarHostState.showSnackbar("Pytanie ${index + 1} musi mieć co najmniej jedną poprawną odpowiedź")
                                                return@launch
                                            }
                                            val questionToSave = QuizQuestion(
                                                questionText = q.questionText,
                                                questionType = q.questionType,
                                                options = if (q.questionType in listOf(
                                                        "multiple_choice",
                                                        "true_false"
                                                    )
                                                ) q.options else emptyMap(),
                                                correctAnswer = q.correctAnswers.joinToString(","),
                                                quizId = quizId
                                            )
                                            Log.d("AddQuiz", "Wysyłanie pytania $index: $questionToSave dla quizId: $quizId")
                                            val questionResponse = api.createQuizQuestion(quizId, questionToSave)
                                            Log.d("AddQuiz", "Odpowiedź serwera dla pytania $index: ${questionResponse.code()}, body: ${questionResponse.body()}")
                                            if (!questionResponse.isSuccessful) {
                                                snackbarHostState.showSnackbar("Błąd zapisu pytania ${index + 1}: ${questionResponse.code()}")
                                                Log.e("AddQuiz", "Błąd zapisu pytania $index: ${questionResponse.errorBody()?.string()}")
                                                return@launch
                                            }
                                        }
                                        snackbarHostState.showSnackbar("Quiz i pytania zapisane pomyślnie")
                                        navController.popBackStack()
                                    } else {
                                        snackbarHostState.showSnackbar("Błąd: Nie udało się pobrać ID quizu lub nieudane zapisanie")
                                        Log.e("AddQuiz", "Nieprawidłowa odpowiedź serwera: $quizResponseBody")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Błąd zapisu quizu: ${quizResponse.code()}")
                                    Log.e("AddQuiz", "Błąd HTTP przy zapisie quizu: ${quizResponse.code()}, error: ${quizResponse.errorBody()?.string()}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Błąd: ${e.message}")
                                Log.e("AddQuiz", "Błąd zapisu quizu", e)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zapisz quiz")
                }
            }
        }
    }
}