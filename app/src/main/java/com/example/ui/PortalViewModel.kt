package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiHelper
import com.example.data.Applicant
import com.example.data.PortalRepository
import com.example.data.Setting
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class ChatMessage(
    val sender: String, // "USER", "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PortalViewModel(private val repository: PortalRepository) : ViewModel() {

    // Theme and font settings
    val settingState: StateFlow<Setting> = repository.settingFlow
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Setting(1, "GOLD", "GOLD")
        )

    // Applicants list
    val allApplicants: StateFlow<List<Applicant>> = repository.allItemsState()
    private fun PortalRepository.allItemsState(): StateFlow<List<Applicant>> {
        return allApplicants.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Smart assistant chat messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("AI", "مرحباً بك! أنا المساعد الذكي لبوابة الطلبات. كيف يمكنني مساعدتك اليوم؟ يمكنك سؤالي عن شروط التقديم أو حالة الطلبات.")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun updateTheme(theme: String, fontColor: String) {
        viewModelScope.launch {
            repository.updateSetting(theme, fontColor)
        }
    }

    // Submit a new application form
    fun submitApplication(
        context: Context,
        fullName: String,
        appType: String,
        message: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (fullName.trim().isEmpty()) {
                onError("الرجاء إدخال الاسم بالكامل")
                return@launch
            }
            if (message.trim().isEmpty()) {
                onError("الرجاء كتابة تفاصيل الطلب")
                return@launch
            }

            var localImagePath: String? = null
            if (imageUri != null) {
                localImagePath = copyUriToInternalStorage(context, imageUri)
                if (localImagePath == null) {
                    onError("حدث خطأ أثناء مزامنة وحفظ الصورة المرفقة")
                    return@launch
                }
            }

            val newApplicant = Applicant(
                fullName = fullName,
                applicationType = appType,
                message = message,
                status = "PENDING",
                imagePath = localImagePath
            )

            try {
                repository.insertApplicant(newApplicant)
                onSuccess()
            } catch (e: Exception) {
                onError("فشل في تقديم الطلب: ${e.localizedMessage}")
            }
        }
    }

    // Direct local storage copy helper for images to support Admin view on all devices
    private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // Save in cache directory
            val file = File(context.cacheDir, "applicant_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Update status (Accept / Reject)
    fun updateStatus(applicant: Applicant, newStatus: String) {
        viewModelScope.launch {
            repository.updateApplicant(applicant.copy(status = newStatus))
        }
    }

    // Delete application
    fun deleteApplication(id: Int) {
        viewModelScope.launch {
            repository.deleteApplicant(id)
        }
    }

    // Ask Gemini question
    fun askGeminiAssistant(query: String) {
        if (query.trim().isEmpty()) return
        
        viewModelScope.launch {
            val userMsg = ChatMessage("USER", query)
            _chatMessages.value = _chatMessages.value + userMsg
            _isChatLoading.value = true

            // Prompt optimization for Arabic portal response
            val promptContext = "أنت مساعد ذكي لبوابة استقبال الطلبات الحكومية والخاصة المبرمج بذكاء. يرجى توجيه المستخدم باللغة العربية بأسلوب راقي وواضح ومختصر للغاية بما لا يتجاوز ٣ أسطر. السؤال المطروح هو: $query"
            val aiResponse = GeminiHelper.askGemini(promptContext)

            _chatMessages.value = _chatMessages.value + ChatMessage("AI", aiResponse)
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("AI", "مرحباً بك! أنا المساعد الذكي لبوابة الطلبات. كيف يمكنني مساعدتك اليوم؟")
        )
    }
}
