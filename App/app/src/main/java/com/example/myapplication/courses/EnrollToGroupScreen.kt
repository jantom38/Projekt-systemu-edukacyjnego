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
 * @file EnrollToGroupScreen.kt
 * Ten plik definiuje ekran zapisu do grupy kursu.
 */

/**
 * Kompozycyjny ekran umożliwiający zapisanie się na kurs grupowy.
 * Użytkownik wprowadza klucz dostępu, aby dołączyć do kursu.
 *
 * @param navController Kontroler nawigacji do obsługi przejść między ekranami.
 * @param courseGroupId Identyfikator grupy kursu, do której użytkownik chce dołączyć.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollToGroupScreen(navController: NavHostController, courseGroupId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    /** Klucz dostępu wprowadzony przez użytkownika.*/
    var accessKey by remember { mutableStateOf("") }
    /** Stan ładowania danych. True, jeśli trwa zapisywanie, w przeciwnym razie false.*/
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Zapisz się na kurs") })
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
            Text(
                "Wprowadź klucz dostępu otrzymany od prowadzącego, aby zapisać się na kurs.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = accessKey,
                onValueChange = { accessKey = it },
                label = { Text("Klucz dostępu") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    /** Walidacja klucza dostępu.*/
                    if (accessKey.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Wprowadź klucz dostępu") }
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            val api = RetrofitClient.getInstance(context)
                            /** Wywołanie endpointu zapisu na kurs.*/
                            val response = api.enrollInCourseGroup(courseGroupId, mapOf("accessKey" to accessKey))
                            if (response.isSuccessful && response.body()?.success == true) {
                                snackbarHostState.showSnackbar(response.body()?.message ?: "Zapisano na kurs!")
                                /** Powrót do menu głównego po pomyślnym zapisie.*/
                                navController.popBackStack("menu", inclusive = false)
                            } else {
                                snackbarHostState.showSnackbar(response.body()?.message ?: "Nieprawidłowy klucz lub błąd serwera.")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Błąd: ${e.message}")
                            Log.e("EnrollToGroup", "Błąd zapisu", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                /** Wyświetlanie wskaźnika ładowania, jeśli trwa zapisywanie.*/
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Zapisz się")
                }
            }
        }
    }
}