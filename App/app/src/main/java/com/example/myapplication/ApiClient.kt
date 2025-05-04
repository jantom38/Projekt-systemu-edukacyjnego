package com.example.myapplication

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// Interfejs API Retrofit dla wszystkich endpointów
interface CourseApiService {
    @GET("/api/courses")
    suspend fun getAllCourses(): List<Course>

    @POST("/api/courses/{id}/verify-key")
    suspend fun verifyAccessKey(@Path("id") courseId: Long, @Body request: Map<String, String>): Map<String, Any>

    @GET("/api/courses/{id}/files")
    suspend fun getCourseFiles(@Path("id") courseId: Long): List<CourseFile>


    @POST("/api/courses")
    suspend fun createCourse(@Body course: Course): Response<Course>

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
}

// Obiekt Retrofit z interceptorem dla tokenu JWT
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/" // dla emulatora

    fun getInstance(context: Context): CourseApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain: Interceptor.Chain ->
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("jwt_token", null)
                val request = chain.request().newBuilder().apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                        Log.d("RetrofitClient", "Added token to request: $token")
                    } else {
                        Log.w("RetrofitClient", "No token found in SharedPreferences")
                    }
                    addHeader("Accept", "application/json")
                }.build()
                chain.proceed(request)
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
