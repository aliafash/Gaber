package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiHelper
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class ChatMessage(
    val sender: String, // "USER", "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PortalViewModel(private val repository: PortalRepository) : ViewModel() {

    // Language setting (AR / EN)
    private val _currentLanguage = MutableStateFlow("AR")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "AR") "EN" else "AR"
    }

    // Theme and font settings
    val settingState: StateFlow<Setting> = repository.settingFlow
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Setting()
        )

    // Lists
    val allApplicants: StateFlow<List<Applicant>> = repository.allApplicants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupervisors: StateFlow<List<Supervisor>> = repository.allSupervisors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allServiceProviders: StateFlow<List<ServiceProvider>> = repository.allServiceProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReviews: StateFlow<List<Review>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivityLogs: StateFlow<List<ActivityLog>> = repository.allActivityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Session between User and Specific Provider
    private val _chatMessagesWithProvider = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessagesWithProvider: StateFlow<List<ChatMessageEntity>> = _chatMessagesWithProvider.asStateFlow()

    // Smart assistant chat messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("AI", "مرحباً بك في تطبيق بوابة الخدمات! أنا مساعدك الذكي. كيف يمكنني إرشادك اليوم؟")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Favorites Organization: Map of Folder -> List of Provider IDs
    private val _favoritesByFolder = MutableStateFlow<Map<String, List<Int>>>(
        mapOf("أعمال" to emptyList(), "شخصي" to emptyList())
    )
    val favoritesByFolder: StateFlow<Map<String, List<Int>>> = _favoritesByFolder.asStateFlow()

    init {
        // Hydrate some default Categories if DB is empty
        viewModelScope.launch {
            repository.allCategories.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.saveCategory(Category(name = "صيانة منزلية", subCategoriesCsv = "كهرباء,سباكة,تكييف,تنظيف", isPinned = true, orderIndex = 0))
                    repository.saveCategory(Category(name = "صحة ورعاية", subCategoriesCsv = "طبيب منزل,علاج طبيعي,ممرض,طبيب أطفال", isPinned = true, orderIndex = 1))
                    repository.saveCategory(Category(name = "تعليم وتدريب", subCategoriesCsv = "دروس خصوصية,تعليم لغات,برمجة ومصمم", isPinned = false, orderIndex = 2))
                    repository.saveCategory(Category(name = "نقل وخدمات لوجستية", subCategoriesCsv = "نقل أثاث,توصيل طلبات,شحن ثقيل", isPinned = false, orderIndex = 3))
                }
            }
            repository.allSupervisors.first().let { supervisors ->
                if (supervisors.isEmpty()) {
                    repository.saveSupervisor(Supervisor(username = "maher", password = "123", is2FaEnabled = false))
                }
            }
        }
    }

    fun toggleFavorite(providerId: Int, folder: String) {
        val current = _favoritesByFolder.value.toMutableMap()
        val list = current[folder]?.toMutableList() ?: mutableListOf()
        if (list.contains(providerId)) {
            list.remove(providerId)
        } else {
            list.add(providerId)
        }
        current[folder] = list
        _favoritesByFolder.value = current
    }

    fun createFavoritesFolder(folderName: String) {
        if (folderName.trim().isEmpty()) return
        val current = _favoritesByFolder.value.toMutableMap()
        if (!current.containsKey(folderName)) {
            current[folderName] = emptyList()
            _favoritesByFolder.value = current
        }
    }

    fun loadChatForProvider(providerId: Int) {
        viewModelScope.launch {
            repository.getChatMessagesForProvider(providerId).collect { msgs ->
                _chatMessagesWithProvider.value = msgs
            }
        }
    }

    fun sendChatMessageToProvider(providerId: Int, text: String, senderType: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            val msg = ChatMessageEntity(
                providerId = providerId,
                senderType = senderType,
                messageText = text
            )
            repository.saveChatMessage(msg)
            // Save log
            repository.saveActivityLog(ActivityLog(username = senderType, action = "SEND_MSG", details = "أرسل رسالة إلى الموفر رقم $providerId"))
        }
    }

    fun logAction(username: String, action: String, details: String) {
        viewModelScope.launch {
            repository.saveActivityLog(ActivityLog(username = username, action = action, details = details))
        }
    }

    fun updateTheme(theme: String, fontColor: String) {
        viewModelScope.launch {
            repository.updateSetting(theme, fontColor)
            autoBackupLocal()
        }
    }

    fun updateFullConfig(newSetting: Setting) {
        viewModelScope.launch {
            repository.updateFullSetting(newSetting)
            autoBackupLocal()
        }
    }

    // Export Excel CSV
    fun exportProvidersToCsv(context: Context): String {
        return try {
            val providers = allServiceProviders.value
            val csv = StringBuilder()
            csv.append("ID,Full Name,Phone,Category,Sub-category,Residence,Work Address,Rating, clicks\n")
            providers.forEach {
                csv.append("${it.id},${it.fullName},${it.phone},${it.categoryName},${it.subCategoryName},${it.residenceArea},${it.workAddress},${it.ratingAvg},${it.clicksCount}\n")
            }
            val dir = File("/sdcard/Download")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "service_providers_export.csv")
            file.writeText(csv.toString())
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            "خطأ: " + e.localizedMessage
        }
    }

    fun exportReviewsToCsv(context: Context): String {
        return try {
            val reviews = allReviews.value
            val csv = StringBuilder()
            csv.append("ID,ProviderID,Reviewer,Rating,Comment,Timestamp\n")
            reviews.forEach {
                csv.append("${it.id},${it.providerId},${it.reviewerName},${it.rating},${it.comment},${it.timestamp}\n")
            }
            val dir = File("/sdcard/Download")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "reviews_export.csv")
            file.writeText(csv.toString())
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            "خطأ: " + e.localizedMessage
        }
    }

    // Auto backup local file to download survive uninstall/data-clear
    private fun autoBackupLocal() {
        viewModelScope.launch {
            try {
                val dir = File("/sdcard/Download")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "service_portal_settings_backup.txt")
                val current = settingState.value
                val lines = listOf(
                    current.selectedTheme,
                    current.selectedFontColor,
                    current.shareLink,
                    current.bannerImageUrl,
                    current.showAssistant.toString(),
                    current.assistantIconType,
                    current.assistantIconSize.toString(),
                    current.assistantLabel,
                    current.footerText,
                    current.welcomeText,
                    current.welcomeTextSize.toString(),
                    current.welcomeTextColor,
                    current.contactEmail,
                    current.contactPhone,
                    current.customAdvertText,
                    current.isMaintenanceMode.toString(),
                    current.maintenanceMessage
                )
                file.writeText(lines.joinToString("\n"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun backupDatabaseToDownload(): String {
        return try {
            val dir = File("/sdcard/Download")
            if (!dir.exists()) dir.mkdirs()
            // Write a JSON-alike state export of settings, categories, supervisors to Download/service_portal_db_backup.json
            val backupData = StringBuilder()
            backupData.append("SETTINGS_THEME::${settingState.value.selectedTheme}\n")
            backupData.append("SETTINGS_FONT::${settingState.value.selectedFontColor}\n")
            backupData.append("FOOTER_TEXT::${settingState.value.footerText}\n")
            backupData.append("WELCOME_TEXT::${settingState.value.welcomeText}\n")
            backupData.append("CONTACT_EMAIL::${settingState.value.contactEmail}\n")
            backupData.append("CONTACT_PHONE::${settingState.value.contactPhone}\n")
            
            val file = File(dir, "service_portal_db_backup.txt")
            file.writeText(backupData.toString())
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            "فشل النسخ الاحتياطي: " + e.localizedMessage
        }
    }

    fun checkAndRestoreFromBackup(): Boolean {
        return try {
            val file = File("/sdcard/Download/service_portal_settings_backup.txt")
            if (file.exists()) {
                val lines = file.readLines()
                if (lines.size >= 8) {
                    val restored = Setting(
                        id = 1,
                        selectedTheme = lines.getOrElse(0) { "GOLD" },
                        selectedFontColor = lines.getOrElse(1) { "GOLD" },
                        shareLink = lines.getOrElse(2) { "https://ai.studio/build" },
                        bannerImageUrl = lines.getOrElse(3) { "" },
                        showAssistant = lines.getOrElse(4) { "true" }.toBoolean(),
                        assistantIconType = lines.getOrElse(5) { "FACE" },
                        assistantIconSize = lines.getOrElse(6) { "18" }.toIntOrNull() ?: 18,
                        assistantLabel = lines.getOrElse(7) { "خدمات" },
                        footerText = lines.getOrElse(8) { "WAM777644670" },
                        welcomeText = lines.getOrElse(9) { "أهلاً بك في بوابة الخدمات الشاملة" },
                        welcomeTextSize = lines.getOrElse(10) { "18" }.toIntOrNull() ?: 18,
                        welcomeTextColor = lines.getOrElse(11) { "GOLD" },
                        contactEmail = lines.getOrElse(12) { "ma7777644@gmail.com" },
                        contactPhone = lines.getOrElse(13) { "777644670" },
                        customAdvertText = lines.getOrElse(14) { "بوابة الخدمات توفر حلولاً متكاملة لتسهيل معاملاتكم اليومية بأمان وسهولة." },
                        isMaintenanceMode = lines.getOrElse(15) { "false" }.toBoolean(),
                        maintenanceMessage = lines.getOrElse(16) { "التطبيق حالياً في وضع الصيانة لترقية الأنظمة. سنعود قريباً!" }
                    )
                    viewModelScope.launch {
                        repository.updateFullSetting(restored)
                    }
                    return true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Submit a new professional registration request
    fun submitCandidateApplication(
        context: Context,
        fullName: String,
        phone: String,
        category: String,
        subCategory: String,
        profileImageUri: Uri?,
        workAddress: String,
        residenceArea: String,
        mapLocation: String,
        idCardUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (fullName.split(" ").filter { it.isNotEmpty() }.size < 3) {
                onError("الرجاء إدخال الاسم الثلاثي بالكامل")
                return@launch
            }
            if (phone.trim().isEmpty()) {
                onError("الرجاء إدخال رقم الهاتف للتواصل")
                return@launch
            }
            if (category.trim().isEmpty() || subCategory.trim().isEmpty()) {
                onError("الرجاء اختيار القسم والخدمة المطلوبة")
                return@launch
            }
            if (profileImageUri == null) {
                onError("الصورة الشخصية لمقدم الخدمة إجبارية!")
                return@launch
            }
            if (workAddress.trim().isEmpty()) {
                onError("الرجاء تحديد عنوان ورشة/مكتب العمل الحالي")
                return@launch
            }
            if (residenceArea.trim().isEmpty()) {
                onError("الرجاء تحديد منطقة السكن والإقامة الحالية")
                return@launch
            }

            var localProfilePath: String? = null
            profileImageUri.let { uri ->
                localProfilePath = copyUriToInternalStorage(context, uri)
                if (localProfilePath == null) {
                    onError("فشل في حفظ الصورة الشخصية محلياً")
                    return@launch
                }
            }

            var localIdCardPath: String? = null
            idCardUri?.let { uri ->
                localIdCardPath = copyUriToInternalStorage(context, uri)
            }

            val applicant = Applicant(
                fullName = fullName,
                phone = phone,
                category = category,
                serviceType = subCategory,
                profileImagePath = localProfilePath,
                workAddress = workAddress,
                residenceArea = residenceArea,
                mapLocation = mapLocation.ifEmpty { null },
                idCardImagePath = localIdCardPath
            )

            try {
                repository.insertApplicant(applicant)
                repository.saveActivityLog(ActivityLog(username = "USER_PRO", action = "SUBMIT_APPLY", details = "قدم طلب انضمام باسم $fullName"))
                onSuccess()
            } catch (e: Exception) {
                onError("حدث خطأ أثناء رفع الطلب: ${e.localizedMessage}")
            }
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "pro_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun processRegistrationRequest(applicant: Applicant, approve: Boolean, supervisorName: String) {
        viewModelScope.launch {
            if (approve) {
                // Add to approved service providers
                val provider = ServiceProvider(
                    fullName = applicant.fullName,
                    phone = applicant.phone,
                    categoryName = applicant.category,
                    subCategoryName = applicant.serviceType,
                    profileImagePath = applicant.profileImagePath,
                    workAddress = applicant.workAddress,
                    residenceArea = applicant.residenceArea,
                    mapLocation = applicant.mapLocation,
                    idCardImagePath = applicant.idCardImagePath
                )
                repository.saveServiceProvider(provider)
                repository.deleteApplicant(applicant.id)
                repository.saveActivityLog(ActivityLog(username = supervisorName, action = "APPROVE_REGR", details = "وافق على تسجيل ${applicant.fullName}"))
            } else {
                repository.deleteApplicant(applicant.id)
                repository.saveActivityLog(ActivityLog(username = supervisorName, action = "REJECT_REGR", details = "رفض تسجيل ${applicant.fullName}"))
            }
        }
    }

    fun addProviderDirectly(provider: ServiceProvider, username: String) {
        viewModelScope.launch {
            repository.saveServiceProvider(provider)
            repository.saveActivityLog(ActivityLog(username = username, action = "ADD_PRO_DIRECT", details = "أضاف مقدم خدمة مباشرة: ${provider.fullName}"))
        }
    }

    fun updateProviderPinStatus(provider: ServiceProvider, isPinned: Boolean, username: String) {
        viewModelScope.launch {
            repository.updateServiceProvider(provider.copy(isPinnedToTop = isPinned))
            repository.saveActivityLog(ActivityLog(username = username, action = "PIN_PROVIDER", details = "تعديل تثبيت الموفر ${provider.fullName} إلى $isPinned"))
        }
    }

    fun updateProviderRecommendedStatus(provider: ServiceProvider, isRec: Boolean, username: String) {
        viewModelScope.launch {
            repository.updateServiceProvider(provider.copy(isRecommended = isRec))
            repository.saveActivityLog(ActivityLog(username = username, action = "RECOMMEND_PROVIDER", details = "تعديل التوصية للموفر ${provider.fullName} إلى $isRec"))
        }
    }

    fun deleteProvider(id: Int, username: String) {
        viewModelScope.launch {
            val provider = repository.getServiceProviderById(id)
            repository.deleteServiceProvider(id)
            repository.saveActivityLog(ActivityLog(username = username, action = "DELETE_PROVIDER", details = "حذف مقدم الخدمة: ${provider?.fullName ?: id}"))
        }
    }

    fun addReview(providerId: Int, reviewer: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.saveReview(Review(providerId = providerId, reviewerName = reviewer, rating = rating, comment = comment))
        }
    }

    fun deleteReview(reviewId: Int, providerId: Int, username: String) {
        viewModelScope.launch {
            repository.deleteReview(reviewId, providerId)
            repository.saveActivityLog(ActivityLog(username = username, action = "DELETE_REVIEW", details = "حذف تعليق رقم $reviewId"))
        }
    }

    fun addManagerSupervisor(supervisor: Supervisor, adminName: String) {
        viewModelScope.launch {
            repository.saveSupervisor(supervisor)
            repository.saveActivityLog(ActivityLog(username = adminName, action = "ADD_SUPERVISOR", details = "أضاف المشرف ${supervisor.username}"))
        }
    }

    fun deleteManagerSupervisor(id: Int, adminName: String) {
        viewModelScope.launch {
            repository.deleteSupervisor(id)
            repository.saveActivityLog(ActivityLog(username = adminName, action = "DELETE_SUPERVISOR", details = "حذف مشرف بالمعرف $id"))
        }
    }

    fun addMainCategory(category: Category, adminName: String) {
        viewModelScope.launch {
            repository.saveCategory(category)
            repository.saveActivityLog(ActivityLog(username = adminName, action = "ADD_CATEGORY", details = "أضاف قسم رئيسي ${category.name}"))
        }
    }

    fun deleteMainCategory(id: Int, adminName: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
            repository.saveActivityLog(ActivityLog(username = adminName, action = "DELETE_CATEGORY", details = "حذف القسم المعرف $id"))
        }
    }

    fun incrementClicks(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.updateServiceProvider(provider.copy(clicksCount = provider.clicksCount + 1))
        }
    }

    // Ask Gemini question
    fun askGeminiAssistant(query: String) {
        if (query.trim().isEmpty()) return
        
        viewModelScope.launch {
            val userMsg = ChatMessage("USER", query)
            _chatMessages.value = _chatMessages.value + userMsg
            _isChatLoading.value = true

            val promptContext = "أنت مساعد ذكي لبوابة الخدمات الشاملة للجمهور. وجه المستخدم باللغة العربية باختصار مذهل لا يتجاوز سطرين. السؤال المطروح هو: $query"
            val aiResponse = GeminiHelper.askGemini(promptContext)

            _chatMessages.value = _chatMessages.value + ChatMessage("AI", aiResponse)
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("AI", "مرحباً بك! كيف يمكنني مساعدتك اليوم بخصوص خدماتنا؟")
        )
    }
}
