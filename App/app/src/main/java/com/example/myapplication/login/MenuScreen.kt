package com.example.myapplication.login

import android.content.Context
import android.util.Log
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
    val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    val userRole = sharedPreferences.getString("user_role", "") ?: ""
    Log.d("MenuScreen", "Odczytana rola: $userRole")

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

            if (userRole == "ADMIN") {
                Button(
                    onClick = {
                        Log.d("MenuScreen", "Kliknięto 'Panel administracyjny', rola: $userRole")
                        navController.navigate("admin_teacher")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Panel administracyjny")
                }
            } else {
                Button(
                    onClick = { navController.navigate("available_courses") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lista dostępnych kursów")
                }
                Button(
                    onClick = {
                        Log.d("MenuScreen", "Kliknięto 'Moje kursy', rola: $userRole")
                        navController.navigate("my_courses")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Moje kursy")
                }
            }

            Button(
                onClick = {
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .remove("jwt_token")
                        .remove("user_role")
                        .apply()
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