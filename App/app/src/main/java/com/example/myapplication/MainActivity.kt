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
import com.example.myapplication.Quizy.AddQuizQuestionScreen
import com.example.myapplication.Quizy.AddQuizScreen
import com.example.myapplication.Quizy.EditQuizScreen
import com.example.myapplication.Quizy.EditQuestionScreen
import com.example.myapplication.Quizy.QuizResultScreen
import com.example.myapplication.Quizy.SolveQuizScreen
import com.example.myapplication.admin.SystemUsersScreen
import com.example.myapplication.courses.AccessKeyScreen
import com.example.myapplication.courses.CourseDetailsScreen
import com.example.myapplication.courses.CourseListScreen
import com.example.myapplication.courses.CourseUsersScreen
import com.example.myapplication.courses.EnrollToGroupScreen
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

        // ZMIENIONA NAWIGACJA DLA NAUCZYCIELA I ADMINA
        composable("teacher") { TeacherScreen(navController) } // Nowy ekran zarządzania grupami
        composable("admin_teacher") { AdminTeacherScreen(navController) } // Można to docelowo połączyć z "teacher"

        // ZMIENIONA TRASA - teraz wymaga ID grupy kursu
        composable(
            "add_course/{courseGroupId}",
            arguments = listOf(navArgument("courseGroupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseGroupId = backStackEntry.arguments?.getLong("courseGroupId") ?: return@composable
            AddCourseScreen(navController, courseGroupId)
        }

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
            "enroll_in_group/{courseGroupId}",
            arguments = listOf(navArgument("courseGroupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseGroupId = backStackEntry.arguments?.getLong("courseGroupId") ?: return@composable
            // Upewnij się, że wywołujesz poprawny Composable - EnrollToGroupScreen
            EnrollToGroupScreen(navController, courseGroupId = courseGroupId)
        }
        composable("system_users") { SystemUsersScreen(navController) }
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
            "edit_quiz/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            EditQuizScreen(navController, quizId)
        }
        composable(
            "add_question/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            AddQuizQuestionScreen(navController, quizId)
        }
        composable(
            "edit_question/{quizId}/{questionId}",
            arguments = listOf(
                navArgument("quizId") { type = NavType.LongType },
                navArgument("questionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            val questionId = backStackEntry.arguments?.getLong("questionId") ?: return@composable
            EditQuestionScreen(navController, quizId, questionId)
        }
        composable(
            "solve_quiz/{quizId}/{courseId}", // Dodajemy courseId
            arguments = listOf(
                navArgument("quizId") { type = NavType.LongType },
                navArgument("courseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            SolveQuizScreen(navController, quizId, courseId)
        }
        composable(
            "quiz_result/{quizId}/{courseId}",
            arguments = listOf(
                navArgument("quizId") { type = NavType.LongType },
                navArgument("courseId") { type = NavType.LongType }
            )
        ) { backStack ->
            val quizId = backStack.arguments?.getLong("quizId") ?: return@composable
            val courseId = backStack.arguments?.getLong("courseId") ?: return@composable
            QuizResultScreen(quizId = quizId, courseId = courseId, navController = navController)
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