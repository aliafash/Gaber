package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.api.RequestAnalysisResult
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SmartSummaryState {
    object Idle : SmartSummaryState()
    object Loading : SmartSummaryState()
    data class Success(val summary: String) : SmartSummaryState()
    data class Error(val message: String) : SmartSummaryState()
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object ProcessingAI : SubmissionState() // Analyzing with Gemini
    object Saving : SubmissionState()
    object Success : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

class PortalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PortalRepository(application)
    private val geminiManager = GeminiManager()

    // --- Core Real-time Firebase flows ---
    val allRequests = repository.allRequestsFlow
    val categories = repository.categoriesFlow
    val serviceProviders = repository.serviceProvidersFlow
    val pendingProviders = repository.pendingProvidersFlow
    val reviews = repository.reviewsFlow
    val banners = repository.bannersFlow
    val globalConfig = repository.globalConfigFlow

    // --- Dynamic Interactive States ---
    val activeLanguage = MutableStateFlow("ar") // "ar" or "en"
    val isCitizenRole = MutableStateFlow(true) // true = Citizen/Public client, false = Authenticated Admin/Provider

    // --- Active Form inputs ---
    // A. Service Citizen Requests / Complaints
    val titleState = MutableStateFlow("")
    val categoryState = MutableStateFlow("")
    val descriptionState = MutableStateFlow("")
    val citizenNameState = MutableStateFlow("")
    val citizenPhoneState = MutableStateFlow("")

    // B. Service Provider Registry Form Inputs (👤 Icon Registration Form)
    val regFullNameState = MutableStateFlow("")
    val regPhoneState = MutableStateFlow("")
    val regCategoryIdState = MutableStateFlow("") // Main category selected
    val regAddressState = MutableStateFlow("")
    val regDistrictState = MutableStateFlow("")
    val regGpsState = MutableStateFlow("")
    val regProfilePicState = MutableStateFlow("") // Base64 profile URI mock string or path
    val regIdCardState = MutableStateFlow("") // Base64 ID Card URI

    // C. Direct Manual Service Provider Form (Admin control page)
    val adminManualNameState = MutableStateFlow("")
    val adminManualPhoneState = MutableStateFlow("")
    val adminManualCategoryIdState = MutableStateFlow("")
    val adminManualAddressState = MutableStateFlow("")
    val adminManualProfilePicState = MutableStateFlow("")

    // D. Ad Banner Form inputs (Admin Panel)
    val bannerImageUrlState = MutableStateFlow("")
    val bannerRedirectLinkState = MutableStateFlow("")
    val bannerSecondsState = MutableStateFlow("5")

    // --- Filters properties ---
    val filterStatus = MutableStateFlow("الكل") // "الكل", "قيد الانتظار", "قيد التنفيذ", "تم الحل"
    val filterCategory = MutableStateFlow("الكل")
    val citizenPhoneFilter = MutableStateFlow("") // Filter for specific citizen phone

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    private val _summaryState = MutableStateFlow<SmartSummaryState>(SmartSummaryState.Idle)
    val summaryState: StateFlow<SmartSummaryState> = _summaryState.asStateFlow()

    private val _selectedRequest = MutableStateFlow<ServiceRequest?>(null)
    val selectedRequest: StateFlow<ServiceRequest?> = _selectedRequest.asStateFlow()

    // --- Smart Chatbot Assistant States ---
    val chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(listOf(
        "مرحباً بك! أنا مساعدك الذكي الافتراضي لبوابة الخدمات اليمنية 🇾🇪. يمكنك سؤالي عن أي شيء يخص الخدمات والوظائف والتسجيل والشكاوى." to false
    ))
    val isChatLoading = MutableStateFlow(false)

    fun sendChatMessage(message: String) {
        if (message.trim().isEmpty()) return
        chatMessages.value = chatMessages.value + (message to true)
        viewModelScope.launch {
            isChatLoading.value = true
            val cats = categories.value.map { if (activeLanguage.value == "ar") it.nameAr else it.nameEn }
            val answer = geminiManager.chatWithAssistant(message, cats)
            chatMessages.value = chatMessages.value + (answer to false)
            isChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        chatMessages.value = listOf(
            "مرحباً بك! أنا مساعدك الذكي الافتراضي لبوابة الخدمات اليمنية 🇾🇪. يمكنك سؤالي عن أي شيء يخص الخدمات والوظائف والتسجيل والشكاوى." to false
        )
    }

    init {
        // Set default category states once loaded or keep placeholder
        viewModelScope.launch {
            categories.collect { list ->
                if (list.isNotEmpty() && categoryState.value.isEmpty()) {
                    categoryState.value = list.firstOrNull { it.isMain }?.nameAr ?: "صيانة منزلية"
                }
            }
        }
    }

    // --- Translation engine ---
    fun translate(ar: String, en: String): String {
        return if (activeLanguage.value == "ar") ar else en
    }

    fun toggleLanguage() {
        activeLanguage.value = if (activeLanguage.value == "ar") "en" else "ar"
    }

    fun setRole(citizen: Boolean) {
        isCitizenRole.value = citizen
    }

    fun selectRequest(request: ServiceRequest?) {
        _selectedRequest.value = request
        if (request == null) {
            _summaryState.value = SmartSummaryState.Idle
        } else {
            getSmartSummary(request)
        }
    }

    fun resetSubmission() {
        _submissionState.value = SubmissionState.Idle
    }

    // --- 1. Citizen complaint registration ---
    fun submitServiceRequest() {
        val title = titleState.value.trim()
        val categoryIdx = categoryState.value
        val desc = descriptionState.value.trim()
        val name = citizenNameState.value.trim()
        val phone = citizenPhoneState.value.trim()

        if (title.isEmpty() || desc.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            _submissionState.value = SubmissionState.Error(
                translate("الرجاء ملء جميع الحقول المطلوبة لرفع الطلب.", "Please fill in all required fields to submit.")
            )
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.ProcessingAI

            // Invoke Gemini API for priority prediction & category verification
            val analysis: RequestAnalysisResult = try {
                geminiManager.analyzeNewRequest(title, categoryIdx, desc)
            } catch (e: Exception) {
                RequestAnalysisResult(
                    predictedCategory = categoryIdx,
                    predictedPriority = "LOW",
                    reasoning = translate(
                        "تم تصنيف الطلب تلقائيًا وجاري تحويله للمعاينة الميدانية.",
                        "Request categorized automatically and pending inspection."
                    )
                )
            }

            _submissionState.value = SubmissionState.Saving

            val request = ServiceRequest(
                title = title,
                category = analysis.predictedCategory.ifEmpty { categoryIdx },
                description = desc,
                citizenName = name,
                citizenPhone = phone,
                priority = analysis.predictedPriority,
                priorityReason = analysis.reasoning,
                status = "PENDING"
            )

            try {
                repository.insertRequest(request)
                if (citizenPhoneFilter.value.isEmpty()) {
                    citizenPhoneFilter.value = phone
                }

                // Reset inputs
                titleState.value = ""
                descriptionState.value = ""
                citizenNameState.value = ""
                citizenPhoneState.value = ""

                _submissionState.value = SubmissionState.Success
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error(
                    translate("حدث خطأ أثناء الاتصال أو المزامنة: ", "An error occurred while connecting or syncing: ") + e.message
                )
            }
        }
    }

    fun updateRequestStatus(id: String, newStatus: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRequestStatus(id, newStatus, notes)
            val updated = repository.getRequestById(id)
            if (_selectedRequest.value?.id == id && updated != null) {
                _selectedRequest.value = updated
                getSmartSummary(updated)
            }
        }
    }

    fun getSmartSummary(request: ServiceRequest) {
        viewModelScope.launch {
            _summaryState.value = SmartSummaryState.Loading
            try {
                val summary = geminiManager.generateSmartSummary(
                    title = request.title,
                    category = request.category,
                    description = request.description,
                    status = request.status,
                    resolutionNote = request.resolutionNote
                )
                _summaryState.value = SmartSummaryState.Success(summary)
            } catch (e: Exception) {
                _summaryState.value = SmartSummaryState.Error(
                    translate("تعذر صياغة خارطة الطريق حالياً.", "Unable to compile the dynamic road map.")
                )
            }
        }
    }

    fun deleteRequest(request: ServiceRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRequest(request)
            if (_selectedRequest.value?.id == request.id) {
                _selectedRequest.value = null
            }
        }
    }

    // --- 2. Professional Provider Registration Form (👤 Form) ---
    fun submitProviderRegistration() : String {
        val name = regFullNameState.value.trim()
        val phone = regPhoneState.value.trim()
        val categoryId = regCategoryIdState.value
        val address = regAddressState.value.trim()
        val district = regDistrictState.value.trim()
        val gps = regGpsState.value.trim()
        val profilePic = regProfilePicState.value
        val idCard = regIdCardState.value

        if (name.isEmpty() || phone.isEmpty() || categoryId.isEmpty() || address.isEmpty() || district.isEmpty() || profilePic.isEmpty()) {
            return translate("الرجاء ملء جميع الحقول المطلوبة ورفع الصورة الشخصية.", "Please fill in all required fields and upload your profile picture.")
        }

        viewModelScope.launch {
            val pending = PendingProvider(
                name = name,
                phone = phone,
                categoryId = categoryId,
                address = address,
                district = district,
                gps = gps,
                profilePic = profilePic,
                idCardPic = idCard,
                status = "PENDING",
                dateCreated = System.currentTimeMillis()
            )
            repository.submitRegistrationRequest(pending)
            
            // Success cleanup
            regFullNameState.value = ""
            regPhoneState.value = ""
            regAddressState.value = ""
            regDistrictState.value = ""
            regGpsState.value = ""
            regProfilePicState.value = ""
            regIdCardState.value = ""
        }
        return "SUCCESS"
    }

    // --- 3. Dynamic Interactive Admin Actions ---
    fun insertCategoryDirect(nameAr: String, nameEn: String, parentId: String?, isMain: Boolean, order: Int) {
        viewModelScope.launch {
            val category = Category(
                nameAr = nameAr,
                nameEn = nameEn,
                parentId = parentId,
                isMain = isMain,
                order = order
            )
            repository.insertCategory(category)
        }
    }

    fun deleteCategoryDirect(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun submitManualProvider() : String {
        val name = adminManualNameState.value.trim()
        val phone = adminManualPhoneState.value.trim()
        val categoryId = adminManualCategoryIdState.value
        val address = adminManualAddressState.value.trim()
        val profile = adminManualProfilePicState.value

        if (name.isEmpty() || phone.isEmpty() || categoryId.isEmpty() || address.isEmpty()) {
            return translate("الرجاء تعبئة البيانات بالكامل لإضافة مقدم الخدمة السريع.", "Please fill in all details for adding quick provider.")
        }

        viewModelScope.launch {
            val provider = ServiceProvider(
                name = name,
                phone = phone,
                categoryId = categoryId,
                address = address,
                profilePic = profile.ifEmpty { "ic_default" },
                isPinned = false,
                isRecommended = false,
                rating = 5.0f,
                ratingCount = 1
            )
            repository.insertServiceProvider(provider)
            
            // Clean fields
            adminManualNameState.value = ""
            adminManualPhoneState.value = ""
            adminManualAddressState.value = ""
            adminManualProfilePicState.value = ""
        }
        return "SUCCESS"
    }

    fun approveRegistration(pending: PendingProvider) {
        viewModelScope.launch {
            repository.approveRegistrationRequest(pending)
        }
    }

    fun rejectRegistration(pendingId: String, reason: String) {
        viewModelScope.launch {
            repository.rejectRegistrationRequest(pendingId, reason)
        }
    }

    fun toggleRecommendProvider(providerId: String, isRecommended: Boolean) {
        viewModelScope.launch {
            val prov = serviceProviders.value.find { it.id == providerId } ?: return@launch
            repository.updateServiceProviderProperties(providerId, isPinned = prov.isPinned, isRecommended = isRecommended)
        }
    }

    fun togglePinProvider(providerId: String, isPinned: Boolean) {
        viewModelScope.launch {
            val prov = serviceProviders.value.find { it.id == providerId } ?: return@launch
            repository.updateServiceProviderProperties(providerId, isPinned = isPinned, isRecommended = prov.isRecommended)
        }
    }

    fun deleteServiceProviderDirect(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.deleteServiceProvider(provider)
        }
    }

    // --- 4. Dynamic Ad Banners Management ---
    fun submitNewBanner() : String {
        val url = bannerImageUrlState.value.trim()
        val redirect = bannerRedirectLinkState.value.trim()
        val seconds = bannerSecondsState.value.toIntOrNull() ?: 5

        if (url.isEmpty()) {
            return translate("الرجاء إدخال عنوان صورة صالح للإعلان.", "Please specify a valid image link for banner advertisement.")
        }

        viewModelScope.launch {
            val banner = Banner(
                imageUrl = url,
                redirectLink = redirect,
                durationSeconds = seconds,
                dateCreated = System.currentTimeMillis()
            )
            repository.insertBanner(banner)
            
            bannerImageUrlState.value = ""
            bannerRedirectLinkState.value = ""
            bannerSecondsState.value = "5"
        }
        return "SUCCESS"
    }

    fun deleteBannerDirect(banner: Banner) {
        viewModelScope.launch {
            repository.deleteBanner(banner)
        }
    }

    // --- 5. Secret Config (Backdoor) Updates ---
    fun saveGlobalConfig(config: AppConfig) {
        repository.saveAppConfig(config)
    }

    fun updateBackdoorSettings(
        appNameAr: String,
        appNameEn: String,
        primaryHexColor: String,
        secondaryHexColor: String,
        promoFooter: String,
        welcomeAr: String,
        welcomeEn: String,
        phone: String,
        email: String,
        adminPass: String
    ) {
        val current = globalConfig.value
        val config = current.copy(
            appNameAr = appNameAr.ifEmpty { current.appNameAr },
            appNameEn = appNameEn.ifEmpty { current.appNameEn },
            primaryColor = primaryHexColor.ifEmpty { current.primaryColor },
            secondaryColor = secondaryHexColor.ifEmpty { current.secondaryColor },
            promoFooter = promoFooter.ifEmpty { current.promoFooter },
            welcomeMessageAr = welcomeAr.ifEmpty { current.welcomeMessageAr },
            welcomeMessageEn = welcomeEn.ifEmpty { current.welcomeMessageEn },
            supportPhone = phone.ifEmpty { current.supportPhone },
            supportEmail = email.ifEmpty { current.supportEmail },
            adminPassword = adminPass.ifEmpty { current.adminPassword }
        )
        repository.saveAppConfig(config)
    }

    // Color conversion utilities
    fun getPrimaryColor(): Color {
        return parseHexColor(globalConfig.value.primaryColor, Color(0xFF007AFF))
    }

    fun getSecondaryColor(): Color {
        return parseHexColor(globalConfig.value.secondaryColor, Color(0xFF34C759))
    }

    fun parseHexColor(hex: String, fallback: Color): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            fallback
        }
    }
}
