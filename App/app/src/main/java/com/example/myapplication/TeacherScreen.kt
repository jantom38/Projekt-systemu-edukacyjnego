package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TeacherScreen(navController: NavHostController) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var courseName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj kurs")
            Spacer(Modifier.width(8.dp))
            Text("Dodaj kurs")
        }

        // Lista kursów (możesz dodać później)
        CourseListScreen(navController)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Dodaj nowy kurs") },
            text = {
                Column {
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Nazwa kursu*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accessKey,
                        onValueChange = { accessKey = it },
                        label = { Text("Klucz dostępu*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            if (courseName.isBlank() || description.isBlank() || accessKey.isBlank()) {
                                errorMessage = "Wszystkie pola są wymagane"
                                return@Button
                            }

                            addCourse(
                                context = context,
                                name = courseName,
                                description = description,
                                accessKey = accessKey,
                                onSuccess = {
                                    showDialog = false
                                    courseName = ""
                                    description = ""
                                    accessKey = ""
                                    // Odśwież listę kursów
                                    navController.navigate("teacher") {
                                        popUpTo("teacher") { inclusive = true }
                                    }
                                },
                                onError = { message ->
                                    errorMessage = message
                                }
                            )
                        }
                    ) {
                        Text("Dodaj")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }
}

private fun addCourse(
    context: Context,
    name: String,
    description: String,
    accessKey: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("API_DEBUG", "Próba dodania kursu: $name")
            val apiService = RetrofitClient.getInstance(context)
            val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("jwt_token", null)
            Log.d("API_DEBUG", "Token: ${token?.take(10)}...") // Logujemy fragment tokenu

            val response = apiService.createCourse(
                Course(
                    id = 0,
                    courseName = name,
                    description = description,
                    accessKey = accessKey
                )
            )

            Log.d("API_DEBUG", "Odpowiedź: ${response.code()} - ${response.message()}")

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("API_ERROR", "Błąd ${response.code()}: $errorBody")
                    onError("Błąd serwera: ${response.code()} - ${errorBody ?: response.message()}")
                }
            }
        } catch (e: Exception) {
            Log.e("API_EXCEPTION", "Błąd: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError("Błąd połączenia: ${e.message ?: "Nieznany błąd"}")
            }
        }
    }
}