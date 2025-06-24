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

/**
 * @file MenuScreen.kt
 * Ten plik definiuje kompozycyjny ekran menu głównego aplikacji.
 * Wyświetla różne opcje nawigacji w zależności od roli zalogowanego użytkownika (ADMIN, TEACHER, STUDENT)
 * oraz przycisk wylogowania.
 */

/**
 * Kompozycyjny ekran menu głównego.
 * Prezentuje opcje dostępne dla użytkownika na podstawie jego roli.
 * Umożliwia nawigację do innych sekcji aplikacji lub wylogowanie.
 *
 * @param navController Kontroler nawigacji do obsługi przejść między ekranami.
 */
@Composable
fun MenuScreen(navController: NavHostController) {
    /** Kontekst aplikacji.*/
    val context = LocalContext.current
    /** Obiekt do zarządzania preferencjami współdzielonymi.*/
    val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    /** Rola zalogowanego użytkownika, pobrana z SharedPreferences. Domyślnie pusta.*/
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

            /** Warunkowe wyświetlanie przycisków w zależności od roli użytkownika.*/
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
            } else if (userRole == "TEACHER") {
                Button(
                    onClick = {
                        Log.d("MenuScreen", "Kliknięto 'Moje kursy (Nauczyciel)', rola: $userRole")
                        navController.navigate("my_courses")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Moje kursy")
                }
                Button(
                    onClick = { navController.navigate("available_courses") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lista dostępnych kursów")
                }
                Button(
                    onClick = { navController.navigate("create_course") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Utwórz kurs")
                }
                Button(
                    onClick = { navController.navigate("my_quizzes") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Moje quizy")
                }
            } else { // Rola STUDENT
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

            /** Przycisk wylogowania, dostępny dla wszystkich ról.*/
            Button(
                onClick = {
                    // Usuń token JWT i rolę z SharedPreferences
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .remove("jwt_token")
                        .remove("user_role")
                        .apply()
                    // Nawiguj do ekranu logowania i usuń wszystkie ekrany z back stacku
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