/**
 * @file AccessKeyScreen.kt
 *  Komponent kompozycyjny UI do weryfikacji klucza dostępu do kursu.
 *
 * Plik zawiera definicję funkcji kompozycyjnej {@link com.example.myapplication.courses.AccessKeyScreen},
 * która tworzy interfejs użytkownika umożliwiający studentom wprowadzenie
 * i zweryfikowanie klucza dostępu wymaganego do dołączenia do kursu.
 * Komponent obsługuje stan UI, interakcje użytkownika oraz komunikację z API.
 */
package com.example.myapplication.courses

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.launch

/**
 * Komponent kompozycyjny ekranu weryfikacji klucza dostępu do kursu.
 * Umożliwia użytkownikowi wprowadzenie klucza dostępu i weryfikację go z serwerem.
 * W przypadku pomyślnej weryfikacji, wywołuje zdefiniowaną akcję `onSuccess`.
 *
 * @param navController Kontroler nawigacji Jetpack Compose do obsługi nawigacji.
 * @param courseId Identyfikator kursu, dla którego weryfikowany jest klucz dostępu.
 * @param onSuccess Funkcja wywoływana po pomyślnej weryfikacji klucza dostępu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessKeyScreen(navController: NavHostController, courseId: Long, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var accessKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Weryfikacja klucza dostępu") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = accessKey,
                onValueChange = { accessKey = it },
                label = { Text("Klucz dostępu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (accessKey.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Klucz dostępu nie może być pusty")
                        }
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            val api = RetrofitClient.getInstance(context)
                            val response = api.verifyAccessKey(courseId, mapOf("accessKey" to accessKey))
                            val success = response["success"] as? Boolean ?: false
                            if (success) {
                                snackbarHostState.showSnackbar("Dostęp przyznany")
                                onSuccess()
                            } else {
                                snackbarHostState.showSnackbar(response["message"] as? String ?: "Nieprawidłowy klucz")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Błąd: ${e.message}")
                            Log.e("AccessKey", "Błąd weryfikacji", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Zweryfikuj")
                }
            }
        }
    }
}