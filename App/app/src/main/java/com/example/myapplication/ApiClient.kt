package com.example.myapplication

import android.content.Context
import android.util.Log
import com.example.myapplication.Quizy.QuizResult
import com.example.myapplication.courses.Course
import com.example.myapplication.files.CourseFile
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

/**
 * @file ApiClient.kt
 * Ten plik zawiera modele danych i interfejs usługi API Retrofit do interakcji z backendem.
 */

// NOWY MODEL DANYCH
/**
 * Klasa danych reprezentująca grupę kursów.
 * @param id Unikalny identyfikator grupy kursów.
 * @param name Nazwa grupy kursów.
 * @param description Opcjonalny opis grupy kursów.
 * @param courses Lista kursów należących do tej grupy.
 */
data class CourseGroup(
    val id: Long,
    val name: String,
    val description: String?,
    val courses: List<Course>
)

// Istniejące klasy danych pozostają niezmienione
/**
 * Klasa danych reprezentująca informacje o kursie użytkownika.
 * @param id Unikalny identyfikator użytkownika.
 * @param username Nazwa użytkownika.
 * @param role Rola użytkownika w ramach kursu (np. student, nauczyciel).
 * @param joinedAt Data dołączenia użytkownika do kursu, jako string (opcjonalnie).
 */
data class UserCourseInfo(
    val id: Long,
    val username: String,
    val role: String,
    val joinedAt: String? = null
)

/**
 * Klasa danych do przesyłania odpowiedzi na quiz.
 * @param questionId ID pytania.
 * @param answer Odpowiedź udzielona na pytanie.
 */
data class QuizAnswerDTO(
    val questionId: Long,
    val answer: String
)

/**
 * Klasa danych reprezentująca quiz.
 * @param id Unikalny identyfikator quizu (opcjonalnie, do tworzenia).
 * @param title Tytuł quizu.
 * @param description Opcjonalny opis quizu.
 * @param createdAt Data utworzenia quizu, jako string (opcjonalnie).
 * @param questions Lista pytań w quizie.
 * @param numberOfQuestionsToDisplay Liczba pytań do wyświetlenia w quizie (opcjonalnie).
 */
data class Quiz(
    val id: Long? = null,
    val title: String,
    val description: String? = null,
    val createdAt: String? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val numberOfQuestionsToDisplay: Int? = null
)

/**
 * Klasa danych reprezentująca quiz do rozwiązania.
 * @param id Unikalny identyfikator quizu.
 * @param title Tytuł quizu.
 * @param description Opcjonalny opis quizu.
 * @param questions Lista pytań w quizie.
 */
data class Quizsolve(
    val id: Long,
    val title: String,
    val description: String?,
    val questions: List<QuizQuestion>
)

/**
 * Klasa danych reprezentująca pytanie quizowe.
 * @param id Unikalny identyfikator pytania (opcjonalnie, do tworzenia).
 * @param questionText Treść pytania.
 * @param questionType Typ pytania (np. "single_choice", "multiple_choice", "text").
 * @param options Mapa opcji dla pytań wielokrotnego wyboru, gdzie klucz to identyfikator opcji, a wartość to tekst opcji (opcjonalnie).
 * @param correctAnswer Poprawna odpowiedź na pytanie.
 * @param quizId ID quizu, do którego należy to pytanie (opcjonalnie).
 */
data class QuizQuestion(
    @SerializedName("questionId")
    val id: Long? = null,
    val questionText: String,
    val questionType: String,
    val options: Map<String, String>?,
    val correctAnswer: String,
    val quizId: Long? = null
)

/**
 * Klasa danych reprezentująca odpowiedź zawierającą historię quizów.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param results Lista elementów historii quizów.
 */
data class QuizHistoryResponse(
    val success: Boolean,
    val results: List<QuizHistoryItem>
)

/**
 * Klasa danych reprezentująca pojedynczy element w historii quizów.
 * @param date Data próby quizu.
 * @param score Wynik uzyskany w próbie quizu.
 */
data class QuizHistoryItem(
    val date: String,
    val score: String
)

/**
 * Klasa danych reprezentująca wynik przesłania quizu.
 * @param success Wskazuje, czy przesłanie zakończyło się sukcesem.
 * @param score Uzyskany wynik.
 * @param correctAnswers Liczba poprawnych odpowiedzi.
 * @param totalQuestions Całkowita liczba pytań.
 * @param percentage Procent poprawnych odpowiedzi.
 */
data class SubmissionResultDTO(
    val success: Boolean,
    val score: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Double
)


/**
 * Klasa danych reprezentująca odpowiedź zawierającą listę quizów.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param quizzes Lista quizów.
 */
data class QuizListResponse(val success: Boolean, val quizzes: List<Quiz>)

/**
 * Klasa danych reprezentująca odpowiedź dla pojedynczego quizu.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param quiz Obiekt quizu.
 * @param message Komunikat związany z odpowiedzią.
 */
data class QuizResponse(
    val success: Boolean,
    val quiz: Quiz,
    val message: String
)

/**
 * Klasa danych reprezentująca odpowiedź dla pojedynczego pytania.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param message Komunikat związany z odpowiedzią.
 * @param question Obiekt pytania quizu.
 */
data class QuestionResponse(val success: Boolean, val message: String, val question: QuizQuestion)

/**
 * Ogólna klasa danych dla odpowiedzi API.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param message Komunikat związany z odpowiedzią.
 */
data class GenericResponse(
    val success: Boolean,
    val message: String
)

/**
 * Klasa danych dla żądań logowania.
 * @param username Nazwa użytkownika do logowania.
 * @param password Hasło do logowania.
 */
data class LoginRequest(val username: String, val password: String)

/**
 * Klasa danych dla odpowiedzi logowania.
 * @param success Wskazuje, czy logowanie zakończyło się sukcesem.
 * @param token Token JWT, jeśli logowanie zakończyło się sukcesem (opcjonalnie).
 * @param role Rola zalogowanego użytkownika (opcjonalnie).
 */
data class LoginResponse(val success: Boolean, val token: String?, val role: String?)

/**
 * Klasa danych dla żądań rejestracji.
 * @param username Nazwa użytkownika do rejestracji.
 * @param password Hasło do rejestracji.
 * @param roleCode Kod roli dla użytkownika (np. "STUDENT", "TEACHER").
 */
data class RegisterRequest(val username: String, val password: String, val roleCode: String)

/**
 * Klasa danych dla odpowiedzi rejestracji.
 * @param success Wskazuje, czy rejestracja zakończyła się sukcesem.
 * @param message Komunikat związany z odpowiedzią.
 */
data class RegisterResponse(val success: Boolean, val message: String)

/**
 * Klasa danych do generowania żądania kodu studenckiego.
 * @param validity Okres ważności kodu.
 */
data class GenerateCodeRequest(val validity: String)

/**
 * Klasa danych do generowania odpowiedzi kodu studenckiego.
 * @param success Wskazuje, czy generowanie kodu zakończyło się sukcesem.
 * @param code Wygenerowany kod.
 * @param expiresAt Data wygaśnięcia kodu.
 * @param message Komunikat związany z odpowiedzią.
 */
data class GenerateCodeResponse(val success: Boolean, val code: String, val expiresAt: String, val message: String)

/**
 * Klasa danych reprezentująca statystyki dla pojedynczego quizu.
 * @param quizId ID quizu.
 * @param quizTitle Tytuł quizu.
 * @param attempts Liczba prób dla quizu.
 * @param averageScore Średnia ocena dla quizu.
 */
data class QuizStat(
    val quizId: Long,
    val quizTitle: String,
    val attempts: Long,
    val averageScore: Double
)

/**
 * Klasa danych reprezentująca szczegółowe wyniki próby quizu.
 * @param userId ID użytkownika, który rozwiązał quiz.
 * @param resultId ID wyniku quizu.
 * @param username Nazwa użytkownika.
 * @param correctAnswers Liczba poprawnych odpowiedzi.
 * @param totalQuestions Całkowita liczba pytań.
 * @param score Uzyskany wynik.
 * @param completionDate Data ukończenia quizu.
 * @param answers Lista map, z których każda reprezentuje odpowiedź na pytanie.
 */
data class QuizDetailedResult(
    val userId: Long,
    val resultId: Long,
    val username: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val score: Double,
    val completionDate: String,
    val answers: List<Map<String, Any>>
)

/**
 * Klasa danych reprezentująca odpowiedź zawierającą statystyki quizu dla kursu.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param courseId ID kursu.
 * @param stats Lista statystyk quizu.
 */
data class QuizStatsResponse(
    val success: Boolean,
    val courseId: Long,
    val stats: List<QuizStat>
)

/**
 * Klasa danych reprezentująca odpowiedź zawierającą szczegółowe wyniki quizu.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param quizId ID quizu.
 * @param quizTitle Tytuł quizu.
 * @param results Lista szczegółowych wyników quizu.
 */
data class QuizDetailedResultsResponse(
    val success: Boolean,
    val quizId: Long,
    val quizTitle: String,
    val results: List<QuizDetailedResult>
)

/**
 * Klasa danych reprezentująca odpowiedź zawierającą użytkowników zapisanych na kurs.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param users Lista informacji o użytkownikach kursu.
 */
data class CourseUsersResponse(
    val success: Boolean,
    val users: List<UserCourseInfo>
)

/**
 * Klasa danych reprezentująca odpowiedź zawierającą listę wszystkich użytkowników.
 * @param success Wskazuje, czy żądanie zakończyło się sukcesem.
 * @param users Lista informacji o użytkownikach kursu.
 */
data class UsersResponse(
    val success: Boolean,
    val users: List<UserCourseInfo>
)

/**
 * Interfejs Retrofit dla usług API kursów.
 * Definiuje punkty końcowe do interakcji z backendem zarządzania kursami.
 */
interface CourseApiService {
    /**
     * Loguje użytkownika.
     * @param request Żądanie logowania zawierające nazwę użytkownika i hasło.
     * @return Obiekt Retrofit Response zawierający LoginResponse.
     */
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Rejestruje nowego użytkownika.
     * @param request Żądanie rejestracji zawierające nazwę użytkownika, hasło i kod roli.
     * @return Obiekt Retrofit Response zawierający RegisterResponse.
     */
    @POST("/api/courses/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    /**
     * Generuje kod rejestracji studenta.
     * @param request Żądanie zawierające okres ważności kodu.
     * @return Obiekt Retrofit Response zawierający GenerateCodeResponse.
     */
    @POST("/api/courses/auth/generate-student-code")
    suspend fun generateStudentCode(@Body request: GenerateCodeRequest): Response<GenerateCodeResponse>

    /**
     * Pobiera wszystkie dostępne kursy.
     * @return Lista obiektów Course.
     */
    @GET("/api/courses")
    suspend fun getAllCourses(): List<Course>

    /**
     * Tworzy nowy kurs.
     * @param courseData Mapa zawierająca dane kursu.
     * @return Obiekt Retrofit Response zawierający mapę z odpowiedzią API.
     */
    @POST("/api/courses")
    suspend fun createCourse(@Body courseData: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any>>

    /**
     * Usuwa kurs o podanym ID.
     * @param id ID kursu do usunięcia.
     * @return Obiekt Retrofit Response zawierający mapę z odpowiedzią API.
     */
    @DELETE("/api/courses/{id}")
    suspend fun deleteCourse(@Path("id") id: Long): Response<Map<String, Any>>

    /**
     * Pobiera wszystkie grupy kursów.
     * @return Obiekt Retrofit Response zawierający listę obiektów CourseGroup.
     */
    @GET("/api/course-groups")
    suspend fun getCourseGroups(): Response<List<CourseGroup>>

    /**
     * Zapisuje użytkownika do określonej grupy kursów.
     * @param groupId ID grupy kursów, do której należy się zapisać.
     * @param request Mapa zawierająca wszelkie dodatkowe dane dotyczące zapisu (np. klucz zapisu).
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @POST("/api/course-groups/{groupId}/enroll")
    suspend fun enrollInCourseGroup(
        @Path("groupId") groupId: Long,
        @Body request: Map<String, String>
    ): Response<GenericResponse>

    /**
     * Usuwa grupę kursów o podanym ID.
     * @param groupId ID grupy kursów do usunięcia.
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @DELETE("/api/course-groups/{groupId}")
    suspend fun deleteCourseGroup(@Path("groupId") groupId: Long): Response<GenericResponse>

    /**
     * Usuwa określony wynik quizu.
     * @param resultId ID wyniku quizu do usunięcia.
     * @return Obiekt Retrofit Response zawierający mapę z odpowiedzią API.
     */
    @DELETE("api/courses/quizzes/results/{resultId}")
    suspend fun deleteQuizResult(@Path("resultId") resultId: Long): Response<Map<String, Any>>

    /**
     * Pobiera raport PDF ze szczegółowymi wynikami quizu dla danego quizu.
     * @param quizId ID quizu.
     * @return Obiekt Retrofit Response zawierający ResponseBody do strumieniowania PDF.
     */
    @Streaming // Ważne dla pobierania dużych plików
    @GET("api/courses/quizzes/{quizId}/detailed-results/pdf")
    suspend fun downloadQuizResultsPdf(@Path("quizId") quizId: Long): Response<ResponseBody>

    /**
     * Tworzy nową grupę kursów.
     * @param request Mapa zawierająca dane dla nowej grupy kursów.
     * @return Obiekt Retrofit Response zawierający utworzoną CourseGroup.
     */
    @POST("/api/course-groups")
    suspend fun createCourseGroup(@Body request: Map<String, String>): Response<CourseGroup>

    /**
     * Duplikuje istniejący kurs do określonej grupy kursów.
     * @param groupId ID grupy kursów, do której ma zostać zduplikowany kurs.
     * @param courseId ID kursu do zduplikowania.
     * @param request Mapa zawierająca wszelkie dodatkowe dane do duplikacji (np. nowa nazwa kursu).
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @POST("/api/course-groups/course-groups/{groupId}/courses/{courseId}/duplicate")
    suspend fun duplicateCourse(
        @Path("groupId") groupId: Long,
        @Path("courseId") courseId: Long,
        @Body request: Map<String, String>
    ): Response<GenericResponse>


    /**
     * Weryfikuje klucz dostępu dla określonego kursu.
     * @param courseId ID kursu.
     * @param request Mapa zawierająca klucz dostępu.
     * @return Mapa z odpowiedzią API, zazwyczaj wskazującą na sukces lub porażkę.
     */
    @POST("/api/courses/{id}/verify-key")
    suspend fun verifyAccessKey(
        @Path("id") courseId: Long,
        @Body request: Map<String, String>
    ): Map<String, Any>

    /**
     * Pobiera wszystkie pliki związane z określonym kursem.
     * @param courseId ID kursu.
     * @return Lista obiektów CourseFile.
     */
    @GET("/api/courses/{id}/files")
    suspend fun getCourseFiles(@Path("id") courseId: Long): List<CourseFile>

    /**
     * Pobiera użytkowników zapisanych na określony kurs.
     * @param courseId ID kursu.
     * @return Obiekt CourseUsersResponse.
     */
    @GET("/api/courses/{courseId}/users")
    suspend fun getCourseUsers(
        @Path("courseId") courseId: Long
    ): CourseUsersResponse

    /**
     * Usuwa użytkownika z określonego kursu.
     * @param courseId ID kursu.
     * @param userId ID użytkownika do usunięcia.
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @DELETE("/api/courses/{courseId}/users/{userId}")
    suspend fun removeUserFromCourse(
        @Path("courseId") courseId: Long,
        @Path("userId") userId: Long
    ): Response<GenericResponse>

    /**
     * Usuwa użytkownika z systemu.
     * @param userId ID użytkownika do usunięcia.
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @DELETE("/api/courses/users/{userId}")
    suspend fun deleteUserFromSystem(@Path("userId") userId: Long): Response<GenericResponse>

    /**
     * Przesyła plik do określonego kursu.
     * @param courseId ID kursu, do którego ma zostać przesłany plik.
     * @param file MultipartBody.Part reprezentujący plik do przesłania.
     * @return Obiekt Retrofit Response zawierający ResponseBody.
     */
    @Multipart
    @POST("/api/courses/{courseId}/files/upload")
    suspend fun uploadFile(
        @Path("courseId") courseId: Long,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    /**
     * Usuwa określony plik z kursu.
     * @param courseId ID kursu.
     * @param fileId ID pliku do usunięcia.
     * @return Obiekt Retrofit Response zawierający ResponseBody.
     */
    @DELETE("/api/courses/{courseId}/files/{fileId}")
    suspend fun deleteCourseFile(
        @Path("courseId") courseId: Long,
        @Path("fileId") fileId: Long
    ): Response<ResponseBody>

    /**
     * Pobiera kursy związane z uwierzytelnionym użytkownikiem.
     * @return Mapa z odpowiedzią API, zazwyczaj zawierająca listę kursów.
     */
    @GET("/api/courses/my-courses")
    suspend fun getUserCourses(): Map<String, Any>

    /**
     * Pobiera wszystkie quizy dla określonego kursu.
     * @param courseId ID kursu.
     * @return Obiekt QuizListResponse.
     */
    @GET("/api/courses/{id}/quizzes")
    suspend fun getCourseQuizzes(@Path("id") courseId: Long): QuizListResponse

    /**
     * Tworzy nowy quiz dla określonego kursu.
     * @param courseId ID kursu, dla którego ma zostać utworzony quiz.
     * @param quiz Obiekt Quiz do utworzenia.
     * @return Obiekt Retrofit Response zawierający QuizResponse.
     */
    @POST("/api/courses/{id}/quizzes")
    suspend fun createQuiz(
        @Path("id") courseId: Long,
        @Body quiz: Quiz
    ): Response<QuizResponse>

    /**
     * Pobiera quiz do celów edycji.
     * @param quizId ID quizu do pobrania.
     * @return Obiekt Retrofit Response zawierający QuizResponse.
     */
    @GET("/api/courses/quizzes/{quizId}/edit")
    suspend fun getQuizForEdit(@Path("quizId") quizId: Long): Response<QuizResponse>

    /**
     * Aktualizuje istniejący quiz.
     * @param quizId ID quizu do aktualizacji.
     * @param quiz Zaktualizowany obiekt Quiz.
     * @return Obiekt Retrofit Response zawierający QuizResponse.
     */
    @PUT("/api/courses/quizzes/{quizId}")
    suspend fun updateQuiz(
        @Path("quizId") quizId: Long,
        @Body quiz: Quiz
    ): Response<QuizResponse>

    /**
     * Tworzy nowe pytanie dla określonego quizu.
     * @param quizId ID quizu, do którego ma zostać dodane pytanie.
     * @param question Obiekt QuizQuestion do utworzenia.
     * @return Obiekt Retrofit Response zawierający QuestionResponse.
     */
    @POST("/api/courses/quizzes/{quizId}/questions")
    suspend fun createQuizQuestion(
        @Path("quizId") quizId: Long,
        @Body question: QuizQuestion
    ): Response<QuestionResponse>

    /**
     * Aktualizuje istniejące pytanie quizu.
     * @param quizId ID quizu, do którego należy pytanie.
     * @param questionId ID pytania do aktualizacji.
     * @param question Zaktualizowany obiekt QuizQuestion.
     * @return Obiekt Retrofit Response zawierający QuestionResponse.
     */
    @PUT("/api/courses/quizzes/{quizId}/questions/{questionId}")
    suspend fun updateQuizQuestion(
        @Path("quizId") quizId: Long,
        @Path("questionId") questionId: Long,
        @Body question: QuizQuestion
    ): Response<QuestionResponse>

    /**
     * Usuwa quiz o podanym ID.
     * @param quizId ID quizu do usunięcia.
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @DELETE("/api/courses/quizzes/{quizId}")
    suspend fun deleteQuiz(@Path("quizId") quizId: Long): Response<GenericResponse>

    /**
     * Usuwa określone pytanie z quizu.
     * @param quizId ID quizu, do którego należy pytanie.
     * @param questionId ID pytania do usunięcia.
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @DELETE("/api/courses/quizzes/{quizId}/questions/{questionId}")
    suspend fun deleteQuizQuestion(
        @Path("quizId") quizId: Long,
        @Path("questionId") questionId: Long
    ): Response<GenericResponse>

    /**
     * Pobiera quiz o podanym ID.
     * @param quizId ID quizu do pobrania.
     * @return Obiekt Retrofit Response zawierający QuizResponse.
     */
    @GET("/api/courses/quizzes/{quizId}")
    suspend fun getQuiz(@Path("quizId") quizId: Long): Response<QuizResponse>

    /**
     * Wysyła odpowiedzi na quiz.
     * @param quizId ID quizu.
     * @param answers Lista obiektów QuizAnswerDTO reprezentujących przesłane odpowiedzi.
     * @return Obiekt Retrofit Response zawierający SubmissionResultDTO.
     */
    @POST("/api/courses/quizzes/{quizId}/submit")
    suspend fun submitQuizAnswers(
        @Path("quizId") quizId: Long,
        @Body answers: List<QuizAnswerDTO>
    ): Response<SubmissionResultDTO>

    /**
     * Pobiera wynik określonej próby quizu.
     * @param quizId ID próby quizu.
     * @return Obiekt Retrofit Response zawierający QuizResult.
     */
    @GET("/api/courses/quizzes/{quizId}/results")
    suspend fun getQuizResult(@Path("quizId") quizId: Long): Response<QuizResult>

    /**
     * Pobiera statystyki quizu dla określonego kursu.
     * @param courseId ID kursu.
     * @return Obiekt Retrofit Response zawierający QuizStatsResponse.
     */
    @GET("/api/courses/{courseId}/quiz-stats")
    suspend fun getCourseQuizStats(@Path("courseId") courseId: Long): Response<QuizStatsResponse>

    /**
     * Pobiera szczegółowe wyniki quizu dla określonego quizu.
     * @param quizId ID quizu.
     * @return Obiekt Retrofit Response zawierający QuizDetailedResultsResponse.
     */
    @GET("/api/courses/quizzes/{quizId}/detailed-results")
    suspend fun getQuizDetailedResults(@Path("quizId") quizId: Long): Response<QuizDetailedResultsResponse>

    /**
     * Pobiera wszystkich użytkowników w systemie.
     * @return Obiekt UsersResponse zawierający listę wszystkich użytkowników.
     */
    @GET("/api/courses/users")
    suspend fun getAllUsers(): UsersResponse

    /**
     * Promuje użytkownika do roli nauczyciela.
     * @param userId ID użytkownika do awansowania.
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @POST("/api/courses/users/{userId}/promote-to-teacher")
    suspend fun promoteToTeacher(@Path("userId") userId: Long): Response<GenericResponse>

    /**
     * Degraduje użytkownika do roli studenta.
     * @param userId ID użytkownika do degradacji.
     * @return Obiekt Retrofit Response zawierający GenericResponse.
     */
    @POST("/api/courses/users/{userId}/demote-to-student")
    suspend fun demoteToStudent(@Path("userId") userId: Long): Response<GenericResponse>
}

/**
 * Obiekt singletonowy do dostarczania instancji klienta Retrofit.
 */
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    /**
     * Pobiera instancję CourseApiService.
     * Konfiguruje OkHttpClient z interceptorem dodającym token JWT do żądań.
     * @param context Kontekst Androida, używany do dostępu do SharedPreferences.
     * @return Instancja CourseApiService.
     */
    fun getInstance(context: Context): CourseApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain: Interceptor.Chain ->
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("jwt_token", null)
                val request = chain.request().newBuilder().apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                        Log.d("RetrofitClient", "Dodano token: $token")
                    } else {
                        Log.w("RetrofitClient", "Brak tokena w SharedPreferences")
                    }
                    addHeader("Accept", "application/json")
                }.build()
                val response = chain.proceed(request)
                Log.d("RetrofitClient", "Odpowiedź serwera: ${response.code}")
                response
            }
            .build()

        val baseUrl = ServerConfig.getBaseUrl(context)
        Log.d("RetrofitClient", "Używam bazowego adresu URL: $baseUrl")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CourseApiService::class.java)
    }
}
/**
 * Obiekt singletonowy [ServerConfig] odpowiada za zarządzanie konfiguracją adresu IP serwera
 * w aplikacji Android. Umożliwia zapisywanie i odczytywanie adresu IP serwera za pomocą
 * [SharedPreferences], a także konstruowanie pełnego bazowego adresu URL API.
 * Dzięki temu aplikacja może dynamicznie łączyć się z różnymi instancjami backendu.
 */
object ServerConfig {

    /**
     * Nazwa pliku SharedPreferences, w którym będą przechowywane preferencje serwera.
     */
    private const val PREFS_NAME = "server_prefs"
    /**
     * Klucz używany do przechowywania adresu IP serwera w SharedPreferences.
     */
    private const val KEY_SERVER_IP = "server_ip"
    /**
     * Domyślny adres IP serwera, używany gdy nie znaleziono żadnego zapisanego adresu IP.
     * Jest to standardowy adres dla emulatora Androida.
     */
    private const val DEFAULT_SERVER_IP = "10.0.2.2" // Domyślne IP dla emulatora Androida

    /**
     * Zapisuje adres IP serwera w SharedPreferences.
     * @param context Kontekst aplikacji.
     * @param ip Adres IP do zapisania.
     */
    fun saveServerIp(context: Context, ip: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(KEY_SERVER_IP, ip)
            .apply()
        Log.d("ServerConfig", "Zapisano nowy adres IP serwera: $ip")
    }

    /**
     * Odczytuje zapisany adres IP serwera z SharedPreferences.
     * @param context Kontekst aplikacji.
     * @return Zapisany adres IP lub domyślną wartość.
     */
    fun getServerIp(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP
    }

    /**
     * Konstruuje i zwraca pełny bazowy adres URL serwera.
     * @param context Kontekst aplikacji.
     * @return Bazowy adres URL w formacie "http://[IP]:8080/".
     */
    fun getBaseUrl(context: Context): String {
        return "http://${getServerIp(context)}:8080/"
    }
}