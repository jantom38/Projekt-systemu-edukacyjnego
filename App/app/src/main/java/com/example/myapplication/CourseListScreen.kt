package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

val sampleCourses = listOf(
    "Angielski dla początkujących",
    "Matematyka 8 klasa",
    "Podstawy programowania",
    "Historia Polski"
)

@Composable
fun CourseListScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dostępne kursy", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(sampleCourses.size) { index ->
                val course = sampleCourses[index]
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate("course/$index")
                        }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(course, style = MaterialTheme.typography.titleMedium)
                        Text("Kliknij, aby przejść do kursu")
                    }
                }
            }
        }
    }
}
