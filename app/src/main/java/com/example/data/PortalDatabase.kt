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
    val imagePath: String?, // Saved local cache file path for documents
    val idCardImagePath: String?, // Saved local cache file path for ID card
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
    val assistantIconType: String = "STAR", // "STAR", "NOTIFICATIONS", "INFO", "FACE"
    val assistantIconSize: Int = 18,
    val assistantLabel: String = "المساعد",
    
    // Dynamic Form Customizations
    val appTypesList: String = "عام ومراجعة,طلب تصديق وثائق,استفسار طارئ,معاملة تجارية / شركات",
    val nameFieldLabel: String = "الاسم الكامل للمتقدم",
    val isNameFieldRequired: Boolean = true,
    val detailsFieldLabel: String = "تفاصيل الطلب والرسالة",
    val isDetailsFieldVisible: Boolean = true,
    val isDetailsFieldRequired: Boolean = true,
    val documentFieldLabel: String = "إرفاق صورة المستند أو الطلب",
    val isDocumentFieldVisible: Boolean = true,
    val isDocumentFieldRequired: Boolean = false,
    val idCardFieldLabel: String = "صورة لبطاقة الهوية الشخصية",
    val isIdCardFieldVisible: Boolean = true,
    val isIdCardFieldRequired: Boolean = false
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

@Database(entities = [Applicant::class, Setting::class], version = 3, exportSchema = false)
abstract class PortalDatabase : RoomDatabase() {
    abstract fun portalDao(): PortalDao
}
