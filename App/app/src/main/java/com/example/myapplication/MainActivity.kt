package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.myapplication.Quizy.AddQuizScreen
import com.example.myapplication.Quizy.QuizResultScreen
import com.example.myapplication.Quizy.SolveQuizScreen
import com.example.myapplication.courses.AccessKeyScreen
import com.example.myapplication.courses.CourseDetailsScreen
import com.example.myapplication.courses.CourseListScreen
import com.example.myapplication.courses.CourseUsersScreen
import com.example.myapplication.courses.MyCoursesScreen
import com.example.myapplication.files.CourseFilesScreen
import com.example.myapplication.files.ManageFilesScreen
import com.example.myapplication.login.LoginScreen
import com.example.myapplication.login.MenuScreen
import com.example.myapplication.login.RegisterScreen
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
        composable("register") { RegisterScreen(navController) }
        composable("courses") { CourseListScreen(navController) }
        composable("user") { UserScreen(navController) }
        composable("teacher") { TeacherScreen(navController) }
        composable("admin") { AdminScreen(navController) }
        composable("add_course") { AddCourseScreen(navController) }
        composable(
            "manage_files/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            ManageFilesScreen(navController = navController, courseId = courseId)
        }
        composable("my_courses") { MyCoursesScreen(navController) }
        composable(
            "course_files/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            CourseFilesScreen(navController, courseId)
        }
        composable(
            "access_key/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            AccessKeyScreen(navController, courseId = courseId) {
                navController.popBackStack("menu", false)
            }
        }
        composable(
            "manage_users/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStack ->
            val courseId = backStack.arguments!!.getLong("courseId")
            CourseUsersScreen(navController, courseId)
        }
        composable("menu") { MenuScreen(navController) }
        composable("available_courses") { UserScreen(navController) }
        composable(
            "course_details/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            CourseDetailsScreen(navController, courseId)
        }
        composable(
            "add_quiz/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            AddQuizScreen(navController, courseId)
        }
        composable(
            "solve_quiz/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            SolveQuizScreen(navController, quizId)
        }
        composable(
            "quiz_result/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStack ->
            val quizId = backStack.arguments!!.getLong("quizId")
            QuizResultScreen(quizId, navController)
        }
        composable(
            "quiz_stats/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            TeacherQuizStatsScreen(navController, courseId)
        }
        composable(
            "quiz_results/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            TeacherQuizResultsScreen(navController, quizId)
        }
    }
}