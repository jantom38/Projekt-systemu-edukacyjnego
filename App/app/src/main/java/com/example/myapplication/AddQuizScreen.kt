package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddQuizScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    data class QuizQuestionInput(
        val questionText: String = "",
        val correctAnswer: String = "",
        val optionA: String = "",
        val optionB: String = "",
        val optionC: String = "",
        val optionD: String = ""
    )
    var questions by remember { mutableStateOf<MutableList<QuizQuestionInput>>(mutableListOf()) }

    fun addQuestion() {
        questions = (questions + QuizQuestionInput()).toMutableList()
    }

    fun updateQuestion(index: Int, update: QuizQuestionInput.() -> QuizQuestionInput) {
        questions = questions.mapIndexed { i: Int, q: QuizQuestionInput ->
            if (i == index) q.update() else q
        }.toMutableList()
    }

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
                        OutlinedTextField(
                            value = question.correctAnswer,
                            onValueChange = {
                                updateQuestion(index) { copy(correctAnswer = it) }
                            },
                            label = { Text("Poprawna odpowiedź") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = question.optionA,
                            onValueChange = {
                                updateQuestion(index) { copy(optionA = it) }
                            },
                            label = { Text("Opcja A") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = question.optionB,
                            onValueChange = {
                                updateQuestion(index) { copy(optionB = it) }
                            },
                            label = { Text("Opcja B") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = question.optionC,
                            onValueChange = {
                                updateQuestion(index) { copy(optionC = it) }
                            },
                            label = { Text("Opcja C") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = question.optionD,
                            onValueChange = {
                                updateQuestion(index) { copy(optionD = it) }
                            },
                            label = { Text("Opcja D") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Tytuł quizu jest wymagany")
                            }
                            return@Button
                        }
                        scope.launch {
                            try {
                                val api = RetrofitClient.getInstance(context)
                                val quiz = Quiz(title = title, description = description.takeIf { it.isNotBlank() })
                                val quizResponse = api.createQuiz(courseId, quiz)
                                if (quizResponse.isSuccessful) {
                                    val quizId = (quizResponse.body()?.get("quiz") as? Map<*, *>)?.get("id") as? Double
                                    if (quizId != null) {
                                        questions.forEach { q ->
                                            val question = QuizQuestion(
                                                questionText = q.questionText,
                                                correctAnswer = q.correctAnswer,
                                                optionA = q.optionA,
                                                optionB = q.optionB,
                                                optionC = q.optionC,
                                                optionD = q.optionD
                                            )
                                            val questionResponse = api.createQuizQuestion(quizId.toLong(), question)
                                            if (!questionResponse.isSuccessful) {
                                                snackbarHostState.showSnackbar("Błąd zapisu pytania: ${questionResponse.code()}")
                                                return@launch
                                            }
                                        }
                                    }
                                    snackbarHostState.showSnackbar("Quiz i pytania zapisane pomyślnie")
                                    navController.popBackStack()
                                } else {
                                    snackbarHostState.showSnackbar("Błąd zapisu quizu: ${quizResponse.code()}")
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