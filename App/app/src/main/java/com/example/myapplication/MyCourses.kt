// MyCoursesScreen.kt
package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.Course
import com.example.myapplication.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MyCoursesScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val resp: Map<String, Any> =
                    RetrofitClient.getInstance(context).getUserCourses()
                val success = resp["success"] as? Boolean ?: false

                if (success) {
                    @Suppress("UNCHECKED_CAST")
                    val rawList = resp["courses"] as? List<Map<String, Any>> ?: emptyList()
                    courses = rawList.map { m ->
                        Course(
                            id = ((m["id"] as? Double)?.toLong() ?: 0L),
                            courseName = m["courseName"] as? String ?: "",
                            description = m["description"] as? String ?: "",
                            accessKey = m["accessKey"] as? String ?: ""
                        )
                    }
                } else {
                    val msg = resp["message"] as? String ?: "Błąd ładowania"
                    withContext(Dispatchers.Main) {
                        snackbarHost.showSnackbar(msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHost.showSnackbar("Wyjątek: ${e.localizedMessage}")
                }
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(courses) { course ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    navController.navigate("course_files/${course.id}")
                                },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(course.courseName) },
                                supportingContent = { Text(course.description) }
                            )
                        }
                    }
                }
            }
        }
    }
}
