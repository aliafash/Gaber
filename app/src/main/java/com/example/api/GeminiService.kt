package com.example.api

import com.example.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response models ---
data class GeminiRequest(
    val contents: List<ContentRequest>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: SystemInstruction? = null
)

data class ContentRequest(
    val parts: List<PartRequest>
)

data class PartRequest(
    val text: String
)

data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

data class SystemInstruction(
    val parts: List<PartRequest>
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: ResponseContent? = null
)

data class ResponseContent(
    val parts: List<ResponsePart>? = null
)

data class ResponsePart(
    val text: String? = null
)

// Output data class
data class RequestAnalysisResult(
    @SerializedName("predictedCategory") val predictedCategory: String = "",
    @SerializedName("predictedPriority") val predictedPriority: String = "LOW",
    @SerializedName("reasoning") val reasoning: String = ""
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiManager {
    private val apiService = RetrofitClient.service
    private val gson = Gson()

    suspend fun analyzeNewRequest(
        title: String,
        category: String,
        description: String
    ): RequestAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "PLACEHOLDER_GEMINI_API_KEY") {
            return mockAnalyzeRequest(title, category, description)
        }

        val prompt = """
            You are an AI assistant processing citizen requests for the "بوابة الخدمات" portal.
            Analyze this issue:
            Title: $title
            User-declared Category: $category
            Description: $description

            Respond strictly with a JSON object containing these exact fields on the root level:
            1. "predictedCategory": Translate or align with a suitable department in Arabic (one of: "مياه وصرف صحي", "بلدية ونظافة", "طرق ومواصلات", "كهرباء وطاقة", "صحة وبيئة", "تعليم وتعلم", "أخرى")
            2. "predictedPriority": Assign criticality level based on public danger (one of: "LOW", "MEDIUM", "HIGH", "CRITICAL")
            3. "reasoning": A crisp Arabic explanation (1-2 sentences) of why this category and priority were assigned, focusing on safety or social urgency.

            Do not wrap in markdown ```json. Output just raw JSON.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = SystemInstruction(
                parts = listOf(PartRequest(text = "You are a backend governmental automation tool. Output strictly a flat JSON object. No Markdown wrappers like ```."))
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            var jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrEmpty()) {
                // Strip markdown blocks if returned
                jsonText = jsonText.trim()
                if (jsonText.startsWith("```")) {
                    jsonText = jsonText.substringAfter("{").substringBeforeLast("}")
                    jsonText = "{$jsonText}"
                }
                gson.fromJson(jsonText, RequestAnalysisResult::class.java)
            } else {
                mockAnalyzeRequest(title, category, description)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mockAnalyzeRequest(title, category, description)
        }
    }

    suspend fun generateSmartSummary(
        title: String,
        category: String,
        description: String,
        status: String,
        resolutionNote: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "PLACEHOLDER_GEMINI_API_KEY") {
            return generateMockSummary(category, status, resolutionNote)
        }

        val statusAr = when (status) {
            "PENDING" -> "قيد الانتظار لمراجعة الأوراق وتكليف المفتش الفني المختص بالتوجيه الميداني."
            "IN_PROGRESS" -> "قيد التنفيذ النشط بواسطة طاقم التدخل الفني لإجراء الإصلاح اللازم."
            "RESOLVED" -> "تم الحل بالكامل وإنجاز المهمة وإغلاق بلاغ المواطن بنجاح."
            else -> status
        }

        val prompt = """
            You are a customer relationship officer for "بوابة الخدمات".
            Provide a smart progress summary outlining expected timeline and procedures in Arabic.
            Title: $title
            Category: $category
            Description: $description
            Current State: $statusAr
            Resolution note: ${if (resolutionNote.isEmpty()) "لا توجد ملاحظات بعد" else resolutionNote}

            Provide 3 to 4 concise visual bullet points in clean, elegant, professional Arabic with a optimistic tone. Reassure the citizen about the safety precautions. Do not output conversational introduction or chatbot signoffs, just return the bullet points.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: generateMockSummary(category, status, resolutionNote)
        } catch (e: Exception) {
            e.printStackTrace()
            generateMockSummary(category, status, resolutionNote)
        }
    }

    // Secure fallback rule-based classification/priority predictor
    private fun mockAnalyzeRequest(title: String, category: String, description: String): RequestAnalysisResult {
        val lowerText = "$title $description $category".lowercase()
        val priority: String
        val reasoning: String
        val suggestedCategory: String

        if (lowerText.contains("حادث") || lowerText.contains("خطر") || lowerText.contains("حريق") || lowerText.contains("انفجار") || lowerText.contains("تسريب غاز") || lowerText.contains("كهرباء مكشوفة")) {
            priority = "CRITICAL"
            suggestedCategory = "كهرباء وطاقة"
            reasoning = "تم رفع درجة الأولوية إلى 'طارئة' لوجود مؤشرات خطر مباشر على الأرواح والمنشآت تطلب التدخل السوري الفوري."
        } else if (lowerText.contains("قطع") || lowerText.contains("تعطل") || lowerText.contains("تسرب") || lowerText.contains("انهيار") || lowerText.contains("فيضان")) {
            priority = "HIGH"
            suggestedCategory = if (lowerText.contains("ماء") || lowerText.contains("مياه")) "مياه وصرف صحي" else "طرق ومواصلات"
            reasoning = "تم تعيين أولوية 'عالية' نظراً للخلل التشغيلي المؤثر على حركة السير أو الموارد الأساسية مما يستلزم المعالجة السريعة."
        } else if (lowerText.contains("نظافة") || lowerText.contains("قمامة") || lowerText.contains("إنارة") || lowerText.contains("حفرة")) {
            priority = "MEDIUM"
            suggestedCategory = "بلدية ونظافة"
            reasoning = "تعتبر هذه من البلاغات الخدمية لقطاع البلدية وتم تصنيف الأولوية كـ 'متوسطة' لجدولتها ضمن خدمات الصيانة الدورية للأحياء."
        } else {
            priority = "LOW"
            suggestedCategory = "أخرى"
            reasoning = "تم تسجيل الطلب بأولوية 'منخفضة' كونه يقع ضمن المقترحات التطويرية أو الاستفسارات العامة وسيفحص بدقة في دورة العمل الدورية."
        }

        return RequestAnalysisResult(
            predictedCategory = if (category.isEmpty() || category == "أخرى") suggestedCategory else category,
            predictedPriority = priority,
            reasoning = reasoning
        )
    }

    private fun generateMockSummary(category: String, status: String, resolutionNote: String): String {
        return when (status) {
            "PENDING" -> "• **استلام الطلب**: تم توجيه البلاغ لقسم العمليات بقطاع **$category**.\n" +
                    "• **المعاينة الجغرافية**: تم حجز موعد للمعاينة الفنية لتقييم الاحتياجات الميدانية والمواد اللازمة.\n" +
                    "• **التكليف**: سيتم تخصيص المهندسين لإتمام العملية خلال 48 ساعة."
            "IN_PROGRESS" -> "• **حالة التنفيذ**: بدأ المهندسون العمل الفعلي في الموقع لمعالجة شكوى الـ **$category**.\n" +
                    "• **توفير الدعم الآلي**: تم تأجير المعدات الثقيلة المطلوبة لضمان انتهاء المشكلة نهائيًا.\n" +
                    "• **الملاحظة الفنية**: ${if (resolutionNote.isEmpty()) "بانتظار إشعار المشرف الميداني حول جاهزية الموقع." else resolutionNote}"
            "RESOLVED" -> "• **إغلاق البلاغ الفني**: تم الانتهاء بالنجاح من كامل المعالجة الميدانية.\n" +
                    "• **رقابة الجودة**: أكد مفتش السلامة إزالة جميع العقبات وضمان ملاءمتها للمقاييس الوطنية والبيئية.\n" +
                    "• **ملاحظات الحل النهائي**: ${if (resolutionNote.isEmpty()) "تم حل المشكلة وتأمين محيط العمل بالكامل." else resolutionNote}"
            else -> "• جاري فحص ملف الطلب من قبل اللجنة التقنية والتنظيمية لبوابة الخدمات لإصدار مصفوفة تقدم العمل الذكي."
        }
    }

    suspend fun chatWithAssistant(message: String, categoriesList: List<String>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "PLACEHOLDER_GEMINI_API_KEY") {
            return mockChatResponse(message, categoriesList)
        }

        val categoriesStr = categoriesList.joinToString(", ")
        val prompt = """
            You are "المساعد الذكي لبوابة الخدمات اليمنية" (Smart Assistant for the Yemeni Services Portal) developed by Tech Support.
            Help citizens or providers.
            Available service categories are: $categoriesStr
            The user said: "$message"
            
            Keep your response short (2-3 sentences), highly polite, and professional in the same language. Use bullet points or short texts if helpful.
            If they ask about available categories, list some of: $categoriesStr.
            If they want to book a professional, explain that they can browse providers on the home screen.
            If they want to register as a provider, guide them to tap the custom Registration button (👤) on the top bar.
            If they want to submit a complaint/problem, they can do so on the Home screen form.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = SystemInstruction(
                parts = listOf(PartRequest(text = "You are a helpful virtual assistant. Keep your response extremely brief, engaging and under 80 words structure."))
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: mockChatResponse(message, categoriesList)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            mockChatResponse(message, categoriesList)
        }
    }

    fun mockChatResponse(message: String, categoriesList: List<String>): String {
        val msg = message.lowercase()
        val cats = categoriesList.joinToString(", ")
        return when {
            msg.contains("مرحبا") || msg.contains("أهلاً") || msg.contains("سلام") || msg.contains("hello") || msg.contains("hi") || msg.contains("مرحباً") -> {
                "مرحباً بك في المساعد الذكي لبوابة الخدمات اليمنية! كيف يمكنني مساعدتك اليوم؟ يمكنك الاستفسار عن الخدمات المتوفرة أو كيفية تقديم شكوى أو التسجيل كمزود خدمة."
            }
            msg.contains("قسم") || msg.contains("أقسام") || msg.contains("تصنيف") || msg.contains("خدمات") || msg.contains("category") || msg.contains("categories") || msg.contains("service") -> {
                "التصنيفات المتاحة لدينا في البوابة تشمل: $cats. يمكنك اختيار أي تصنيف لتقديم طلبك أو تصفح المهنيين فيه."
            }
            msg.contains("تسجيل") || msg.contains("مزود") || msg.contains("مهني") || msg.contains("register") || msg.contains("provider") || msg.contains("join") -> {
                "لتسجيل نفسك كمقدم خدمة أو مهني معتمد في البوابة وتلقي طلبات العمل، يرجى الضغط على زر التسجيل (👤) في الشريط العلوي وملء استمارة التسجيل ببياناتك وصورة هويتك الشخصية ليتسنى للإدارة مراجعتها وتفعيل حسابك."
            }
            msg.contains("شكوى") || msg.contains("طلب") || msg.contains("بلاغ") || msg.contains("complaint") || msg.contains("request") || msg.contains("issue") -> {
                "لتقديم بلاغ أو شكوى عن خلل فني أو طلب صيانة طارئة، تفضل بالدخول للشاشة الرئيسية وتعبئة استمارة 'طلب صيانة جديد'. سيقوم نظام الذكاء الاصطناعي بتصنيف طلبك وتحديد أولويته تلقائياً وتوجيهه للمختصين."
            }
            msg.contains("رقم") || msg.contains("اتصال") || msg.contains("دعم") || msg.contains("تواصل") || msg.contains("phone") || msg.contains("contact") || msg.contains("support") -> {
                "يمكنك التواصل مع الدعم الفني للبوابة عبر الرقم المباشر للاتصال المباشر والدعم وهو: 777644670 أو البريد الإلكتروني: support@portal.ye. نحن هنا لخدمتكم على مدار الساعة."
            }
            else -> {
                "مرحباً بك! أنا المساعد الذكي المدمج أعمل (أونلاين/أوفلاين) لمساعدتك في بوابة الخدمات اليمنية. يمكنك طرح أي سؤال حول أقسام الصيانة والخدمات المتوفرة، أو تقديم البلاغات، أو آلية التسجيل كمهني معتمد في البوابة."
            }
        }
    }
}
