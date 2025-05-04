package com.example.myapplication
import androidx.compose.runtime.Composable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
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
        composable("add_course") { AddCourseScreen(navController) }
        composable(
            "manage_files/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            ManageFilesScreen(navController = navController, courseId = courseId)
        }


        composable("my_courses") { MyCoursesScreen(navController) }
        composable("course_files/{courseId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("courseId")!!.toLong()
            CourseFilesScreen(navController, id)
        }
        composable("access_key/{courseId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("courseId")!!.toLong()
            AccessKeyScreen(navController, courseId = id) {
                navController.popBackStack("menu", false)
            }
        }
        composable("menu") {
            MenuScreen(navController)
        }
        composable("available_courses") {
            UserScreen(navController)
        }
        composable("course_files/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toLongOrNull()
            if (courseId != null) {
                CourseFilesScreen(navController, courseId)
            }
        }
    }
}