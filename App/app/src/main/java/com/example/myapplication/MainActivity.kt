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

/**
 * @file MainActivity.kt
 *  Główna aktywność aplikacji, konfigurująca nawigację i motyw.
 *
 * Ten plik zawiera klasę MainActivity, która jest punktem wejścia do aplikacji
 * opartej na Jetpack Compose. Odpowiada za ustawienie motywu i hostowanie
 * grafu nawigacyjnego aplikacji.
 */

/**
 *  Główna aktywność aplikacji.
 *
 * Klasa `MainActivity` dziedziczy po `ComponentActivity` i służy jako główny
 * kontener dla interfejsu użytkownika opartego na Jetpack Compose.
 * Ustawia motyw aplikacji i inicjuje nawigację.
 */
class MainActivity : ComponentActivity() {
    /**
     *  Wywoływana, gdy aktywność jest tworzona.
     *
     * Ta metoda inicjuje kompozycję interfejsu użytkownika aplikacji, ustawiając
     * `MyApplicationTheme` i renderując główny komponent `EduApp`.
     *
     * @param savedInstanceState Jeśli aktywność jest ponownie inicjowana po uprzednim
     * zamknięciu, ten pakiet zawiera dane dostarczone ostatnio w [onSaveInstanceState].
     * W przeciwnym razie ma wartość null.
     */
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

/**
 *  Główny komponent kompozycyjny aplikacji edukacyjnej.
 *
 * `EduApp` definiuje strukturę nawigacji dla całej aplikacji za pomocą `NavHost`.
 * Rejestruje wszystkie trasy i powiązane z nimi komponenty kompozycyjne,
 * takie jak ekrany logowania, rejestracji, kursów, zarządzania plikami, quizów i użytkowników.
 */
@Composable
fun EduApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        /**
         *  Trasa logowania.
         * @param LoginScreen Komponent ekranu logowania.
         */
        composable("login") { LoginScreen(navController) }
        /**
         *  Trasa rejestracji.
         * @param RegisterScreen Komponent ekranu rejestracji.
         */
        composable("register") { RegisterScreen(navController) }
        /**
         *  Trasa listy kursów (ogólna).
         * @param CourseListScreen Komponent ekranu listy kursów.
         */
        composable("courses") { CourseListScreen(navController) }
        /**
         *  Trasa ekranu użytkownika (ogólna).
         * @param UserScreen Komponent ekranu użytkownika.
         */
        composable("user") { UserScreen(navController) }

        // ZMIENIONA NAWIGACJA DLA NAUCZYCIELA I ADMINA
        /**
         *  Trasa ekranu dla nauczyciela.
         * @param TeacherScreen Komponent ekranu nauczyciela.
         */
        composable("teacher") { TeacherScreen(navController) } // Nowy ekran zarządzania grupami
        /**
         *  Trasa ekranu dla administratora/nauczyciela.
         * @param AdminTeacherScreen Komponent ekranu administratora/nauczyciela.
         */
        composable("admin_teacher") { AdminTeacherScreen(navController) } // Można to docelowo połączyć z "teacher"

        // ZMIENIONA TRASA - teraz wymaga ID grupy kursu
        /**
         *  Trasa dodawania kursu do grupy.
         * @param courseGroupId ID grupy kursu.
         * @param AddCourseScreen Komponent ekranu dodawania kursu.
         */
        composable(
            "add_course/{courseGroupId}",
            arguments = listOf(navArgument("courseGroupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseGroupId = backStackEntry.arguments?.getLong("courseGroupId") ?: return@composable
            AddCourseScreen(navController, courseGroupId)
        }

        /**
         *  Trasa zarządzania plikami kursu.
         * @param courseId ID kursu.
         * @param ManageFilesScreen Komponent ekranu zarządzania plikami.
         */
        composable(
            "manage_files/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            ManageFilesScreen(navController = navController, courseId = courseId)
        }
        /**
         *  Trasa moich kursów.
         * @param MyCoursesScreen Komponent ekranu moich kursów.
         */
        composable("my_courses") { MyCoursesScreen(navController) }
        /**
         *  Trasa plików kursu.
         * @param courseId ID kursu.
         * @param CourseFilesScreen Komponent ekranu plików kursu.
         */
        composable(
            "course_files/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            CourseFilesScreen(navController, courseId)
        }
        /**
         *  Trasa zapisu do grupy kursu.
         * @param courseGroupId ID grupy kursu.
         * @param EnrollToGroupScreen Komponent ekranu zapisu do grupy.
         */
        composable(
            "enroll_in_group/{courseGroupId}",
            arguments = listOf(navArgument("courseGroupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseGroupId = backStackEntry.arguments?.getLong("courseGroupId") ?: return@composable
            // Upewnij się, że wywołujesz poprawny Composable - EnrollToGroupScreen
            EnrollToGroupScreen(navController, courseGroupId = courseGroupId)
        }
        /**
         *  Trasa ekranu użytkowników systemu (dla administratora).
         * @param SystemUsersScreen Komponent ekranu użytkowników systemu.
         */
        composable("system_users") { SystemUsersScreen(navController) }
        /**
         *  Trasa zarządzania użytkownikami kursu.
         * @param courseId ID kursu.
         * @param CourseUsersScreen Komponent ekranu zarządzania użytkownikami kursu.
         */
        composable(
            "manage_users/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStack ->
            val courseId = backStack.arguments!!.getLong("courseId")
            CourseUsersScreen(navController, courseId)
        }
        /**
         *  Trasa głównego menu.
         * @param MenuScreen Komponent ekranu menu.
         */
        composable("menu") { MenuScreen(navController) }
        /**
         *  Trasa dostępnych kursów.
         * @param UserScreen Komponent ekranu użytkownika (prawdopodobnie do usunięcia lub zmiany).
         */
        composable("available_courses") { UserScreen(navController) }
        /**
         *  Trasa szczegółów kursu.
         * @param courseId ID kursu.
         * @param CourseDetailsScreen Komponent ekranu szczegółów kursu.
         */
        composable(
            "course_details/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            CourseDetailsScreen(navController, courseId)
        }
        /**
         *  Trasa dodawania quizu.
         * @param courseId ID kursu, do którego dodawany jest quiz.
         * @param AddQuizScreen Komponent ekranu dodawania quizu.
         */
        composable(
            "add_quiz/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            AddQuizScreen(navController, courseId)
        }
        /**
         *  Trasa edycji quizu.
         * @param quizId ID quizu do edycji.
         * @param EditQuizScreen Komponent ekranu edycji quizu.
         */
        composable(
            "edit_quiz/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            EditQuizScreen(navController, quizId)
        }
        /**
         *  Trasa dodawania pytania do quizu.
         * @param quizId ID quizu, do którego dodawane jest pytanie.
         * @param AddQuizQuestionScreen Komponent ekranu dodawania pytania.
         */
        composable(
            "add_question/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            AddQuizQuestionScreen(navController, quizId)
        }
        /**
         *  Trasa edycji pytania w quizie.
         * @param quizId ID quizu zawierającego pytanie.
         * @param questionId ID pytania do edycji.
         * @param EditQuestionScreen Komponent ekranu edycji pytania.
         */
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
        /**
         *  Trasa rozwiązywania quizu.
         * @param quizId ID quizu do rozwiązania.
         * @param courseId ID kursu, do którego należy quiz.
         * @param SolveQuizScreen Komponent ekranu rozwiązywania quizu.
         */
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
        /**
         *  Trasa wyświetlania wyniku quizu.
         * @param quizId ID quizu.
         * @param courseId ID kursu, do którego należy quiz.
         * @param QuizResultScreen Komponent ekranu wyników quizu.
         */
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
        /**
         *  Trasa statystyk quizu dla nauczyciela.
         * @param courseId ID kursu.
         * @param TeacherQuizStatsScreen Komponent ekranu statystyk quizu dla nauczyciela.
         */
        composable(
            "quiz_stats/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            TeacherQuizStatsScreen(navController, courseId)
        }

        /**
         *  Trasa wyników quizu dla nauczyciela.
         * @param quizId ID quizu.
         * @param TeacherQuizResultsScreen Komponent ekranu wyników quizu dla nauczyciela.
         */
        composable(
            "quiz_results/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: return@composable
            TeacherQuizResultsScreen(navController, quizId)
        }
    }
}