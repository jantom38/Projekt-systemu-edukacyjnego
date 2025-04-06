package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CourseDetailScreen(courseId: String) {
    val courseName = sampleCourses.getOrElse(courseId.toInt()) { "Nieznany kurs" }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(courseName, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Opis kursu: \nTen kurs pomoże Ci nauczyć się podstaw. Składa się z lekcji, quizów i ćwiczeń.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
