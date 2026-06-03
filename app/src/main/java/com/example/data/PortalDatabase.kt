package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "applicants")
data class Applicant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String,
    val category: String,
    val serviceType: String,
    val profileImagePath: String?,  // Mandatory image
    val workAddress: String, // Work center address
    val residenceArea: String, // Residence district
    val mapLocation: String?, // Optional coordinates
    val idCardImagePath: String?, // Optional ID image
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "supervisors")
data class Supervisor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    val is2FaEnabled: Boolean = false,
    val canAddProviders: Boolean = true,
    val canProcessRequests: Boolean = true
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subCategoriesCsv: String = "",
    val isPinned: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String,
    val categoryName: String,
    val subCategoryName: String = "",
    val profileImagePath: String?,
    val workAddress: String,
    val residenceArea: String,
    val mapLocation: String? = null,
    val idCardImagePath: String? = null,
    val isPinnedToTop: Boolean = false,
    val isRecommended: Boolean = false,
    val isVerified: Boolean = true,
    val isBlocked: Boolean = false,
    val ratingAvg: Float = 5.0f,
    val totalReviewsCount: Int = 0,
    val clicksCount: Int = 0
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val senderType: String, // "USER", "PROVIDER", "AI"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMuted: Boolean = false
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val id: Int = 1,
    val selectedTheme: String = "GOLD", // "SLATE", "GOLD", "EMERALD"
    val selectedFontColor: String = "GOLD", // "WHITE", "GOLD", "SILVER"
    val shareLink: String = "https://ai.studio/build",
    val bannerImageUrl: String = "",
    val showAssistant: Boolean = true,
    val assistantIconType: String = "FACE", // "STAR", "NOTIFICATIONS", "INFO", "FACE"
    val assistantIconSize: Int = 18,
    val assistantLabel: String = "خدمات",
    val assistantXPercent: Float = 0.85f,
    val assistantYPercent: Float = 0.85f,
    val assistantColor: String = "GOLD",

    // Header Customization
    val welcomeText: String = "أهلاً بك في بوابة الخدمات الشاملة",
    val welcomeTextSize: Int = 18,
    val welcomeTextColor: String = "GOLD",
    
    // Top Bar Customization flags & descriptions
    val iconRefreshVisible: Boolean = true,
    val iconRefreshText: String = "تحديث",
    val iconLangVisible: Boolean = true,
    val iconLangText: String = "اللغة",
    val iconNightVisible: Boolean = true,
    val iconNightText: String = "الوضع",
    val iconAdminVisible: Boolean = true,
    val iconAdminText: String = "المشرفون",
    val iconRegisterVisible: Boolean = true,
    val iconRegisterText: String = "التسجيل",
    val iconHomeVisible: Boolean = true,
    val iconHomeText: String = "الرئيسية",
    
    // Contact Info (About)
    val contactEmail: String = "ma7777644@gmail.com",
    val contactPhone: String = "777644670",
    val appUpdateCheckEnabled: Boolean = true,
    val customAdvertText: String = "بوابة الخدمات توفر حلولاً متكاملة لتسهيل معاملاتكم اليومية بأمان وسهولة.",
    val appShareLinkUrl: String = "https://ai.studio/build",

    // Center Footer Text
    val footerText: String = "WAM777644670",
    val footerTextVisible: Boolean = true,
    
    // Search Box Custom Text
    val searchPlaceholder: String = "ابحث عن مقدم الخدمة بالاسم أو الرقم...",
    
    // Form Customization
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
    val isIdCardFieldRequired: Boolean = false,

    // Maintenance Mode
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "التطبيق حالياً في وضع الصيانة لترقية الأنظمة. سنعود قريباً!"
)

@Dao
interface PortalDao {
    // Applicants & Registrations
    @Query("SELECT * FROM applicants ORDER BY timestamp DESC")
    fun getAllApplicants(): Flow<List<Applicant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplicant(applicant: Applicant)

    @Update
    suspend fun updateApplicant(applicant: Applicant)

    @Query("DELETE FROM applicants WHERE id = :id")
    suspend fun deleteApplicant(id: Int)

    // Supervisors
    @Query("SELECT * FROM supervisors")
    fun getAllSupervisors(): Flow<List<Supervisor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSupervisor(supervisor: Supervisor)

    @Update
    suspend fun updateSupervisor(supervisor: Supervisor)

    @Query("DELETE FROM supervisors WHERE id = :id")
    suspend fun deleteSupervisor(id: Int)

    // Categories
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)

    // Service Providers
    @Query("SELECT * FROM service_providers")
    fun getAllServiceProvidersFlow(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE id = :id")
    suspend fun getServiceProviderById(id: Int): ServiceProvider?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveServiceProvider(provider: ServiceProvider)

    @Update
    suspend fun updateServiceProvider(provider: ServiceProvider)

    @Query("DELETE FROM service_providers WHERE id = :id")
    suspend fun deleteServiceProvider(id: Int)

    // Reviews
    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY timestamp DESC")
    fun getReviewsForProviderFlow(providerId: Int): Flow<List<Review>>

    @Query("SELECT * FROM reviews ORDER BY timestamp DESC")
    fun getAllReviewsFlow(): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReview(review: Review)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReview(id: Int)

    // Chats
    @Query("SELECT * FROM chat_messages WHERE providerId = :providerId ORDER BY timestamp ASC")
    fun getChatMessagesForProvider(providerId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChatMessage(msg: ChatMessageEntity)

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActivityLog(log: ActivityLog)

    // Settings
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingFlow(): Flow<Setting?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSetting(): Setting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: Setting)
}

@Database(
    entities = [
        Applicant::class,
        Supervisor::class,
        Category::class,
        ServiceProvider::class,
        Review::class,
        ChatMessageEntity::class,
        ActivityLog::class,
        Setting::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PortalDatabase : RoomDatabase() {
    abstract fun portalDao(): PortalDao
}
