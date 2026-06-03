package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class PortalRepository(private val context: Context) {

    private var firestore: FirebaseFirestore? = null
    private val prefs: SharedPreferences = context.getSharedPreferences("portal_config_prefs", Context.MODE_PRIVATE)

    // State flows representing real-time synchronized collections
    val allRequestsFlow = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    val serviceProvidersFlow = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val pendingProvidersFlow = MutableStateFlow<List<PendingProvider>>(emptyList())
    val reviewsFlow = MutableStateFlow<List<Review>>(emptyList())
    val bannersFlow = MutableStateFlow<List<Banner>>(emptyList())
    val globalConfigFlow = MutableStateFlow(loadConfigFromPrefs())

    // Public getters matching original repository pattern
    val allRequests: Flow<List<ServiceRequest>> = allRequestsFlow

    init {
        try {
            // Check if default FirebaseApp is initialized, otherwise initialize with dummy defaults
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("mock-api-key-maw-777644670")
                    .setApplicationId("com.aistudio.yemeniservicesportal")
                    .setProjectId("yemeni-services-portal")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }
            val db = FirebaseFirestore.getInstance()
            
            // Set persistence enabled to TRUE as requested by the user
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            firestore = db
            Log.d("PortalRepository", "Firebase Firestore initialized with offline persistence.")
        } catch (e: Exception) {
            Log.e("PortalRepository", "Firebase initialization failed. Working in solo/local persistence mode.", e)
        }

        // Start dynamic listeners
        startListening()
    }

    private fun startListening() {
        val db = firestore
        if (db == null) {
            setupMockMemoryData()
            return
        }

        // 1. Service requests
        db.collection("service_requests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PortalRepository", "Listen failed for service_requests", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceRequest::class.java).copy(id = doc.id)
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    allRequestsFlow.value = list.sortedByDescending { it.dateCreated }
                }
            }

        // 2. Categories
        db.collection("categories")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PortalRepository", "Listen failed for categories", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        try {
                            doc.toObject(Category::class.java).copy(id = doc.id)
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    if (list.isEmpty()) {
                        seedDefaultCategories()
                    } else {
                        categoriesFlow.value = list.sortedBy { it.order }
                    }
                }
            }

        // 3. Service Providers
        db.collection("service_providers")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PortalRepository", "Listen failed for service_providers", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceProvider::class.java).copy(id = doc.id)
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    serviceProvidersFlow.value = list
                }
            }

        // 4. Pending Providers (Registration Requests)
        db.collection("pending_providers")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PortalRepository", "Listen failed for pending_providers", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        try {
                            doc.toObject(PendingProvider::class.java).copy(id = doc.id)
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    pendingProvidersFlow.value = list.sortedByDescending { it.dateCreated }
                }
            }

        // 5. Reviews
        db.collection("reviews")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PortalRepository", "Listen failed for reviews", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        try {
                            doc.toObject(Review::class.java).copy(id = doc.id)
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    reviewsFlow.value = list
                }
            }

        // 6. Banners
        db.collection("banners")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PortalRepository", "Listen failed for banners", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { doc ->
                        try {
                            doc.toObject(Banner::class.java).copy(id = doc.id)
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    if (list.isEmpty()) {
                        seedDefaultBanners()
                    } else {
                        bannersFlow.value = list.sortedByDescending { it.dateCreated }
                    }
                }
            }

        // 7. Global App Configurations (Backdoor)
        db.collection("configs").document("global_config")
            .addSnapshotListener { doc, e ->
                if (e != null) {
                    Log.e("PortalRepository", "Listen failed for configs", e)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    try {
                        val config = doc.toObject(AppConfig::class.java)
                        if (config != null) {
                            globalConfigFlow.value = config
                            saveConfigToPrefs(config)
                        }
                    } catch (ex: Exception) {
                        Log.e("PortalRepository", "Error parsing AppConfig", ex)
                    }
                } else {
                    // Seed initial config
                    saveAppConfig(loadConfigFromPrefs())
                }
            }
    }

    private fun setupMockMemoryData() {
        // Fallback mock setups
        categoriesFlow.value = getMockCategories()
        bannersFlow.value = getMockBanners()
    }

    // --- Service Requests Procedures ---
    fun getRequestsByCitizen(phone: String): Flow<List<ServiceRequest>> {
        return allRequests.map { list ->
            list.filter { it.citizenPhone == phone }
        }
    }

    fun getRequestByIdFlow(id: String): Flow<ServiceRequest?> {
        return allRequests.map { list ->
            list.find { it.id == id }
        }
    }

    suspend fun getRequestById(id: String): ServiceRequest? {
        return allRequestsFlow.value.find { it.id == id }
    }

    suspend fun insertRequest(request: ServiceRequest): String {
        val db = firestore
        if (db != null) {
            val docRef = db.collection("service_requests").document()
            val finalReq = request.copy(id = docRef.id)
            docRef.set(finalReq)
            return finalReq.id
        } else {
            val localId = "req_" + System.currentTimeMillis()
            val finalReq = request.copy(id = localId)
            allRequestsFlow.value = (listOf(finalReq) + allRequestsFlow.value)
            return localId
        }
    }

    suspend fun updateRequest(request: ServiceRequest) {
        val db = firestore
        if (db != null) {
            db.collection("service_requests").document(request.id).set(request)
        } else {
            allRequestsFlow.value = allRequestsFlow.value.map {
                if (it.id == request.id) request else it
            }
        }
    }

    suspend fun updateRequestStatus(id: String, status: String, resolutionNote: String) {
        val db = firestore
        if (db != null) {
            db.collection("service_requests").document(id)
                .update("status", status, "resolutionNote", resolutionNote)
        } else {
            allRequestsFlow.value = allRequestsFlow.value.map {
                if (it.id == id) it.copy(status = status, resolutionNote = resolutionNote) else it
            }
        }
    }

    suspend fun deleteRequest(request: ServiceRequest) {
        val db = firestore
        if (db != null) {
            db.collection("service_requests").document(request.id).delete()
        } else {
            allRequestsFlow.value = allRequestsFlow.value.filterNot { it.id == request.id }
        }
    }

    // --- Category Management ---
    suspend fun insertCategory(category: Category) {
        val db = firestore
        val finalCat = if (category.id.isEmpty()) {
            val ref = db?.collection("categories")?.document()
            category.copy(id = ref?.id ?: "cat_${System.currentTimeMillis()}")
        } else {
            category
        }

        if (db != null) {
            db.collection("categories").document(finalCat.id).set(finalCat)
        } else {
            categoriesFlow.value = (categoriesFlow.value + finalCat).sortedBy { it.order }
        }
    }

    suspend fun deleteCategory(category: Category) {
        val db = firestore
        if (db != null) {
            db.collection("categories").document(category.id).delete()
        } else {
            categoriesFlow.value = categoriesFlow.value.filterNot { it.id == category.id }
        }
    }

    private fun seedDefaultCategories() {
        val defaultCats = getMockCategories()
        val db = firestore ?: return
        for (cat in defaultCats) {
            val ref = db.collection("categories").document()
            db.collection("categories").document(ref.id).set(cat.copy(id = ref.id))
        }
    }

    private fun getMockCategories(): List<Category> {
        return listOf(
            Category(id = "c1", nameAr = "صيانة منزلية", nameEn = "Home Maintenance", imageUrl = "", isMain = true, parentId = null, order = 1),
            Category(id = "c2", nameAr = "صحة ورعاية", nameEn = "Health and Care", imageUrl = "", isMain = true, parentId = null, order = 2),
            Category(id = "c3", nameAr = "تعليم وتدريب", nameEn = "Education & Training", imageUrl = "", isMain = true, parentId = null, order = 3),
            Category(id = "c4", nameAr = "نقل وخدمات", nameEn = "Transport & Support", imageUrl = "", isMain = true, parentId = null, order = 4),
            
            // Subcategories
            Category(id = "sub1", nameAr = "كهرباء منازل", nameEn = "Residential Electrician", imageUrl = "", isMain = false, parentId = "c1", order = 1),
            Category(id = "sub2", nameAr = "سباكة وتوصيل صيانة", nameEn = "Plumbing Works", imageUrl = "", isMain = false, parentId = "c1", order = 2),
            Category(id = "sub3", nameAr = "طبيب منزلي", nameEn = "Home Care Doctor", imageUrl = "", isMain = false, parentId = "c2", order = 3),
            Category(id = "sub4", nameAr = "مدرس خصوصي لغات الكبار", nameEn = "Language Private Tutor", imageUrl = "", isMain = false, parentId = "c3", order = 4),
            Category(id = "sub5", nameAr = "شحن ونقل العفش والآثاث", nameEn = "Luggage and Furniture Moving", imageUrl = "", isMain = false, parentId = "c4", order = 5)
        )
    }

    // --- Service Provider Management ---
    suspend fun insertServiceProvider(provider: ServiceProvider) {
        val db = firestore
        val finalProvider = if (provider.id.isEmpty()) {
            val ref = db?.collection("service_providers")?.document()
            provider.copy(id = ref?.id ?: "prov_${System.currentTimeMillis()}")
        } else {
            provider
        }

        if (db != null) {
            db.collection("service_providers").document(finalProvider.id).set(finalProvider)
        } else {
            serviceProvidersFlow.value = serviceProvidersFlow.value.filterNot { it.id == finalProvider.id } + finalProvider
        }
    }

    suspend fun updateServiceProviderProperties(providerId: String, isPinned: Boolean, isRecommended: Boolean) {
        val db = firestore
        if (db != null) {
            db.collection("service_providers").document(providerId)
                .update("isPinned", isPinned, "isRecommended", isRecommended)
        } else {
            serviceProvidersFlow.value = serviceProvidersFlow.value.map {
                if (it.id == providerId) it.copy(isPinned = isPinned, isRecommended = isRecommended) else it
            }
        }
    }

    suspend fun deleteServiceProvider(provider: ServiceProvider) {
        val db = firestore
        if (db != null) {
            db.collection("service_providers").document(provider.id).delete()
        } else {
            serviceProvidersFlow.value = serviceProvidersFlow.value.filterNot { it.id == provider.id }
        }
    }

    // --- Pending Registry Management ---
    suspend fun submitRegistrationRequest(pending: PendingProvider): String {
        val db = firestore
        val docId = db?.collection("pending_providers")?.document()?.id ?: "pend_${System.currentTimeMillis()}"
        val finalPending = pending.copy(id = docId, status = "PENDING")
        if (db != null) {
            db.collection("pending_providers").document(docId).set(finalPending)
        } else {
            pendingProvidersFlow.value = (listOf(finalPending) + pendingProvidersFlow.value)
        }
        return docId
    }

    suspend fun approveRegistrationRequest(pending: PendingProvider) {
        // 1. Move to service_providers
        val provider = ServiceProvider(
            id = pending.id,
            name = pending.name,
            phone = pending.phone,
            categoryId = pending.categoryId,
            address = pending.address,
            district = pending.district,
            gps = pending.gps,
            profilePic = pending.profilePic,
            idCardPic = pending.idCardPic,
            isPinned = false,
            isRecommended = false,
            rating = 5.0f,
            ratingCount = 1
        )
        insertServiceProvider(provider)

        // 2. Clear or update registration status to APPROVED
        val db = firestore
        if (db != null) {
            db.collection("pending_providers").document(pending.id).update("status", "APPROVED")
        } else {
            pendingProvidersFlow.value = pendingProvidersFlow.value.map {
                if (it.id == pending.id) it.copy(status = "APPROVED") else it
            }
        }
    }

    suspend fun rejectRegistrationRequest(pendingId: String, reason: String) {
        val db = firestore
        if (db != null) {
            db.collection("pending_providers").document(pendingId)
                .update("status", "REJECTED", "rejectReason", reason)
        } else {
            pendingProvidersFlow.value = pendingProvidersFlow.value.map {
                if (it.id == pendingId) it.copy(status = "REJECTED", rejectReason = reason) else it
            }
        }
    }

    // --- Review System ---
    suspend fun submitReview(review: Review) {
        val db = firestore
        val finalReview = if (review.id.isEmpty()) {
            val ref = db?.collection("reviews")?.document()
            review.copy(id = ref?.id ?: "rev_${System.currentTimeMillis()}")
        } else {
            review
        }

        if (db != null) {
            db.collection("reviews").document(finalReview.id).set(finalReview)
        } else {
            reviewsFlow.value = reviewsFlow.value + finalReview
        }

        // Recalculate ServiceProvider average rating from new review
        recalculateProviderRating(review.providerId)
    }

    private suspend fun recalculateProviderRating(providerId: String) {
        val currentReviews = reviewsFlow.value.filter { it.providerId == providerId }
        if (currentReviews.isEmpty()) return
        val totalStars = currentReviews.sumOf { it.rating.toDouble() }.toFloat()
        val count = currentReviews.size
        val avg = totalStars / count

        val db = firestore
        if (db != null) {
            db.collection("service_providers").document(providerId)
                .update("rating", avg, "ratingCount", count)
        } else {
            serviceProvidersFlow.value = serviceProvidersFlow.value.map {
                if (it.id == providerId) it.copy(rating = avg, ratingCount = count) else it
            }
        }
    }

    // --- Dynamic Banner Management ---
    suspend fun insertBanner(banner: Banner) {
        val db = firestore
        val finalBanner = if (banner.id.isEmpty()) {
            val ref = db?.collection("banners")?.document()
            banner.copy(id = ref?.id ?: "banner_${System.currentTimeMillis()}")
        } else {
            banner
        }

        if (db != null) {
            db.collection("banners").document(finalBanner.id).set(finalBanner)
        } else {
            bannersFlow.value = (listOf(finalBanner) + bannersFlow.value)
        }
    }

    suspend fun deleteBanner(banner: Banner) {
        val db = firestore
        if (db != null) {
            db.collection("banners").document(banner.id).delete()
        } else {
            bannersFlow.value = bannersFlow.value.filterNot { it.id == banner.id }
        }
    }

    private fun seedDefaultBanners() {
        val mockBanners = getMockBanners()
        val db = firestore ?: return
        for (b in mockBanners) {
            val ref = db.collection("banners").document()
            db.collection("banners").document(ref.id).set(b.copy(id = ref.id))
        }
    }

    private fun getMockBanners(): List<Banner> {
        return listOf(
            Banner(
                id = "b_default_1",
                imageUrl = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?q=80&w=600&auto=format&fit=crop",
                redirectLink = "777644670",
                durationSeconds = 6,
                dateCreated = System.currentTimeMillis()
            ),
            Banner(
                id = "b_default_2",
                imageUrl = "https://images.unsplash.com/photo-1581092921461-eab62e97a780?q=80&w=600&auto=format&fit=crop",
                redirectLink = "support@portal.ye",
                durationSeconds = 4,
                dateCreated = System.currentTimeMillis() - 10000
            )
        )
    }

    // --- Secret Configuration Settings (Backdoor Storage Management) ---
    private fun saveConfigToPrefs(config: AppConfig) {
        prefs.edit().apply {
            putString("appNameAr", config.appNameAr)
            putString("appNameEn", config.appNameEn)
            putString("primaryColor", config.primaryColor)
            putString("secondaryColor", config.secondaryColor)
            putString("launcherIcon", config.launcherIcon)
            putString("promoFooter", config.promoFooter)
            putString("welcomeMessageAr", config.welcomeMessageAr)
            putString("welcomeMessageEn", config.welcomeMessageEn)
            putString("supportPhone", config.supportPhone)
            putString("supportEmail", config.supportEmail)
            putString("adminPassword", config.adminPassword)
            
            // Smart Assistant
            putBoolean("assistantEnabled", config.assistantEnabled)
            putBoolean("assistantVisible", config.assistantVisible)
            putString("assistantPosition", config.assistantPosition)
            
            // Nav Icons
            putBoolean("navHomeVisible", config.navHomeVisible)
            putString("navHomeIcon", config.navHomeIcon)
            putInt("navHomeOrder", config.navHomeOrder)
            
            putBoolean("navRegisterVisible", config.navRegisterVisible)
            putString("navRegisterIcon", config.navRegisterIcon)
            putInt("navRegisterOrder", config.navRegisterOrder)
            
            putBoolean("navAdminVisible", config.navAdminVisible)
            putString("navAdminIcon", config.navAdminIcon)
            putInt("navAdminOrder", config.navAdminOrder)
            
            putBoolean("navLangVisible", config.navLangVisible)
            putString("navLangIcon", config.navLangIcon)
            putInt("navLangOrder", config.navLangOrder)
            
            putBoolean("navRefreshVisible", config.navRefreshVisible)
            putString("navRefreshIcon", config.navRefreshIcon)
            putInt("navRefreshOrder", config.navRefreshOrder)
            apply()
        }
    }

    private fun loadConfigFromPrefs(): AppConfig {
        return AppConfig(
            appNameAr = prefs.getString("appNameAr", "بوابة الخدمات الموحدة") ?: "بوابة الخدمات الموحدة",
            appNameEn = prefs.getString("appNameEn", "Unified Services Portal") ?: "Unified Services Portal",
            primaryColor = prefs.getString("primaryColor", "#007AFF") ?: "#007AFF",
            secondaryColor = prefs.getString("secondaryColor", "#34C759") ?: "#34C759",
            launcherIcon = prefs.getString("launcherIcon", "ic_default") ?: "ic_default",
            promoFooter = prefs.getString("promoFooter", "MAW 777644670") ?: "MAW 777644670",
            welcomeMessageAr = prefs.getString("welcomeMessageAr", "سهل أعمالك ووكل مهنيك المفضل بكل سهولة وأمان ونظام مبرمج متكامل.") 
                ?: "سهل أعمالك ووكل مهنيك المفضل بكل سهولة وأمان ونظام مبرمج متكامل.",
            welcomeMessageEn = prefs.getString("welcomeMessageEn", "Simplify your tasks and book your favorite professionals with ease.") 
                ?: "Simplify your tasks and book your favorite professionals with ease.",
            supportPhone = prefs.getString("supportPhone", "777644670") ?: "777644670",
            supportEmail = prefs.getString("supportEmail", "support@portal.ye") ?: "support@portal.ye",
            adminPassword = prefs.getString("adminPassword", "maher736462") ?: "maher736462",
            
            assistantEnabled = prefs.getBoolean("assistantEnabled", true),
            assistantVisible = prefs.getBoolean("assistantVisible", true),
            assistantPosition = prefs.getString("assistantPosition", "bottom_right") ?: "bottom_right",
            
            navHomeVisible = prefs.getBoolean("navHomeVisible", true),
            navHomeIcon = prefs.getString("navHomeIcon", "Home") ?: "Home",
            navHomeOrder = prefs.getInt("navHomeOrder", 1),
            
            navRegisterVisible = prefs.getBoolean("navRegisterVisible", true),
            navRegisterIcon = prefs.getString("navRegisterIcon", "PersonAdd") ?: "PersonAdd",
            navRegisterOrder = prefs.getInt("navRegisterOrder", 2),
            
            navAdminVisible = prefs.getBoolean("navAdminVisible", true),
            navAdminIcon = prefs.getString("navAdminIcon", "Lock") ?: "Lock",
            navAdminOrder = prefs.getInt("navAdminOrder", 3),
            
            navLangVisible = prefs.getBoolean("navLangVisible", true),
            navLangIcon = prefs.getString("navLangIcon", "Language") ?: "Language",
            navLangOrder = prefs.getInt("navLangOrder", 4),
            
            navRefreshVisible = prefs.getBoolean("navRefreshVisible", true),
            navRefreshIcon = prefs.getString("navRefreshIcon", "Refresh") ?: "Refresh",
            navRefreshOrder = prefs.getInt("navRefreshOrder", 5)
        )
    }

    fun saveAppConfig(config: AppConfig) {
        globalConfigFlow.value = config
        saveConfigToPrefs(config)

        // Sync to cloud Firestore
        val db = firestore
        if (db != null) {
            db.collection("configs").document("global_config").set(config)
                .addOnFailureListener {
                    Log.e("PortalRepository", "Failed to sync AppConfig to cloud", it)
                }
        }
    }
}
