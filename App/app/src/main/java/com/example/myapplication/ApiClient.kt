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
 *  This file contains the data models and the Retrofit API service interface for interacting with the backend.
 */

// NOWY MODEL DANYCH
/**
 *  Data class representing a course group.
 * @param id The unique identifier of the course group.
 * @param name The name of the course group.
 * @param description An optional description of the course group.
 * @param courses A list of courses belonging to this group.
 */
data class CourseGroup(
    val id: Long,
    val name: String,
    val description: String?,
    val courses: List<Course>
)

// Existing data classes remain unchanged
/**
 *  Data class representing information about a user's course.
 * @param id The unique identifier of the user.
 * @param username The username of the user.
 * @param role The role of the user within the course (e.g., student, teacher).
 * @param joinedAt The date the user joined the course, as a string (optional).
 */
data class UserCourseInfo(
    val id: Long,
    val username: String,
    val role: String,
    val joinedAt: String? = null
)

/**
 *  Data class for submitting a quiz answer.
 * @param questionId The ID of the question.
 * @param answer The answer provided for the question.
 */
data class QuizAnswerDTO(
    val questionId: Long,
    val answer: String
)

/**
 *  Data class representing a quiz.
 * @param id The unique identifier of the quiz (optional, for creation).
 * @param title The title of the quiz.
 * @param description An optional description of the quiz.
 * @param createdAt The creation date of the quiz, as a string (optional).
 * @param questions A list of questions in the quiz.
 * @param numberOfQuestionsToDisplay The number of questions to display in the quiz (optional).
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
 *  Data class representing a quiz to be solved.
 * @param id The unique identifier of the quiz.
 * @param title The title of the quiz.
 * @param description An optional description of the quiz.
 * @param questions A list of questions in the quiz.
 */
data class Quizsolve(
    val id: Long,
    val title: String,
    val description: String?,
    val questions: List<QuizQuestion>
)

/**
 *  Data class representing a quiz question.
 * @param id The unique identifier of the question (optional, for creation).
 * @param questionText The text of the question.
 * @param questionType The type of the question (e.g., "single_choice", "multiple_choice", "text").
 * @param options A map of options for multiple-choice questions, where the key is the option identifier and the value is the option text (optional).
 * @param correctAnswer The correct answer to the question.
 * @param quizId The ID of the quiz this question belongs to (optional).
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
 *  Data class representing a response containing quiz history.
 * @param success Indicates if the request was successful.
 * @param results A list of quiz history items.
 */
data class QuizHistoryResponse(
    val success: Boolean,
    val results: List<QuizHistoryItem>
)

/**
 *  Data class representing a single item in quiz history.
 * @param date The date of the quiz attempt.
 * @param score The score achieved in the quiz attempt.
 */
data class QuizHistoryItem(
    val date: String,
    val score: String
)

/**
 *  Data class representing the result of a quiz submission.
 * @param success Indicates if the submission was successful.
 * @param score The score achieved.
 * @param correctAnswers The number of correct answers.
 * @param totalQuestions The total number of questions.
 * @param percentage The percentage of correct answers.
 */
data class SubmissionResultDTO(
    val success: Boolean,
    val score: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Double
)



/**
 *  Data class representing a response containing a list of quizzes.
 * @param success Indicates if the request was successful.
 * @param quizzes A list of quizzes.
 */
data class QuizListResponse(val success: Boolean, val quizzes: List<Quiz>)

/**
 *  Data class representing a response for a single quiz.
 * @param success Indicates if the request was successful.
 * @param quiz The quiz object.
 * @param message A message related to the response.
 */
data class QuizResponse(
    val success: Boolean,
    val quiz: Quiz,
    val message: String
)

/**
 *  Data class representing a response for a single question.
 * @param success Indicates if the request was successful.
 * @param message A message related to the response.
 * @param question The quiz question object.
 */
data class QuestionResponse(val success: Boolean, val message: String, val question: QuizQuestion)

/**
 *  Generic data class for API responses.
 * @param success Indicates if the request was successful.
 * @param message A message related to the response.
 */
data class GenericResponse(
    val success: Boolean,
    val message: String
)

/**
 *  Data class for login requests.
 * @param username The username for login.
 * @param password The password for login.
 */
data class LoginRequest(val username: String, val password: String)

/**
 *  Data class for login responses.
 * @param success Indicates if the login was successful.
 * @param token The JWT token if login was successful (optional).
 * @param role The role of the logged-in user (optional).
 */
data class LoginResponse(val success: Boolean, val token: String?, val role: String?)

/**
 *  Data class for registration requests.
 * @param username The username for registration.
 * @param password The password for registration.
 * @param roleCode The role code for the user (e.g., "STUDENT", "TEACHER").
 */
data class RegisterRequest(val username: String, val password: String, val roleCode: String)

/**
 *  Data class for registration responses.
 * @param success Indicates if the registration was successful.
 * @param message A message related to the response.
 */
data class RegisterResponse(val success: Boolean, val message: String)

/**
 *  Data class for generating a student code request.
 * @param validity The validity period of the code.
 */
data class GenerateCodeRequest(val validity: String)

/**
 *  Data class for generating a student code response.
 * @param success Indicates if the code generation was successful.
 * @param code The generated code.
 * @param expiresAt The expiration date of the code.
 * @param message A message related to the response.
 */
data class GenerateCodeResponse(val success: Boolean, val code: String, val expiresAt: String, val message: String)

/**
 *  Data class representing statistics for a single quiz.
 * @param quizId The ID of the quiz.
 * @param quizTitle The title of the quiz.
 * @param attempts The number of attempts for the quiz.
 * @param averageScore The average score for the quiz.
 */
data class QuizStat(
    val quizId: Long,
    val quizTitle: String,
    val attempts: Long,
    val averageScore: Double
)

/**
 *  Data class representing detailed results for a quiz attempt.
 * @param userId The ID of the user who took the quiz.
 * @param resultId The ID of the quiz result.
 * @param username The username of the user.
 * @param correctAnswers The number of correct answers.
 * @param totalQuestions The total number of questions.
 * @param score The score achieved.
 * @param completionDate The date of quiz completion.
 * @param answers A list of maps, each representing an answer to a question.
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
 *  Data class representing a response containing quiz statistics for a course.
 * @param success Indicates if the request was successful.
 * @param courseId The ID of the course.
 * @param stats A list of quiz statistics.
 */
data class QuizStatsResponse(
    val success: Boolean,
    val courseId: Long,
    val stats: List<QuizStat>
)

/**
 *  Data class representing a response containing detailed quiz results.
 * @param success Indicates if the request was successful.
 * @param quizId The ID of the quiz.
 * @param quizTitle The title of the quiz.
 * @param results A list of detailed quiz results.
 */
data class QuizDetailedResultsResponse(
    val success: Boolean,
    val quizId: Long,
    val quizTitle: String,
    val results: List<QuizDetailedResult>
)

/**
 *  Data class representing a response containing users enrolled in a course.
 * @param success Indicates if the request was successful.
 * @param users A list of user course information.
 */
data class CourseUsersResponse(
    val success: Boolean,
    val users: List<UserCourseInfo>
)

/**
 *  Data class representing a response containing a list of all users.
 * @param success Indicates if the request was successful.
 * @param users A list of user course information.
 */
data class UsersResponse(
    val success: Boolean,
    val users: List<UserCourseInfo>
)

/**
 *  Retrofit interface for Course API services.
 * Defines the endpoints for interacting with the course management backend.
 */
interface CourseApiService {
    /**
     *  Logs in a user.
     * @param request The login request containing username and password.
     * @return A Retrofit Response object containing LoginResponse.
     */
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     *  Registers a new user.
     * @param request The registration request containing username, password, and role code.
     * @return A Retrofit Response object containing RegisterResponse.
     */
    @POST("/api/courses/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    /**
     *  Generates a student registration code.
     * @param request The request containing the validity period for the code.
     * @return A Retrofit Response object containing GenerateCodeResponse.
     */
    @POST("/api/courses/auth/generate-student-code")
    suspend fun generateStudentCode(@Body request: GenerateCodeRequest): Response<GenerateCodeResponse>

    /**
     *  Retrieves all available courses.
     * @return A list of Course objects.
     */
    @GET("/api/courses")
    suspend fun getAllCourses(): List<Course>

    /**
     *  Creates a new course.
     * @param courseData A map containing the course data.
     * @return A Retrofit Response object containing a map with the API response.
     */
    @POST("/api/courses")
    suspend fun createCourse(@Body courseData: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any>>

    /**
     *  Deletes a course by its ID.
     * @param id The ID of the course to delete.
     * @return A Retrofit Response object containing a map with the API response.
     */
    @DELETE("/api/courses/{id}")
    suspend fun deleteCourse(@Path("id") id: Long): Response<Map<String, Any>>

    /**
     *  Retrieves all course groups.
     * @return A Retrofit Response object containing a list of CourseGroup objects.
     */
    @GET("/api/course-groups")
    suspend fun getCourseGroups(): Response<List<CourseGroup>>

    /**
     *  Enrolls a user in a specific course group.
     * @param groupId The ID of the course group to enroll in.
     * @param request A map containing any additional enrollment data (e.g., enrollment key).
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @POST("/api/course-groups/{groupId}/enroll")
    suspend fun enrollInCourseGroup(
        @Path("groupId") groupId: Long,
        @Body request: Map<String, String>
    ): Response<GenericResponse>

    /**
     *  Deletes a course group by its ID.
     * @param groupId The ID of the course group to delete.
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @DELETE("/api/course-groups/{groupId}")
    suspend fun deleteCourseGroup(@Path("groupId") groupId: Long): Response<GenericResponse>

    /**
     *  Deletes a specific quiz result.
     * @param resultId The ID of the quiz result to delete.
     * @return A Retrofit Response object containing a map with the API response.
     */
    @DELETE("api/courses/quizzes/results/{resultId}")
    suspend fun deleteQuizResult(@Path("resultId") resultId: Long): Response<Map<String, Any>>

    /**
     *  Downloads a PDF report of detailed quiz results for a given quiz.
     * @param quizId The ID of the quiz.
     * @return A Retrofit Response object containing a ResponseBody for streaming the PDF.
     */
    @Streaming // Ważne dla pobierania dużych plików
    @GET("api/courses/quizzes/{quizId}/detailed-results/pdf")
    suspend fun downloadQuizResultsPdf(@Path("quizId") quizId: Long): Response<ResponseBody>

    /**
     *  Creates a new course group.
     * @param request A map containing the data for the new course group.
     * @return A Retrofit Response object containing the created CourseGroup.
     */
    @POST("/api/course-groups")
    suspend fun createCourseGroup(@Body request: Map<String, String>): Response<CourseGroup>

    /**
     *  Duplicates an existing course into a specified course group.
     * @param groupId The ID of the course group to duplicate the course into.
     * @param courseId The ID of the course to duplicate.
     * @param request A map containing any additional data for the duplication (e.g., new course name).
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @POST("/api/course-groups/course-groups/{groupId}/courses/{courseId}/duplicate")
    suspend fun duplicateCourse(
        @Path("groupId") groupId: Long,
        @Path("courseId") courseId: Long,
        @Body request: Map<String, String>
    ): Response<GenericResponse>


    /**
     *  Verifies an access key for a specific course.
     * @param courseId The ID of the course.
     * @param request A map containing the access key.
     * @return A map with the API response, typically indicating success or failure.
     */
    @POST("/api/courses/{id}/verify-key")
    suspend fun verifyAccessKey(
        @Path("id") courseId: Long,
        @Body request: Map<String, String>
    ): Map<String, Any>

    /**
     *  Retrieves all files associated with a specific course.
     * @param courseId The ID of the course.
     * @return A list of CourseFile objects.
     */
    @GET("/api/courses/{id}/files")
    suspend fun getCourseFiles(@Path("id") courseId: Long): List<CourseFile>

    /**
     *  Retrieves users enrolled in a specific course.
     * @param courseId The ID of the course.
     * @return A CourseUsersResponse object containing the list of users.
     */
    @GET("/api/courses/{courseId}/users")
    suspend fun getCourseUsers(
        @Path("courseId") courseId: Long
    ): CourseUsersResponse

    /**
     *  Removes a user from a specific course.
     * @param courseId The ID of the course.
     * @param userId The ID of the user to remove.
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @DELETE("/api/courses/{courseId}/users/{userId}")
    suspend fun removeUserFromCourse(
        @Path("courseId") courseId: Long,
        @Path("userId") userId: Long
    ): Response<GenericResponse>

    /**
     *  Deletes a user from the system.
     * @param userId The ID of the user to delete.
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @DELETE("/api/courses/users/{userId}")
    suspend fun deleteUserFromSystem(@Path("userId") userId: Long): Response<GenericResponse>

    /**
     *  Uploads a file to a specific course.
     * @param courseId The ID of the course to upload the file to.
     * @param file The MultipartBody.Part representing the file to upload.
     * @return A Retrofit Response object containing a ResponseBody.
     */
    @Multipart
    @POST("/api/courses/{courseId}/files/upload")
    suspend fun uploadFile(
        @Path("courseId") courseId: Long,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    /**
     *  Deletes a specific file from a course.
     * @param courseId The ID of the course.
     * @param fileId The ID of the file to delete.
     * @return A Retrofit Response object containing a ResponseBody.
     */
    @DELETE("/api/courses/{courseId}/files/{fileId}")
    suspend fun deleteCourseFile(
        @Path("courseId") courseId: Long,
        @Path("fileId") fileId: Long
    ): Response<ResponseBody>

    /**
     *  Retrieves courses associated with the authenticated user.
     * @return A map with the API response, typically containing a list of courses.
     */
    @GET("/api/courses/my-courses")
    suspend fun getUserCourses(): Map<String, Any>

    /**
     *  Retrieves all quizzes for a specific course.
     * @param courseId The ID of the course.
     * @return A QuizListResponse object.
     */
    @GET("/api/courses/{id}/quizzes")
    suspend fun getCourseQuizzes(@Path("id") courseId: Long): QuizListResponse

    /**
     *  Creates a new quiz for a specific course.
     * @param courseId The ID of the course to create the quiz for.
     * @param quiz The Quiz object to create.
     * @return A Retrofit Response object containing a QuizResponse.
     */
    @POST("/api/courses/{id}/quizzes")
    suspend fun createQuiz(
        @Path("id") courseId: Long,
        @Body quiz: Quiz
    ): Response<QuizResponse>

    /**
     *  Retrieves a quiz for editing purposes.
     * @param quizId The ID of the quiz to retrieve.
     * @return A Retrofit Response object containing a QuizResponse.
     */
    @GET("/api/courses/quizzes/{quizId}/edit")
    suspend fun getQuizForEdit(@Path("quizId") quizId: Long): Response<QuizResponse>

    /**
     *  Updates an existing quiz.
     * @param quizId The ID of the quiz to update.
     * @param quiz The updated Quiz object.
     * @return A Retrofit Response object containing a QuizResponse.
     */
    @PUT("/api/courses/quizzes/{quizId}")
    suspend fun updateQuiz(
        @Path("quizId") quizId: Long,
        @Body quiz: Quiz
    ): Response<QuizResponse>

    /**
     *  Creates a new question for a specific quiz.
     * @param quizId The ID of the quiz to add the question to.
     * @param question The QuizQuestion object to create.
     * @return A Retrofit Response object containing a QuestionResponse.
     */
    @POST("/api/courses/quizzes/{quizId}/questions")
    suspend fun createQuizQuestion(
        @Path("quizId") quizId: Long,
        @Body question: QuizQuestion
    ): Response<QuestionResponse>

    /**
     *  Updates an existing quiz question.
     * @param quizId The ID of the quiz the question belongs to.
     * @param questionId The ID of the question to update.
     * @param question The updated QuizQuestion object.
     * @return A Retrofit Response object containing a QuestionResponse.
     */
    @PUT("/api/courses/quizzes/{quizId}/questions/{questionId}")
    suspend fun updateQuizQuestion(
        @Path("quizId") quizId: Long,
        @Path("questionId") questionId: Long,
        @Body question: QuizQuestion
    ): Response<QuestionResponse>

    /**
     *  Deletes a quiz by its ID.
     * @param quizId The ID of the quiz to delete.
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @DELETE("/api/courses/quizzes/{quizId}")
    suspend fun deleteQuiz(@Path("quizId") quizId: Long): Response<GenericResponse>

    /**
     *  Deletes a specific question from a quiz.
     * @param quizId The ID of the quiz the question belongs to.
     * @param questionId The ID of the question to delete.
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @DELETE("/api/courses/quizzes/{quizId}/questions/{questionId}")
    suspend fun deleteQuizQuestion(
        @Path("quizId") quizId: Long,
        @Path("questionId") questionId: Long
    ): Response<GenericResponse>

    /**
     *  Retrieves a quiz by its ID.
     * @param quizId The ID of the quiz to retrieve.
     * @return A Retrofit Response object containing a QuizResponse.
     */
    @GET("/api/courses/quizzes/{quizId}")
    suspend fun getQuiz(@Path("quizId") quizId: Long): Response<QuizResponse>

    /**
     *  Submits answers for a quiz.
     * @param quizId The ID of the quiz.
     * @param answers A list of QuizAnswerDTO objects representing the submitted answers.
     * @return A Retrofit Response object containing a SubmissionResultDTO.
     */
    @POST("/api/courses/quizzes/{quizId}/submit")
    suspend fun submitQuizAnswers(
        @Path("quizId") quizId: Long,
        @Body answers: List<QuizAnswerDTO>
    ): Response<SubmissionResultDTO>

    /**
     *  Retrieves the result of a specific quiz attempt.
     * @param quizId The ID of the quiz attempt.
     * @return A Retrofit Response object containing a QuizResult.
     */
    @GET("/api/courses/quizzes/{quizId}/results")
    suspend fun getQuizResult(@Path("quizId") quizId: Long): Response<QuizResult>

    /**
     *  Retrieves quiz statistics for a specific course.
     * @param courseId The ID of the course.
     * @return A Retrofit Response object containing a QuizStatsResponse.
     */
    @GET("/api/courses/{courseId}/quiz-stats")
    suspend fun getCourseQuizStats(@Path("courseId") courseId: Long): Response<QuizStatsResponse>

    /**
     *  Retrieves detailed quiz results for a specific quiz.
     * @param quizId The ID of the quiz.
     * @return A Retrofit Response object containing a QuizDetailedResultsResponse.
     */
    @GET("/api/courses/quizzes/{quizId}/detailed-results")
    suspend fun getQuizDetailedResults(@Path("quizId") quizId: Long): Response<QuizDetailedResultsResponse>

    /**
     *  Retrieves all users in the system.
     * @return A UsersResponse object containing a list of all users.
     */
    @GET("/api/courses/users")
    suspend fun getAllUsers(): UsersResponse

    /**
     *  Promotes a user to a teacher role.
     * @param userId The ID of the user to promote.
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @POST("/api/courses/users/{userId}/promote-to-teacher")
    suspend fun promoteToTeacher(@Path("userId") userId: Long): Response<GenericResponse>

    /**
     *  Demotes a user to a student role.
     * @param userId The ID of the user to demote.
     * @return A Retrofit Response object containing a GenericResponse.
     */
    @POST("/api/courses/users/{userId}/demote-to-student")
    suspend fun demoteToStudent(@Path("userId") userId: Long): Response<GenericResponse>
}

/**
 *  Singleton object for providing a Retrofit client instance.
 */
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    /**
     *  Gets an instance of the CourseApiService.
     * Configures OkHttpClient with an interceptor to add JWT token to requests.
     * @param context The Android context, used for accessing SharedPreferences.
     * @return An instance of CourseApiService.
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
object ServerConfig {

    private const val PREFS_NAME = "server_prefs"
    private const val KEY_SERVER_IP = "server_ip"
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