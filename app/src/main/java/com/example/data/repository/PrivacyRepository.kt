package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.DataBrokerItem
import com.example.data.local.UserProfile
import com.example.data.remote.GoogleSearchService
import com.example.data.remote.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PrivacyRepository(
    private val appDao: AppDao,
    private val searchService: GoogleSearchService = GoogleSearchService.create()
) {
    val userProfile: Flow<UserProfile> = appDao.getUserProfile().map { profile ->
        profile ?: UserProfile()
    }

    val brokers: Flow<List<DataBrokerItem>> = appDao.getAllBrokers().map { list ->
        if (list.isEmpty()) {
            defaultBrokers
        } else {
            list
        }
    }

    suspend fun ensureDefaultBrokers() = withContext(Dispatchers.IO) {
        appDao.insertBrokers(defaultBrokers)
    }

    suspend fun saveProfile(fullName: String, email: String, phone: String) = withContext(Dispatchers.IO) {
        val entity = UserProfile(
            id = 1,
            fullName = fullName.trim(),
            email = email.trim(),
            phoneNumber = phone.trim(),
            updatedAt = System.currentTimeMillis()
        )
        appDao.saveUserProfile(entity)
    }

    suspend fun updateBrokerListed(id: String, isListed: Boolean) = withContext(Dispatchers.IO) {
        appDao.updateBrokerStatus(id, isListed)
    }

    suspend fun searchWeb(apiKey: String, cx: String, query: String): Result<List<SearchResultItem>> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_SEARCH_API_KEY") {
                return@withContext Result.failure(
                    IllegalArgumentException("Google Custom Search API Key is not configured. Please add your key in AI Studio Secrets or app settings.")
                )
            }
            if (cx.isBlank() || cx == "YOUR_SEARCH_ENGINE_ID") {
                return@withContext Result.failure(
                    IllegalArgumentException("Search Engine ID (cx) is not configured. Please add your Search Engine ID in AI Studio Secrets or app settings.")
                )
            }
            if (query.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Search query is empty. Please enter your name, email, or phone number first.")
                )
            }

            val response = searchService.search(apiKey = apiKey, cx = cx, query = query, num = 10)
            val items = response.items ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val defaultBrokers = listOf(
            DataBrokerItem(
                id = "spokeo",
                name = "Spokeo",
                optOutUrl = "https://www.spokeo.com/optout",
                isListed = false,
                notes = "People search engine that aggregates public records, contact info, and relatives."
            ),
            DataBrokerItem(
                id = "whitepages",
                name = "Whitepages",
                optOutUrl = "https://www.whitepages.com/suppression-requests",
                isListed = false,
                notes = "Online directory with contact info, home addresses, phone numbers, and background checks."
            ),
            DataBrokerItem(
                id = "beenverified",
                name = "BeenVerified",
                optOutUrl = "https://www.beenverified.com/app/optout/search",
                isListed = false,
                notes = "Public records aggregator covering property records, phone lookups, and arrest history."
            )
        )
    }
}
