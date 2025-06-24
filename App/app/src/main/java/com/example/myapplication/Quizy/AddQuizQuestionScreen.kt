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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.QuizQuestion
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * @file AddQuizQuestionScreen.kt
 * Plik zawiera funkcję Composable dla ekranu dodawania nowych pytań do quizu oraz powiązany ViewModel.
 */

/**
 * ViewModel dla ekranu AddQuizQuestionScreen.
 *
 * Ten ViewModel zarządza logiką tworzenia nowych pytań do quizu poprzez interakcję z API.
 *
 * @param context Kontekst aplikacji.
 * @param quizId Identyfikator quizu, do którego zostanie dodane pytanie.
 */
class AddQuizQuestionViewModel(context: Context, private val quizId: Long) : ViewModel() {
    private val apiService = RetrofitClient.getInstance(context)

    /**
     * Tworzy nowe pytanie do quizu.
     *
     * @param question Obiekt [QuizQuestion] do utworzenia.
     * @param onSuccess Funkcja zwrotna wywoływana po pomyślnym utworzeniu pytania.
     * @param onError Funkcja zwrotna wywoływana w przypadku błędu podczas tworzenia pytania, z komunikatem błędu.
     */
    fun createQuestion(question: QuizQuestion, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("AddQuizQuestionViewModel", "Tworzenie pytania dla quizu ID: $quizId")
                val response = apiService.createQuizQuestion(quizId, question)
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("AddQuizQuestionViewModel", "Pytanie utworzone pomyślnie")
                    onSuccess()
                } else {
                    val errorMessage = response.body()?.message ?: "Błąd serwera: ${response.code()}"
                    Log.e("AddQuizQuestionViewModel", "Nie udało się utworzyć pytania: $errorMessage")
                    onError(errorMessage)
                }
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Brak autoryzacji"
                    403 -> "Brak uprawnień do dodania pytania"
                    404 -> "Quiz nie znaleziony"
                    else -> "Błąd serwera: ${e.code()}"
                }
                Log.e("AddQuizQuestionViewModel", "Błąd HTTP podczas tworzenia pytania", e)
                onError(errorMessage)
            } catch (e: Exception) {
                val errorMessage = "Błąd połączenia: ${e.message ?: "Nieznany błąd"}"
                Log.e("AddQuizQuestionViewModel", "Błąd sieciowy podczas tworzenia pytania", e)
                onError(errorMessage)
            }
        }
    }
}

/**
 * Funkcja Composable dla ekranu dodawania pytań do quizu.
 *
 * Ten ekran umożliwia użytkownikom dodawanie nowych pytań do określonego quizu.
 * Użytkownicy mogą wprowadzić treść pytania, wybrać typ pytania (wielokrotnego wyboru, prawda/fałsz, otwarte),
 * określić opcje (dla pytań wielokrotnego wyboru i prawda/fałsz) oraz wskazać poprawną odpowiedź(-i).
 *
 * @param navController Kontroler nawigacji do przechodzenia między ekranami.
 * @param quizId Identyfikator quizu, do którego zostanie dodane pytanie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuizQuestionScreen(navController: NavHostController, quizId: Long) {
    val context = LocalContext.current
    val viewModel: AddQuizQuestionViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddQuizQuestionViewModel(context, quizId) as T
        }
    })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var questionText by remember { mutableStateOf("") }
    var questionType by remember { mutableStateOf("multiple_choice") }
    var correctAnswer by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(mapOf<String, String>("A" to "", "B" to "")) }
    var expanded by remember { mutableStateOf(false) } // Dla menu rozwijanego typu pytania
    var tfExpanded by remember { mutableStateOf(false) } // Dla menu rozwijanego prawda/fałsz

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dodaj pytanie") },
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
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                label = { Text("Treść pytania") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Wielokrotnego wyboru") },
                        onClick = {
                            questionType = "multiple_choice"
                            options = mapOf("A" to "", "B" to "")
                            correctAnswer = ""
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Prawda/Fałsz") },
                        onClick = {
                            questionType = "true_false"
                            options = mapOf("True" to "Prawda", "False" to "Fałsz") // Ustawia predefiniowane opcje dla prawda/fałsz
                            correctAnswer = "" // Czyści poprzednią poprawną odpowiedź
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Otwarte") },
                        onClick = {
                            questionType = "open_ended"
                            options = emptyMap()
                            correctAnswer = ""
                            expanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
                                    // Usuń z poprawnych odpowiedzi, jeśli była wybrana
                                    correctAnswer = correctAnswer.split(",").filter { it != key }.joinToString(",")
                                },
                                enabled = options.size > 2 // Minimum 2 opcje
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
                            enabled = options.size < 10 // Maksimum 10 opcji
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
                // Menu rozwijane dla poprawnych odpowiedzi prawda/fałsz
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
                    viewModel.createQuestion(
                        QuizQuestion(
                            questionText = questionText,
                            questionType = questionType,
                            options = if (questionType in listOf("multiple_choice", "true_false")) options else null, // Zapewnia przekazanie opcji dla prawda/fałsz
                            correctAnswer = correctAnswer,
                            quizId = quizId
                        ),
                        onSuccess = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Pytanie dodane pomyślnie")
                                navController.navigate("edit_quiz/${quizId}")
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
                Text("Dodaj pytanie")
            }
        }
    }
}