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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccessKeyScreen(navController: NavHostController, courseId: Long) {
    val context = LocalContext.current
    var accessKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wprowadź klucz dostępu do kursu",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = accessKey,
            onValueChange = { accessKey = it },
            label = { Text("Klucz dostępu") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                isLoading = true
                verifyAccessKey(
                    context = context,
                    courseId = courseId,
                    accessKey = accessKey,
                    onSuccess = {
                        isLoading = false
                        navController.navigate("course_files/$courseId") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        }
                    },
                    onError = { message ->
                        isLoading = false
                        errorMessage = message
                    }
                )
            },
            enabled = !isLoading && accessKey.isNotBlank()
        ) {
            Text("Zweryfikuj klucz")
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

private fun verifyAccessKey(
    context: Context,
    courseId: Long,
    accessKey: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val apiService = RetrofitClient.getInstance(context)
            val response = apiService.verifyAccessKey(courseId, mapOf("accessKey" to accessKey))
            if (response["success"] == true) {
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                withContext(Dispatchers.Main) { onError(response["message"] as String) }
            }
        } catch (e: retrofit2.HttpException) {
            withContext(Dispatchers.Main) {
                onError(when (e.code()) {
                    403 -> "Nieprawidłowy klucz dostępu"
                    404 -> "Kurs nie znaleziony"
                    else -> "Błąd serwera: ${e.message()}"
                })
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Błąd połączenia: ${e.message ?: "Nieznany błąd"}")
            }
        }
    }
}