// app/src/main/java/com/example/myapplication/ui/MenuScreen.kt
package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MenuScreen(navController: NavHostController) {
    val context = LocalContext.current

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Witaj!", style = MaterialTheme.typography.headlineMedium)

            Button(
                onClick = { navController.navigate("available_courses") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lista dostępnych kursów")
            }

            Button(
                onClick = { navController.navigate("my_courses") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Twoje kursy")
            }

            // --- nowy przycisk Wyloguj ---
            Button(
                onClick = {
                    // 1. Usuń token z SharedPreferences
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .remove("jwt_token")
                        .apply()
                    // 2. Wróć do ekranu logowania i wyczyść historię
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Wyloguj", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}
