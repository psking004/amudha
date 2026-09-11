package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.DataBrokerItem
import com.example.data.remote.SearchResultItem
import com.example.data.repository.PrivacyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<SearchResultItem>, val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class ProfileFormState(
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val isSavedNotificationVisible: Boolean = false
)

class SelfCheckViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PrivacyRepository

    val brokers: StateFlow<List<DataBrokerItem>>
    val formState = MutableStateFlow(ProfileFormState())

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    // API credentials: Defaults to BuildConfig, with fallback to in-app entry for live testing
    private val _apiKey = MutableStateFlow(BuildConfig.GOOGLE_SEARCH_API_KEY)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _searchEngineId = MutableStateFlow(BuildConfig.GOOGLE_SEARCH_ENGINE_ID)
    val searchEngineId: StateFlow<String> = _searchEngineId.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PrivacyRepository(database.appDao())

        brokers = repository.brokers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PrivacyRepository.defaultBrokers
        )

        // Load saved profile into form
        viewModelScope.launch {
            repository.ensureDefaultBrokers()
            repository.userProfile.collect { profile ->
                formState.update { current ->
                    current.copy(
                        fullName = profile.fullName,
                        email = profile.email,
                        phoneNumber = profile.phoneNumber
                    )
                }
            }
        }
    }

    fun onFullNameChange(value: String) {
        formState.update { it.copy(fullName = value, isSavedNotificationVisible = false) }
    }

    fun onEmailChange(value: String) {
        formState.update { it.copy(email = value, isSavedNotificationVisible = false) }
    }

    fun onPhoneNumberChange(value: String) {
        formState.update { it.copy(phoneNumber = value, isSavedNotificationVisible = false) }
    }

    fun saveProfile() {
        val current = formState.value
        viewModelScope.launch {
            repository.saveProfile(current.fullName, current.email, current.phoneNumber)
            formState.update { it.copy(isSavedNotificationVisible = true) }
        }
    }

    fun toggleBrokerListed(brokerId: String, isListed: Boolean) {
        viewModelScope.launch {
            repository.updateBrokerListed(brokerId, isListed)
        }
    }

    fun updateCustomCredentials(key: String, cx: String) {
        _apiKey.value = key.trim()
        _searchEngineId.value = cx.trim()
    }

    fun runCheck(context: Context) {
        val current = formState.value
        // Save current form values first
        saveProfile()

        // 1. Open user's default browser to HaveIBeenPwned
        openHaveIBeenPwned(context, current.email)

        // 2. Call Google Custom Search API
        val queryParts = listOf(current.fullName, current.email, current.phoneNumber)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val query = queryParts.joinToString(" ")

        if (query.isBlank()) {
            _searchState.value = SearchUiState.Error(
                "Please enter your full name, email, or phone number in the form above to run a privacy search."
            )
            return
        }

        val effectiveKey = _apiKey.value.ifBlank { BuildConfig.GOOGLE_SEARCH_API_KEY }
        val effectiveCx = _searchEngineId.value.ifBlank { BuildConfig.GOOGLE_SEARCH_ENGINE_ID }

        if (effectiveKey.isBlank() || effectiveKey == "YOUR_GOOGLE_SEARCH_API_KEY") {
            _searchState.value = SearchUiState.Error(
                "Google Custom Search API Key is missing. Enter your key in App Settings or via .env (GOOGLE_SEARCH_API_KEY)."
            )
            return
        }

        if (effectiveCx.isBlank() || effectiveCx == "YOUR_SEARCH_ENGINE_ID") {
            _searchState.value = SearchUiState.Error(
                "Search Engine ID (cx) is missing. Enter your ID in App Settings or via .env (GOOGLE_SEARCH_ENGINE_ID)."
            )
            return
        }

        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            val result = repository.searchWeb(
                apiKey = effectiveKey,
                cx = effectiveCx,
                query = query
            )

            result.fold(
                onSuccess = { items ->
                    _searchState.value = SearchUiState.Success(results = items, query = query)
                },
                onFailure = { error ->
                    _searchState.value = SearchUiState.Error(
                        error.localizedMessage ?: "Failed to perform search. Please check your credentials and internet connection."
                    )
                }
            )
        }
    }

    fun openHaveIBeenPwned(context: Context, email: String) {
        try {
            // Copy email to clipboard so user can effortlessly paste it into HaveIBeenPwned
            if (email.isNotBlank()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("email", email.trim())
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    "Opening HaveIBeenPwned... (Email copied to clipboard!)",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // HaveIBeenPwned does not support email in query string on public home page,
            // so we navigate to the home page or account lookup
            val targetUrl = "https://haveibeenpwned.com/"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
