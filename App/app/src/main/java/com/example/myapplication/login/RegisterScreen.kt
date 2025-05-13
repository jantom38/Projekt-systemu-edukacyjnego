//package com.example.myapplication.login
//
//import android.content.Context
//import android.widget.Toast
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import com.example.myapplication.RegisterRequest
//import com.example.myapplication.RetrofitClient
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//@Composable
//fun RegisterScreen(navController: NavHostController) {
//    var username by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var roleCode by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(false) }
//    val context = LocalContext.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = "Rejestracja",
//            style = MaterialTheme.typography.headlineMedium
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = username,
//            onValueChange = { username = it },
//            label = { Text("Login") },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Hasło") },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        OutlinedTextField(
//            value = roleCode,
//            onValueChange = { roleCode = it },
//            label = { Text("Kod rejestracji") },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(
//            onClick = {
//                if (username.isBlank() || password.isBlank() || roleCode.isBlank()) {
//                    Toast.makeText(context, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
//                    return@Button
//                }
//
//                isLoading = true
//                registerUser(
//                    context = context,
//                    username = username,
//                    password = password,
//                    roleCode = roleCode,
//                    onSuccess = { message ->
//                        isLoading = false
//                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//                        navController.navigate("login") {
//                            popUpTo("register") { inclusive = true }
//                        }
//                    },
//                    onError = { errorMessage ->
//                        isLoading = false
//                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
//                    }
//                )
//            },
//            enabled = !isLoading,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            if (isLoading) {
//                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
//            } else {
//                Text("Zarejestruj się")
//            }
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        TextButton(
//            onClick = { navController.popBackStack() },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Wróć do logowania")
//        }
//    }
//}
//
//private fun registerUser(
//    context: Context,
//    username: String,
//    password: String,
//    roleCode: String,
//    onSuccess: (String) -> Unit,
//    onError: (String) -> Unit
//) {
//    CoroutineScope(Dispatchers.IO).launch {
//        try {
//            val api = RetrofitClient.getInstance(context)
//            val request = RegisterRequest(username, password, roleCode)
//            val response = api.register(request)
//
//            withContext(Dispatchers.Main) {
//                if (response.isSuccessful && response.body()?.success == true) {
//                    onSuccess(response.body()?.message ?: "Rejestracja pomyślna")
//                } else {
//                    val errorMessage = response.body()?.message ?: response.errorBody()?.string() ?: "Błąd rejestracji"
//                    onError(errorMessage)
//                }
//            }
//        } catch (e: Exception) {
//            withContext(Dispatchers.Main) {
//                onError("Błąd połączenia: ${e.message ?: "Nieznany błąd"}")
//            }
//        }
//    }
//}