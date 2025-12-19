package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Модель даних для відповіді API
data class TodoTaskResponse(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: String
)

// API інтерфейс
interface TaskApiService {
    @GET("api/tasks")
    suspend fun getTasks(): List<TodoTaskResponse>
    
    @POST("api/tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): TodoTaskResponse
    
    @PATCH("api/tasks/{id}/toggle")
    suspend fun toggleTask(@Path("id") id: Int): TodoTaskResponse
    
    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int)
}

data class CreateTaskRequest(
    val title: String,
    val description: String?
)

// Singleton для Retrofit
object RetrofitInstance {
    // Замініть на вашу IP адресу (для емулятора використовуйте 10.0.2.2)
    private const val BASE_URL = "http://10.0.2.2:5000/"
    
    val api: TaskApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApiService::class.java)
    }
}
