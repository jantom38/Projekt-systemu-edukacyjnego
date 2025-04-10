package com.example.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun LoginScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Logowanie",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Login") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Wprowadź login i hasło", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                authenticateUser(
                    username = username,
                    password = password,
                    onSuccess = { message ->
                        isLoading = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        navController.navigate("courses")
                    },
                    onError = { errorMessage ->
                        isLoading = false
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Zaloguj się")
            }
        }
    }
}

private fun authenticateUser(
    username: String,
    password: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val serverUrl = "http://10.0.2.2:8080/api/auth/login" // 10.0.2.2 to localhost w emulatorze

            // Przygotowanie danych JSON zgodnie z formatem oczekiwanym przez serwer
            val requestBody = """
                {
                    "username": "$username",
                    "password": "$password"
                }
            """.trimIndent()

            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            // Wysłanie danych
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
                os.flush()
            }

            // Obsługa odpowiedzi
            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream.bufferedReader().use { it.readText() }
            }

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val responseJson = JSONObject(response)
                    if (responseJson.getBoolean("success")) {
                        withContext(Dispatchers.Main) {
                            onSuccess(responseJson.getString("message"))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError(responseJson.getString("message"))
                        }
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        try {
                            val errorJson = JSONObject(response)
                            onError(errorJson.getString("message")
                                ?: "Nieznany błąd: $responseCode")
                        } catch (e: Exception) {
                            onError("Błąd serwera: $responseCode")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Błąd połączenia: ${e.message ?: "Nieznany błąd"}")
            }
        }
    }
}