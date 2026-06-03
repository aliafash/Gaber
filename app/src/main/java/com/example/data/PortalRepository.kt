package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PortalRepository(private val portalDao: PortalDao) {

    val allApplicants: Flow<List<Applicant>> = portalDao.getAllApplicants()
    val allSupervisors: Flow<List<Supervisor>> = portalDao.getAllSupervisors()
    val allCategories: Flow<List<Category>> = portalDao.getAllCategories()
    val allServiceProviders: Flow<List<ServiceProvider>> = portalDao.getAllServiceProvidersFlow()
    val allReviews: Flow<List<Review>> = portalDao.getAllReviewsFlow()
    val allActivityLogs: Flow<List<ActivityLog>> = portalDao.getAllActivityLogs()
    
    val settingFlow: Flow<Setting?> = portalDao.getSettingFlow()

    // Applicants CRUD
    suspend fun insertApplicant(applicant: Applicant) {
        portalDao.insertApplicant(applicant)
    }

    suspend fun updateApplicant(applicant: Applicant) {
        portalDao.updateApplicant(applicant)
    }

    suspend fun deleteApplicant(id: Int) {
        portalDao.deleteApplicant(id)
    }

    // Supervisors CRUD
    suspend fun saveSupervisor(supervisor: Supervisor) {
        portalDao.saveSupervisor(supervisor)
    }

    suspend fun updateSupervisor(supervisor: Supervisor) {
        portalDao.updateSupervisor(supervisor)
    }

    suspend fun deleteSupervisor(id: Int) {
        portalDao.deleteSupervisor(id)
    }

    // Categories CRUD
    suspend fun saveCategory(category: Category) {
        portalDao.saveCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        portalDao.updateCategory(category)
    }

    suspend fun deleteCategory(id: Int) {
        portalDao.deleteCategory(id)
    }

    // Service Providers CRUD
    suspend fun saveServiceProvider(provider: ServiceProvider) {
        portalDao.saveServiceProvider(provider)
    }

    suspend fun updateServiceProvider(provider: ServiceProvider) {
        portalDao.updateServiceProvider(provider)
    }

    suspend fun deleteServiceProvider(id: Int) {
        portalDao.deleteServiceProvider(id)
    }
    
    suspend fun getServiceProviderById(id: Int): ServiceProvider? {
        return portalDao.getServiceProviderById(id)
    }

    // Reviews CRUD
    fun getReviewsForProvider(providerId: Int): Flow<List<Review>> {
        return portalDao.getReviewsForProviderFlow(providerId)
    }

    suspend fun saveReview(review: Review) {
        portalDao.saveReview(review)
        updateProviderRatingStats(review.providerId)
    }

    suspend fun deleteReview(reviewId: Int, providerId: Int) {
        portalDao.deleteReview(reviewId)
        updateProviderRatingStats(providerId)
    }

    private suspend fun updateProviderRatingStats(providerId: Int) {
        val provider = portalDao.getServiceProviderById(providerId) ?: return
        val providerReviews = portalDao.getReviewsForProviderFlow(providerId).firstOrNull() ?: emptyList()
        val totalCount = providerReviews.size
        val avgRating = if (totalCount > 0) {
            providerReviews.map { it.rating }.average().toFloat()
        } else {
            5.0f
        }
        portalDao.updateServiceProvider(
            provider.copy(
                ratingAvg = avgRating,
                totalReviewsCount = totalCount
            )
        )
    }

    // Chat messages CRUD
    fun getChatMessagesForProvider(providerId: Int): Flow<List<ChatMessageEntity>> {
        return portalDao.getChatMessagesForProvider(providerId)
    }

    suspend fun saveChatMessage(msg: ChatMessageEntity) {
        portalDao.saveChatMessage(msg)
    }

    // Activity log CRUD
    suspend fun saveActivityLog(log: ActivityLog) {
        portalDao.saveActivityLog(log)
    }

    // Settings
    suspend fun getSetting(): Setting {
        val existing = portalDao.getSetting()
        if (existing == null) {
            val defaultSetting = Setting()
            portalDao.saveSetting(defaultSetting)
            return defaultSetting
        }
        return existing
    }

    suspend fun updateSetting(theme: String, fontColor: String) {
        val current = getSetting()
        portalDao.saveSetting(
            current.copy(
                selectedTheme = theme,
                selectedFontColor = fontColor
            )
        )
    }

    suspend fun updateFullSetting(setting: Setting) {
        portalDao.saveSetting(setting)
    }
}
