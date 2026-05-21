package com.streamvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.models.*
import com.streamvault.data.repository.StreamVaultRepository
import com.streamvault.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ── Gallery ──────────────────────────────────────────────────────────────────
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repo: StreamVaultRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredVideos: StateFlow<List<Video>> = combine(_videos, _searchQuery) { videos, q ->
        if (q.isBlank()) videos
        else videos.filter { it.title.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { loadVideos() }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getVideos()
                .onSuccess { _videos.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun deleteVideo(id: String) {
        viewModelScope.launch {
            repo.deleteVideo(id)
                .onSuccess { _videos.update { list -> list.filter { it.id != id } } }
                .onFailure { _error.value = it.message }
        }
    }
}

// ── Admin / Jobs ──────────────────────────────────────────────────────────────
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: StreamVaultRepository
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _addJobState = MutableStateFlow<AddJobState>(AddJobState.Idle)
    val addJobState: StateFlow<AddJobState> = _addJobState

    private var pollJob: kotlinx.coroutines.Job? = null

    init {
        loadJobs()
        startPolling()
    }

    private fun startPolling() {
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(1500)
                repo.getJobs().onSuccess { jobs ->
                    _jobs.value = jobs.sortedByDescending { it.createdAt }
                }
            }
        }
    }

    fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getJobs()
                .onSuccess { _jobs.value = it.sortedByDescending { j -> j.createdAt } }
            _isLoading.value = false
        }
    }

    fun addJob(url: String, title: String, segmentLength: Int) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _addJobState.value = AddJobState.Loading
            repo.createJob(AddJobRequest(url, title, segmentLength))
                .onSuccess {
                    _addJobState.value = AddJobState.Success
                    loadJobs()
                }
                .onFailure { _addJobState.value = AddJobState.Error(it.message ?: "Unknown error") }
        }
    }

    fun cancelJob(id: String) {
        viewModelScope.launch {
            repo.cancelJob(id).onSuccess { loadJobs() }
        }
    }

    fun resetAddState() { _addJobState.value = AddJobState.Idle }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}

sealed class AddJobState {
    object Idle : AddJobState()
    object Loading : AddJobState()
    object Success : AddJobState()
    data class Error(val msg: String) : AddJobState()
}

// ── Player ────────────────────────────────────────────────────────────────────
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repo: StreamVaultRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    private val _video = MutableStateFlow<Video?>(null)
    val video: StateFlow<Video?> = _video

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl

    fun loadVideo(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getVideo(id)
                .onSuccess { video ->
                    _video.value = video
                    val baseUrl = prefs.getBaseUrlOnce()
                    _streamUrl.value = "$baseUrl${video.m3u8Path}"
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}

