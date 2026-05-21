package com.streamvault.data.repository

import com.streamvault.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Inject
import javax.inject.Singleton

interface StreamVaultApi {
    @GET("api/videos")
    suspend fun getVideos(): List<Video>

    @GET("api/videos/search")
    suspend fun searchVideos(@Query("q") query: String): List<Video>

    @GET("api/videos/{id}")
    suspend fun getVideo(@Path("id") id: String): Video

    @DELETE("api/videos/{id}")
    suspend fun deleteVideo(@Path("id") id: String): Response<GenericResponse>

    @GET("api/jobs")
    suspend fun getJobs(): List<Job>

    @POST("api/jobs")
    suspend fun createJob(@Body request: AddJobRequest): Job

    @POST("api/jobs/{id}/cancel")
    suspend fun cancelJob(@Path("id") id: String): Job
}

@Singleton
class StreamVaultRepository @Inject constructor(
    private val api: StreamVaultApi,
    private val prefs: PreferencesRepository
) {
    suspend fun getVideos() = runCatching { api.getVideos() }
    suspend fun searchVideos(q: String) = runCatching { api.searchVideos(q) }
    suspend fun getVideo(id: String) = runCatching { api.getVideo(id) }
    suspend fun deleteVideo(id: String) = runCatching { api.deleteVideo(id) }
    suspend fun getJobs() = runCatching { api.getJobs() }
    suspend fun createJob(req: AddJobRequest) = runCatching { api.createJob(req) }
    suspend fun cancelJob(id: String) = runCatching { api.cancelJob(id) }
}

