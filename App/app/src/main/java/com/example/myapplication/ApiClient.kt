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

// Existing data classes remain unchanged
data class UserCourseInfo(
    val id: Long,
    val username: String,
    val role: String,
    val joinedAt: String? = null
)

data class QuizAnswerDTO(
    val questionId: Long,
    val answer: String
)

data class Quiz(
    val id: Long? = null,
    val title: String,
    val description: String? = null,
    val createdAt: String? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val numberOfQuestionsToDisplay: Int? = null
)

data class Quizsolve(
    val id: Long,
    val title: String,
    val description: String?,
    val questions: List<QuizQuestion>
)

data class QuizQuestion(
    @SerializedName("questionId")
    val id: Long? = null,
    val questionText: String,
    val questionType: String,
    val options: Map<String, String>?,
    val correctAnswer: String,
    val quizId: Long? = null
)

data class QuizHistoryResponse(
    val success: Boolean,
    val results: List<QuizHistoryItem>
)

data class QuizHistoryItem(
    val date: String,
    val score: String
)

data class SubmissionResultDTO(
    val success: Boolean,
    val score: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Double
)

data class QuizListResponse(val success: Boolean, val quizzes: List<Quiz>)

data class QuizResponse(
    val success: Boolean,
    val quiz: Quiz,
    val message: String
)

data class QuestionResponse(val success: Boolean, val message: String, val question: QuizQuestion)

data class GenericResponse(
    val success: Boolean,
    val message: String
)

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val success: Boolean, val token: String?, val role: String?)

data class RegisterRequest(val username: String, val password: String, val roleCode: String)

data class RegisterResponse(val success: Boolean, val message: String)

data class GenerateCodeRequest(val validity: String)

data class GenerateCodeResponse(val success: Boolean, val code: String, val expiresAt: String, val message: String)

data class QuizStat(
    val quizId: Long,
    val quizTitle: String,
    val attempts: Long,
    val averageScore: Double
)

data class QuizDetailedResult(
    val userId: Long,
    val username: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val score: Double,
    val completionDate: String,
    val answers: List<Map<String, Any>>
)

data class QuizStatsResponse(
    val success: Boolean,
    val courseId: Long,
    val stats: List<QuizStat>
)

data class QuizDetailedResultsResponse(
    val success: Boolean,
    val quizId: Long,
    val quizTitle: String,
    val results: List<QuizDetailedResult>
)

data class CourseUsersResponse(
    val success: Boolean,
    val users: List<UserCourseInfo>
)

data class UsersResponse(
    val success: Boolean,
    val users: List<UserCourseInfo>
)

interface CourseApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/courses/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/api/courses/auth/generate-student-code")
    suspend fun generateStudentCode(@Body request: GenerateCodeRequest): Response<GenerateCodeResponse>

    @GET("/api/courses")
    suspend fun getAllCourses(): List<Course>

    @POST("/api/courses/{id}/verify-key")
    suspend fun verifyAccessKey(
        @Path("id") courseId: Long,
        @Body request: Map<String, String>
    ): Map<String, Any>

    @GET("/api/courses/{id}/files")
    suspend fun getCourseFiles(@Path("id") courseId: Long): List<CourseFile>

    @POST("/api/courses")
    suspend fun createCourse(@Body course: Course): Response<Course>

    @DELETE("/api/courses/{id}")
    suspend fun deleteCourse(@Path("id") id: Long): Response<Map<String, Any>>

    @GET("/api/courses/{courseId}/users")
    suspend fun getCourseUsers(
        @Path("courseId") courseId: Long
    ): CourseUsersResponse

    @DELETE("/api/courses/{courseId}/users/{userId}")
    suspend fun removeUserFromCourse(
        @Path("courseId") courseId: Long,
        @Path("userId") userId: Long
    ): Response<GenericResponse>

    @Multipart
    @POST("/api/courses/{courseId}/files/upload")
    suspend fun uploadFile(
        @Path("courseId") courseId: Long,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @DELETE("/api/courses/{courseId}/files/{fileId}")
    suspend fun deleteCourseFile(
        @Path("courseId") courseId: Long,
        @Path("fileId") fileId: Long
    ): Response<ResponseBody>

    @GET("/api/courses/my-courses")
    suspend fun getUserCourses(): Map<String, Any>

    @GET("/api/courses/{id}/quizzes")
    suspend fun getCourseQuizzes(@Path("id") courseId: Long): QuizListResponse

    @POST("/api/courses/{id}/quizzes")
    suspend fun createQuiz(
        @Path("id") courseId: Long,
        @Body quiz: Quiz
    ): Response<QuizResponse>

    @PUT("/api/courses/quizzes/{quizId}")
    suspend fun updateQuiz(
        @Path("quizId") quizId: Long,
        @Body quiz: Quiz
    ): Response<QuizResponse>

    @POST("/api/courses/quizzes/{quizId}/questions")
    suspend fun createQuizQuestion(
        @Path("quizId") quizId: Long,
        @Body question: QuizQuestion
    ): Response<QuestionResponse>

    @PUT("/api/courses/quizzes/{quizId}/questions/{questionId}")
    suspend fun updateQuizQuestion(
        @Path("quizId") quizId: Long,
        @Path("questionId") questionId: Long,
        @Body question: QuizQuestion
    ): Response<QuestionResponse>

    @DELETE("/api/courses/quizzes/{quizId}")
    suspend fun deleteQuiz(@Path("quizId") quizId: Long): Response<GenericResponse>

    @DELETE("/api/courses/quizzes/{quizId}/questions/{questionId}")
    suspend fun deleteQuizQuestion(
        @Path("quizId") quizId: Long,
        @Path("questionId") questionId: Long
    ): Response<GenericResponse>

    @GET("/api/courses/quizzes/{quizId}")
    suspend fun getQuiz(@Path("quizId") quizId: Long): Response<QuizResponse>

    @POST("/api/courses/quizzes/{quizId}/submit")
    suspend fun submitQuizAnswers(
        @Path("quizId") quizId: Long,
        @Body answers: List<QuizAnswerDTO>
    ): Response<SubmissionResultDTO>

    @GET("/api/courses/quizzes/{quizId}/results")
    suspend fun getQuizResult(@Path("quizId") quizId: Long): Response<QuizResult>

    @GET("/api/courses/{courseId}/quiz-stats")
    suspend fun getCourseQuizStats(@Path("courseId") courseId: Long): Response<QuizStatsResponse>

    @GET("/api/courses/quizzes/{quizId}/detailed-results")
    suspend fun getQuizDetailedResults(@Path("quizId") quizId: Long): Response<QuizDetailedResultsResponse>

    @GET("/api/courses/users")
    suspend fun getAllUsers(): UsersResponse

    @POST("/api/courses/users/{userId}/promote-to-teacher")
    suspend fun promoteToTeacher(@Path("userId") userId: Long): Response<GenericResponse>

    @POST("/api/courses/users/{userId}/demote-to-student")
    suspend fun demoteToStudent(@Path("userId") userId: Long): Response<GenericResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.160.18:8080/"

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

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CourseApiService::class.java)
    }
}