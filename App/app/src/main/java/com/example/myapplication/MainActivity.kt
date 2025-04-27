package com.example.myapplication
import androidx.compose.runtime.Composable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.myapplication.AccessKeyScreen
import com.example.myapplication.LoginScreen
import com.example.myapplication.CourseListScreen
import com.example.myapplication.CourseFilesScreen
//import com.example.myapplication.CourseDetailScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EduApp()
                }
            }
        }
    }
}
@Composable
fun EduApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("courses") { CourseListScreen(navController) }
        composable("user") { UserScreen(navController) }
        composable("teacher") { TeacherScreen(navController) }

        // Wspólne trasy dla wszystkich użytkowników
        composable("access_key/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toLongOrNull()
            if (courseId != null) {
                AccessKeyScreen(
                    navController = navController,
                    courseId = courseId,
                    onSuccess = {
                        // Po poprawnym wprowadzeniu klucza przejdź do plików
                        navController.navigate("course_files/$courseId") {
                            popUpTo("user") { inclusive = false }
                        }
                    }
                )
            }
        }

        composable("course_files/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toLongOrNull()
            if (courseId != null) {
                CourseFilesScreen(navController, courseId)
            }
        }
    }
}