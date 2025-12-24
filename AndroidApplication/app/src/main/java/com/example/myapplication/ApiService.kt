package com.example.myapplication

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit

// Модель даних для відповіді API
data class TodoTaskResponse(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: String,
    val imageUrl: String?
)

// Модель користувача
data class UserResponse(
    val id: Int,
    val email: String,
    val name: String,
    val avatarUrl: String?,
    val createdAt: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

// API інтерфейс
interface TaskApiService {
    @GET("api/tasks")
    suspend fun getTasks(): List<TodoTaskResponse>
    
    @Multipart
    @POST("api/tasks")
    suspend fun createTask(
        @Part("title") title: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part image: MultipartBody.Part?
    ): TodoTaskResponse
    
    @PATCH("api/tasks/{id}/toggle")
    suspend fun toggleTask(@Path("id") id: Int): TodoTaskResponse
    
    @PUT("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body request: UpdateTaskRequest): TodoTaskResponse
    
    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int)
    
    // Auth endpoints
    @Multipart
    @POST("api/auth/register")
    suspend fun register(
        @Part("email") email: okhttp3.RequestBody,
        @Part("password") password: okhttp3.RequestBody,
        @Part("name") name: okhttp3.RequestBody,
        @Part avatar: MultipartBody.Part?
    ): UserResponse
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): UserResponse
    
    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): UserResponse
    
    @Multipart
    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Part("name") name: okhttp3.RequestBody,
        @Part avatar: MultipartBody.Part?
    ): UserResponse
}

data class UpdateTaskRequest(
    val title: String?,
    val description: String?,
    val isCompleted: Boolean?
)

data class CreateTaskRequest(
    val title: String,
    val description: String?
)

// Singleton для Retrofit
object RetrofitInstance {
    // Замініть на вашу IP адресу (для емулятора використовуйте 10.0.2.2)
    const val BASE_URL = "http://10.0.2.2:5000"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val api: TaskApiService by lazy {
        Retrofit.Builder()
            .baseUrl("$BASE_URL/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApiService::class.java)
    }
}

// Допоміжні функції для створення запиту
fun createTextRequestBody(text: String): okhttp3.RequestBody {
    return text.toRequestBody("text/plain".toMediaTypeOrNull())
}

fun createImagePart(imageFile: File?): MultipartBody.Part? {
    if (imageFile == null || !imageFile.exists()) return null
    val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("image", imageFile.name, requestBody)
}

fun createAvatarPart(imageFile: File?): MultipartBody.Part? {
    if (imageFile == null || !imageFile.exists()) return null
    val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("avatar", imageFile.name, requestBody)
}
