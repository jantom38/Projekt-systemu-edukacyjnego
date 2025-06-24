package com.example.myapplication.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.RegisterRequest
import com.example.myapplication.RetrofitClient
import com.example.myapplication.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * @file LoginScreen.kt
 * Zawiera komponenty kompozycyjne i funkcje obsługujące ekrany logowania i rejestracji.
 *
 * Ten plik definiuje interfejsy użytkownika dla procesów logowania i rejestracji,
 * a także logikę biznesową związaną z komunikacją z serwerem w celu
 * uwierzytelniania i rejestrowania użytkowników.
 */

/**
 * Komponent kompozycyjny ekranu logowania.
 *
 * Ten komponent wyświetla interfejs użytkownika do logowania, umożliwiając użytkownikom
 * wprowadzenie nazwy użytkownika i hasła. Obsługuje również nawigację do ekranu rejestracji
 * oraz wyświetla wskaźnik ładowania podczas procesu uwierzytelniania.
 * Umożliwia także zmianę adresu IP serwera.
 *
 * @param navController Kontroler nawigacji do obsługi przejść między ekranami.
 */
@Composable
fun LoginScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Stany dla okna dialogowego zmiany IP serwera
    var showServerDialog by remember { mutableStateOf(false) }
    var serverIpInput by remember { mutableStateOf(ServerConfig.getServerIp(context)) }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Zmień adres serwera") },
            text = {
                OutlinedTextField(
                    value = serverIpInput,
                    onValueChange = { serverIpInput = it },
                    label = { Text("Adres IP serwera") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverIpInput.isNotBlank()) {
                            ServerConfig.saveServerIp(context, serverIpInput)
                            Toast.makeText(context, "Adres serwera zaktualizowany", Toast.LENGTH_SHORT).show()
                            showServerDialog = false
                        } else {
                            Toast.makeText(context, "Adres IP nie może być pusty", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = {
                serverIpInput = ServerConfig.getServerIp(context)
                showServerDialog = true
            }) {
                Icon(Icons.Default.Settings, contentDescription = "Zmień adres serwera")
            }
        }

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
            visualTransformation = PasswordVisualTransformation(),
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
                        Log.d("LoginScreen", "Przekierowanie po zalogowaniu, rola: $role")
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
                        Log.e("LoginScreen", "Błąd logowania: $errorMessage")
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

/**
 * Komponent kompozycyjny ekranu rejestracji.
 *
 * Ten komponent wyświetla interfejs użytkownika do rejestracji nowego konta, umożliwiając
 * wprowadzenie nazwy użytkownika, hasła i kodu rejestracji. Obsługuje również nawigację
 * powrotną do ekranu logowania oraz wyświetla wskaźnik ładowania podczas procesu rejestracji.
 *
 * @param navController Kontroler nawigacji do obsługi przejść między ekranami.
 */
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
            visualTransformation = PasswordVisualTransformation(),
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

/**
 * Funkcja do uwierzytelniania użytkownika poprzez wysłanie żądania POST do serwera.
 *
 * Ta funkcja wysyła dane logowania (nazwę użytkownika i hasło) do serwera.
 * W zależności od odpowiedzi serwera wywołuje odpowiednie callbacki.
 * Obsługuje zapis tokena JWT i roli użytkownika po udanym zalogowaniu.
 *
 * @param context Kontekst Androida, używany do wyświetlania Toastów i dostępu do SharedPreferences.
 * @param username Nazwa użytkownika do uwierzytelnienia.
 * @param password Hasło użytkownika do uwierzytelnienia.
 * @param onSuccess Callback wywoływany po udanym uwierzytelnieniu, zwraca rolę i wiadomość.
 * @param onError Callback wywoływany w przypadku błędu uwierzytelniania, zwraca komunikat o błędzie.
 */
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
            val serverUrl = "${ServerConfig.getBaseUrl(context)}api/auth/login"

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

            Log.d("LoginScreen", "Odpowiedź serwera: $response")

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val responseJson = JSONObject(response)
                    if (responseJson.getBoolean("success")) {
                        val token = responseJson.getString("token")
                        val role = responseJson.getString("role")
                        Log.d("LoginScreen", "Zapis roli: $role")
                        saveToken(context, token, role)
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
                Log.e("LoginScreen", "Wyjątek: ${e.message}")
            }
        } finally {
            connection?.disconnect()
        }
    }
}

/**
 * Funkcja do rejestrowania nowego użytkownika poprzez wysłanie żądania POST do serwera.
 *
 * Ta funkcja wysyła dane rejestracyjne (nazwę użytkownika, hasło i kod roli) do serwera
 * za pośrednictwem Retrofit. W zależności od odpowiedzi serwera wywołuje odpowiednie callbacki.
 *
 * @param context Kontekst Androida, używany do wyświetlania Toastów.
 * @param username Nazwa użytkownika do zarejestrowania.
 * @param password Hasło użytkownika do zarejestrowania.
 * @param roleCode Kod roli dla nowego użytkownika.
 * @param onSuccess Callback wywoływany po udanej rejestracji, zwraca wiadomość.
 * @param onError Callback wywoływany w przypadku błędu rejestracji, zwraca komunikat o błędzie.
 */
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

/**
 * Zapisuje token JWT i rolę użytkownika w SharedPreferences.
 *
 * Ta funkcja przechowuje token uwierzytelniający JWT i przypisaną rolę użytkownika
 * w prywatnych preferencjach współdzielonych aplikacji.
 *
 * @param context Kontekst Androida, używany do uzyskania dostępu do SharedPreferences.
 * @param token Token JWT do zapisania.
 * @param role Rola użytkownika do zapisania.
 */
private fun saveToken(context: Context, token: String, role: String) {
    val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit()
        .putString("jwt_token", token)
        .putString("user_role", role)
        .apply()
    Log.d("LoginScreen", "Zapisano w SharedPreferences: token=$token, role=$role")
}