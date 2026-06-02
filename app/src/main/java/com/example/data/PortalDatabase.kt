package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "applicants")
data class Applicant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val applicationType: String,
    val message: String,
    val status: String, // "PENDING", "ACCEPTED", "REJECTED"
    val imagePath: String?, // Saved local cache file path
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val id: Int = 1,
    val selectedTheme: String = "GOLD", // "SLATE", "GOLD", "EMERALD"
    val selectedFontColor: String = "GOLD", // "WHITE", "GOLD", "SILVER"
    val shareLink: String = "https://ai.studio/build",
    val bannerImageUrl: String = "https://images.unsplash.com/photo-1542744173-8e7e53415bb0?q=80&w=600&auto=format&fit=crop",
    val showAssistant: Boolean = true,
    val assistantIconType: String = "STAR", // "STAR", "LIGHTBULB", "INFO", "CHAT"
    val assistantIconSize: Int = 18,
    val assistantLabel: String = "المساعد"
)

@Dao
interface PortalDao {
    // Applicants
    @Query("SELECT * FROM applicants ORDER BY timestamp DESC")
    fun getAllApplicants(): Flow<List<Applicant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplicant(applicant: Applicant)

    @Update
    suspend fun updateApplicant(applicant: Applicant)

    @Query("DELETE FROM applicants WHERE id = :id")
    suspend fun deleteApplicant(id: Int)

    // Settings
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingFlow(): Flow<Setting?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSetting(): Setting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: Setting)
}

@Database(entities = [Applicant::class, Setting::class], version = 2, exportSchema = false)
abstract class PortalDatabase : RoomDatabase() {
    abstract fun portalDao(): PortalDao
}
