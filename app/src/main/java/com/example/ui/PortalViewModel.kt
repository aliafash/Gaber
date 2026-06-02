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
            initialValue = Setting(
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
            autoBackupLocal()
        }
    }

    fun updateFullConfig(newSetting: Setting) {
        viewModelScope.launch {
            repository.updateFullSetting(newSetting)
            autoBackupLocal()
        }
    }

    // Auto backup local file that survives normal application data wipe
    private fun autoBackupLocal() {
        viewModelScope.launch {
            try {
                // Persistent folder that survives standard uninstall on several Android versions
                val dir = File("/sdcard/Download")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "smart_portal_settings_backup.txt")
                val current = settingState.value
                val data = "${current.selectedTheme};${current.selectedFontColor};${current.shareLink};${current.bannerImageUrl};${current.showAssistant};${current.assistantIconType};${current.assistantIconSize};${current.assistantLabel}"
                file.writeText(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Automatic restore settings if data was deleted or reinstalled
    fun checkAndRestoreFromBackup(): Boolean {
        return try {
            val file = File("/sdcard/Download/smart_portal_settings_backup.txt")
            if (file.exists()) {
                val data = file.readText().split(";")
                if (data.size >= 8) {
                    val restored = Setting(
                        id = 1,
                        selectedTheme = data[0],
                        selectedFontColor = data[1],
                        shareLink = data[2],
                        bannerImageUrl = data[3],
                        showAssistant = data[4].toBoolean(),
                        assistantIconType = data[5],
                        assistantIconSize = data[6].toIntOrNull() ?: 18,
                        assistantLabel = data[7]
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
