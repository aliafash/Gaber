package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PortalRepository(private val portalDao: PortalDao) {

    val allApplicants: Flow<List<Applicant>> = portalDao.getAllApplicants()
    val settingFlow: Flow<Setting?> = portalDao.getSettingFlow()

    suspend fun insertApplicant(applicant: Applicant) {
        portalDao.insertApplicant(applicant)
    }

    suspend fun updateApplicant(applicant: Applicant) {
        portalDao.updateApplicant(applicant)
    }

    suspend fun deleteApplicant(id: Int) {
        portalDao.deleteApplicant(id)
    }

    suspend fun getSetting(): Setting {
        val existing = portalDao.getSetting()
        if (existing == null) {
            val defaultSetting = Setting(
                id = 1,
                selectedTheme = "GOLD",
                selectedFontColor = "GOLD",
                shareLink = "https://ai.studio/build",
                bannerImageUrl = "https://images.unsplash.com/photo-1542744173-8e7e53415bb0?q=80&w=600&auto=format&fit=crop",
                showAssistant = true,
                assistantIconType = "STAR",
                assistantIconSize = 18,
                assistantLabel = "المساعد"
            )
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
