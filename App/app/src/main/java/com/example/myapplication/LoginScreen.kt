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
                authenticateUser(username, password, onSuccess = {
                    isLoading = false
                    navController.navigate("courses")
                }, onError = { errorMessage ->
                    isLoading = false
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                })
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
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Adres serwera - zmień na odpowiedni IP swojego serwera
            val serverUrl = "http://10.0.2.2:8080/login" // 10.0.2.2 to localhost w emulatorze

            val loginRequest = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Wysłanie danych
            connection.outputStream.use { outputStream ->
                outputStream.write(loginRequest.toString().toByteArray())
            }

            // Sprawdzenie odpowiedzi
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    // Odczytanie odpowiedzi
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(response)

                    if (responseJson.getBoolean("success")) {
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError(responseJson.getString("message"))
                        }
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    val errorResponse = connection.errorStream.bufferedReader().use { it.readText() }
                    val errorJson = JSONObject(errorResponse)
                    withContext(Dispatchers.Main) {
                        onError(errorJson.getString("error"))
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        onError("Błąd serwera: ${connection.responseCode}")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Błąd połączenia: ${e.localizedMessage}")
            }
        }
    }
}