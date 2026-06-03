package com.example.data

data class ServiceRequest(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val citizenName: String = "",
    val citizenPhone: String = "",
    val status: String = "PENDING",    // PENDING, IN_PROGRESS, RESOLVED
    val priority: String = "LOW",      // LOW, MEDIUM, HIGH, CRITICAL
    val priorityReason: String = "",   // Recommended by Gemini
    val suggestedCategory: String = "", // Recommended by Gemini
    val resolutionNote: String = "",   // Admin resolution log
    val dateCreated: Long = System.currentTimeMillis()
)

data class Category(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val imageUrl: String = "",
    val isMain: Boolean = true,
    val parentId: String? = null,
    val order: Int = 0
)

data class ServiceProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val address: String = "",
    val district: String = "",
    val gps: String = "",
    val profilePic: String = "",
    val idCardPic: String = "",
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false,
    val rating: Float = 5.0f,
    val ratingCount: Int = 1
)

data class PendingProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val address: String = "",
    val district: String = "",
    val gps: String = "",
    val profilePic: String = "",
    val idCardPic: String = "",
    val status: String = "PENDING",    // PENDING, APPROVED, REJECTED
    val rejectReason: String = "",
    val dateCreated: Long = System.currentTimeMillis()
)

data class Review(
    val id: String = "",
    val providerId: String = "",
    val reviewerName: String = "",
    val rating: Float = 5.0f,
    val comment: String = "",
    val dateCreated: Long = System.currentTimeMillis()
)

data class Banner(
    val id: String = "",
    val imageUrl: String = "",
    val redirectLink: String = "",
    val durationSeconds: Int = 5,
    val dateCreated: Long = System.currentTimeMillis()
)

data class AppConfig(
    val appNameAr: String = "بوابة الخدمات اليمنية",
    val appNameEn: String = "Yemeni Services Portal",
    val primaryColor: String = "#007AFF",   // Hex color
    val secondaryColor: String = "#34C759", // Hex color
    val launcherIcon: String = "ic_default",
    val promoFooter: String = "MAW 777644670",
    val welcomeMessageAr: String = "سهل أعمالك ووكل مهنيك المفضل بكل سهولة وأمان ونظام مبرمج متكامل.",
    val welcomeMessageEn: String = "Simplify your tasks and book your favorite professionals with ease, safety, and an integrated system.",
    val supportPhone: String = "777644670",
    val supportEmail: String = "support@portal.ye",
    val adminPassword: String = "maher736462",
    
    // Smart Assistant Configuration
    val assistantEnabled: Boolean = true,
    val assistantVisible: Boolean = true,
    val assistantPosition: String = "bottom_right", // bottom_right, bottom_left, top_right, top_left
    
    // Top Navigation Icons Configuration
    val navHomeVisible: Boolean = true,
    val navHomeIcon: String = "Home",
    val navHomeOrder: Int = 1,
    val navRegisterVisible: Boolean = true,
    val navRegisterIcon: String = "PersonAdd",
    val navRegisterOrder: Int = 2,
    val navAdminVisible: Boolean = true,
    val navAdminIcon: String = "Lock",
    val navAdminOrder: Int = 3,
    val navLangVisible: Boolean = true,
    val navLangIcon: String = "Language",
    val navLangOrder: Int = 4,
    val navRefreshVisible: Boolean = true,
    val navRefreshIcon: String = "Refresh",
    val navRefreshOrder: Int = 5
)
