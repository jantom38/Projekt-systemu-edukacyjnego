package com.example.myapplication.login

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.RetrofitClient
import com.example.myapplication.RegisterRequest
import com.example.myapplication.RegisterResponse
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
                    context = context,
                    username = username,
                    password = password,
                    onSuccess = { role, message ->
                        isLoading = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        when (role) {
                            "TEACHER" -> navController.navigate("teacher") {
                                popUpTo("login") { inclusive = true }
                            }
                            else -> navController.navigate("menu") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
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

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zarejestruj się")
        }
    }
}

@Composable
fun RegisterScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var roleCode by remember { mutableStateOf("") }
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
            text = "Rejestracja",
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

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = roleCode,
            onValueChange = { roleCode = it },
            label = { Text("Kod rejestracji") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank() || roleCode.isBlank()) {
                    Toast.makeText(context, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                registerUser(
                    context = context,
                    username = username,
                    password = password,
                    roleCode = roleCode,
                    onSuccess = { message ->
                        isLoading = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
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
                Text("Zarejestruj się")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wróć do logowania")
        }
    }
}

private fun authenticateUser(
    context: Context,
    username: String,
    password: String,
    onSuccess: (role: String, message: String) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        var connection: HttpURLConnection? = null
        try {
            val serverUrl = "http://10.0.2.2:8080/api/auth/login"

            val jsonPayload = JSONObject().apply {
                put("username", username)
                put("password", password)
            }.toString()

            val url = URL(serverUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val responseJson = JSONObject(response)
                    if (responseJson.getBoolean("success")) {
                        val token = responseJson.getString("token")
                        val role = responseJson.getString("role")
                        saveToken(context, token)
                        withContext(Dispatchers.Main) {
                            onSuccess(role, responseJson.getString("message"))
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
                            onError(errorJson.getString("message") ?: "Błąd serwera: $responseCode")
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
        } finally {
            connection?.disconnect()
        }
    }
}

private fun registerUser(
    context: Context,
    username: String,
    password: String,
    roleCode: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val api = RetrofitClient.getInstance(context)
            val request = RegisterRequest(username, password, roleCode)
            val response = api.register(request)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess(response.body()?.message ?: "Rejestracja pomyślna")
                } else {
                    val errorMessage = response.body()?.message ?: response.errorBody()?.string() ?: "Błąd rejestracji"
                    onError(errorMessage)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Błąd połączenia: ${e.message ?: "Nieznany błąd"}")
            }
        }
    }
}

private fun saveToken(context: Context, token: String) {
    val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("jwt_token", token).apply()
}