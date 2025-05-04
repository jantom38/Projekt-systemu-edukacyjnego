// AccessKeyScreen.kt
package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun AccessKeyScreen(
    navController: NavHostController,
    courseId: Long,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var accessKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var alreadyEnrolled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val response = RetrofitClient.getInstance(context).getUserCourses(
                "Bearer " + context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("jwt_token", "")!!
            )
            if (response.isSuccessful) {
                val courses = response.body()?.courses ?: emptyList()
                if (courses.any { it.id == courseId }) {
                    alreadyEnrolled = true
                    snackbarHost.showSnackbar("Jesteś już zapisany do tego kursu.")
                    onSuccess()
                }
            } else {
                snackbarHost.showSnackbar("Błąd przy sprawdzaniu kursów użytkownika.")
            }
        } catch (e: Exception) {
            snackbarHost.showSnackbar("Błąd sieci: ${e.localizedMessage}")
        } finally {
            isLoading = false
        }
    }

    if (alreadyEnrolled) return

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = accessKey,
                onValueChange = { accessKey = it },
                label = { Text("Klucz dostępu") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val resp: Map<String, Any> =
                                RetrofitClient.getInstance(context)
                                    .verifyAccessKey(
                                        courseId,
                                        mapOf("accessKey" to accessKey)
                                    )
                            val success = resp["success"] as? Boolean ?: false
                            val msg = resp["message"] as? String ?: "Nieznana odpowiedź"

                            withContext(Dispatchers.Main) {
                                snackbarHost.showSnackbar(msg)
                                if (success) {
                                    onSuccess()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                snackbarHost.showSnackbar("Błąd: ${e.localizedMessage}")
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = accessKey.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                } else {
                    Text("Potwierdź klucz")
                }
            }
        }
    }
}
