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
            val defaultSetting = Setting(1, "GOLD", "GOLD")
            portalDao.saveSetting(defaultSetting)
            return defaultSetting
        }
        return existing
    }

    suspend fun updateSetting(theme: String, fontColor: String) {
        portalDao.saveSetting(Setting(id = 1, selectedTheme = theme, selectedFontColor = fontColor))
    }
}
