package com.example.ui

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun getIconFromName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "Home" -> Icons.Default.Home
        "PersonAdd" -> Icons.Default.PersonAdd
        "Lock" -> Icons.Default.Lock
        "Language" -> Icons.Default.Language
        "Refresh" -> Icons.Default.Refresh
        "Settings" -> Icons.Default.Settings
        "Star" -> Icons.Default.Star
        "Build" -> Icons.Default.Build
        "Call" -> Icons.Default.Call
        "Info" -> Icons.Default.Info
        "Face" -> Icons.Default.Face
        "Work" -> Icons.Default.Work
        "Public" -> Icons.Default.Public
        "Search" -> Icons.Default.Search
        "Favorite" -> Icons.Default.Favorite
        "ThumbUp" -> Icons.Default.ThumbUp
        "Email" -> Icons.Default.Email
        else -> Icons.Default.Home
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PortalAppContent(viewModel: PortalViewModel) {
    val context = LocalContext.current
    val config by viewModel.globalConfig.collectAsState()
    val lang by viewModel.activeLanguage.collectAsState()
    
    // Custom dynamic colors loaded from backdoor settings
    val primaryColor = viewModel.getPrimaryColor()
    val secondaryColor = viewModel.getSecondaryColor()

    // Navigation state
    var currentScreen by remember { mutableStateOf("home") } // "home", "register", "admin"
    var showChatDialog by remember { mutableStateOf(false) }
    
    // Backdoor 5-tap trigger states
    var homeTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var backdoorUnlocked by remember { mutableStateOf(false) }

    // Floating citizen complaint submission response
    val submissionState by viewModel.submissionState.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()

    // Function to handle backdoor tap counts
    val handleHomeTap: () -> Unit = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < 1500) {
            homeTapCount++
        } else {
            homeTapCount = 1
        }
        lastTapTime = currentTime
        if (homeTapCount >= 5) {
            homeTapCount = 0
            showPasswordDialog = true
        }
    }

    // Wrap entire app in custom Theme colors dynamically
    CompositionLocalProvider(
        LocalExtendedColors provides ExtendedColors(
            customPrimary = primaryColor,
            customSecondary = secondaryColor
        )
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Logo and App Name with backdoor tap trigger support
                            Row(
                                modifier = Modifier
                                    .clickable { handleHomeTap() }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = viewModel.translate(config.appNameAr, config.appNameEn),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = viewModel.translate("المنصة الذكية الموحدة", "Unified Smart Platform"),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Dynamic Bilingual Navigation Icons
                            val navList = listOf(
                                Triple("home", config.navHomeOrder, config.navHomeVisible),
                                Triple("register", config.navRegisterOrder, config.navRegisterVisible),
                                Triple("admin", config.navAdminOrder, config.navAdminVisible),
                                Triple("lang", config.navLangOrder, config.navLangVisible),
                                Triple("refresh", config.navRefreshOrder, config.navRefreshVisible)
                            ).filter { it.third }.sortedBy { it.second }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                navList.forEach { (type, _, _) ->
                                    when (type) {
                                        "home" -> {
                                            IconButton(
                                                onClick = { 
                                                    currentScreen = "home"
                                                    handleHomeTap() 
                                                },
                                                modifier = Modifier.testTag("nav_home")
                                            ) {
                                                Icon(
                                                    imageVector = getIconFromName(config.navHomeIcon),
                                                    contentDescription = "Home",
                                                    tint = if (currentScreen == "home") primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        "register" -> {
                                            IconButton(
                                                onClick = { currentScreen = "register" },
                                                modifier = Modifier.testTag("nav_register")
                                            ) {
                                                Icon(
                                                    imageVector = getIconFromName(config.navRegisterIcon),
                                                    contentDescription = "Register Provider",
                                                    tint = if (currentScreen == "register") primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        "admin" -> {
                                            IconButton(
                                                onClick = { currentScreen = "admin" },
                                                modifier = Modifier.testTag("nav_admin")
                                            ) {
                                                Icon(
                                                    imageVector = getIconFromName(config.navAdminIcon),
                                                    contentDescription = "Admin Control Panel",
                                                    tint = if (currentScreen == "admin") primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        "lang" -> {
                                            IconButton(
                                                onClick = { viewModel.toggleLanguage() },
                                                modifier = Modifier.testTag("nav_lang_switch")
                                            ) {
                                                Icon(
                                                    imageVector = getIconFromName(config.navLangIcon),
                                                    contentDescription = "Switch Language",
                                                    tint = secondaryColor
                                                )
                                            }
                                        }
                                        "refresh" -> {
                                            IconButton(
                                                onClick = {
                                                    Toast.makeText(context, viewModel.translate("تم تحديث البيانات والمزامنة الفورية مع السحابة!", "Data refreshed and synced with Cloud!"), Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("nav_refresh")
                                            ) {
                                                Icon(
                                                    imageVector = getIconFromName(config.navRefreshIcon),
                                                    contentDescription = "Refresh Portal",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Persistent Ad Promo Banner Footer (التذييل الدعائي الموحد)
                Surface(
                    color = primaryColor,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.translate(
                                "بوابة الخدمات الموحدة • الدعم والترويج: ${config.promoFooter}",
                                "Unified Services Portal • Ads & Support: ${config.promoFooter}"
                            ),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main Multi-Screen Selector Container
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState == "home") -width else width } + fadeIn() with
                                slideOutHorizontally { width -> if (targetState == "home") width else -width } + fadeOut()
                    },
                    label = "screen_animation"
                ) { target ->
                    when (target) {
                        "home" -> HomeScreen(viewModel = viewModel)
                        "register" -> RegisterProviderScreen(viewModel = viewModel, onSubmitted = { currentScreen = "home" })
                        "admin" -> AdminPanelScreen(viewModel = viewModel, unlockedBackdoor = backdoorUnlocked)
                    }
                }

                // AI Processing Overlay Animation for citizen complaint forms
                if (submissionState is SubmissionState.ProcessingAI || submissionState is SubmissionState.Saving) {
                    Dialog(
                        onDismissRequest = {},
                        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = primaryColor,
                                    modifier = Modifier.size(50.dp)
                                )
                                Text(
                                    text = viewModel.translate("جاري تشغيل تصنيف الذكاء الاصطناعي...", "Running AI Priority Classifier..."),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = viewModel.translate(
                                        "يقوم محرك Gemini 3.5 Flash بتحليل العنوان والوصف لتقدير الأولوية وتصنيف البلاغ وتحديد القسم المختص تلقائياً.",
                                        "Gemini 3.5 Flash is analyzing your report title & details to automatically estimate risk priority and route to correct department."
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Citizens' complaint Details & summarizer
                selectedRequest?.let { request ->
                    RequestDetailsDialog(
                        request = request,
                        viewModel = viewModel,
                        onDismiss = { viewModel.selectRequest(null) }
                    )
                }

                // Dialog 1: Backdoor password challenge
                if (showPasswordDialog) {
                    var passInput by remember { mutableStateOf("") }
                    Dialog(onDismissRequest = { showPasswordDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = viewModel.translate("إعدادات النظام الخلفية", "System Backdoor Settings"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                OutlinedTextField(
                                    value = passInput,
                                    onValueChange = { passInput = it },
                                    label = { Text(viewModel.translate("رمز التحقق السري لمالك التطبيق", "Owner security passcode")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showPasswordDialog = false }) {
                                        Text(viewModel.translate("إلغاء", "Cancel"))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                        onClick = {
                                            if (passInput == "maher--736462") {
                                                backdoorUnlocked = true
                                                showPasswordDialog = false
                                                currentScreen = "admin"
                                                Toast.makeText(context, viewModel.translate("تم فتح الإعدادات السرية بنجاح!", "Secret Admin Configuration Unlocked!"), Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, viewModel.translate("رمز التحقق خاطئ!", "Invalid passcode!"), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text(viewModel.translate("فتح", "Unlock"))
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating circular smart assistant FAB
                if (config.assistantEnabled && config.assistantVisible) {
                    val (alignment, padding) = when (config.assistantPosition) {
                        "bottom_left" -> Alignment.BottomStart to PaddingValues(start = 24.dp, bottom = 90.dp)
                        "top_right" -> Alignment.TopEnd to PaddingValues(end = 24.dp, top = 90.dp)
                        "top_left" -> Alignment.TopStart to PaddingValues(start = 24.dp, top = 90.dp)
                        else -> Alignment.BottomEnd to PaddingValues(end = 24.dp, bottom = 90.dp)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = alignment
                    ) {
                        FloatingActionButton(
                            onClick = { showChatDialog = true },
                            containerColor = primaryColor,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("floating_assistant_fab"),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Smart Assistant",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Dialog for Smart Assistant Chatbot Companion
                if (showChatDialog && config.assistantEnabled && config.assistantVisible) {
                    val chatMessages by viewModel.chatMessages.collectAsState()
                    val isChatLoading by viewModel.isChatLoading.collectAsState()
                    var chatText by remember { mutableStateOf("") }
                    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()

                    LaunchedEffect(chatMessages.size) {
                        if (chatMessages.isNotEmpty()) {
                            scrollState.animateScrollToItem(chatMessages.size - 1)
                        }
                    }

                    Dialog(
                        onDismissRequest = { showChatDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .fillMaxHeight(0.75f)
                                .padding(8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Header with active indicator and gradients
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(primaryColor, secondaryColor)
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF34C759))
                                            )
                                            Column {
                                                Text(
                                                    text = viewModel.translate("المساعد التقني الذكي", "Portal AI Assistant"),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = viewModel.translate("يعمل أوفلاين/أونلاين لدعم الصيانة والشكاوى ⚡", "Works Offline/Online for utilities and support ⚡"),
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.clearChatHistory() }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.White)
                                            }
                                            IconButton(onClick = { showChatDialog = false }) {
                                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                            }
                                        }
                                    }
                                }

                                // Scrollable Chats History
                                LazyColumn(
                                    state = scrollState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(chatMessages) { (text, isUser) ->
                                        val bubbleBg = if (isUser) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                        val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                        val bubbleAlignment = if (isUser) Alignment.End else Alignment.Start
                                        val shape = if (isUser) {
                                            RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
                                        } else {
                                            RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = bubbleAlignment
                                        ) {
                                            Surface(
                                                color = bubbleBg,
                                                shape = shape,
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Text(
                                                    text = text,
                                                    color = textColor,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    lineHeight = 18.sp
                                                )
                                            }
                                            Text(
                                                text = if (isUser) viewModel.translate("أنت", "You") else viewModel.translate("المساعد الذكي", "Portal AI"),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                            )
                                        }
                                    }

                                    if (isChatLoading) {
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = primaryColor
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = viewModel.translate("جاري توليد الرد الذكي...", "Thinking of a smart response..."),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Suggestions
                                val suggestions = listOf(
                                    viewModel.translate("أقسام الصيانة", "Browse Categories"),
                                    viewModel.translate("كيف أسجل كمهني؟", "How to Register?"),
                                    viewModel.translate("تقديم شكوى جديدة", "Submit Complaint"),
                                    viewModel.translate("رقم الدعم للاتصال", "Support Phone")
                                )
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp, start = 12.dp, end = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(suggestions) { suggestion ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.clickable {
                                                viewModel.sendChatMessage(suggestion)
                                            }
                                        ) {
                                            Text(
                                                text = suggestion,
                                                fontSize = 11.sp,
                                                color = primaryColor,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Interactive Text entry bar
                                Surface(
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = chatText,
                                            onValueChange = { chatText = it },
                                            placeholder = { Text(viewModel.translate("اكتب سؤالك هنا...", "Write your question..."), fontSize = 13.sp) },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 3,
                                            shape = RoundedCornerShape(20.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = primaryColor,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        )
                                        IconButton(
                                            onClick = {
                                                if (chatText.trim().isNotEmpty()) {
                                                    viewModel.sendChatMessage(chatText)
                                                    chatText = ""
                                                }
                                            },
                                            enabled = chatText.trim().isNotEmpty() && !isChatLoading,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(if (chatText.trim().isNotEmpty() && !isChatLoading) primaryColor else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Send",
                                                tint = if (chatText.trim().isNotEmpty() && !isChatLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Home Screen: Rotating Banners, Categories Grid, recommended & Pinned list, registration complaint ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: PortalViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val config by viewModel.globalConfig.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeProviders by viewModel.serviceProviders.collectAsState()
    val mainListCategories = categories.filter { it.isMain }

    // Dropdown/Filter settings states
    var selectedMainCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSubCategory by remember { mutableStateOf<Category?>(null) }
    var searchDistrictInput by remember { mutableStateOf("") }
    var searchProviderNameInput by remember { mutableStateOf("") }

    // Static unsplash photo database helper for professional illustration
    val getProfessionPhoto: (String) -> String = { id ->
        when(id) {
            "sub1" -> "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?q=80&w=300&auto=format&fit=crop" // electrician
            "sub2" -> "https://images.unsplash.com/photo-1581092921461-eab62e97a780?q=80&w=300&auto=format&fit=crop" // plumbing
            "sub3" -> "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?q=80&w=300&auto=format&fit=crop" // doctor
            "sub4" -> "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?q=80&w=300&auto=format&fit=crop" // tutor
            "sub5" -> "https://images.unsplash.com/photo-1600585154526-990dced4db0d?q=80&w=300&auto=format&fit=crop" // mover
            else -> "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?q=80&w=300&auto=format&fit=crop" // corporate
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        
        // 1. Dynamic Rotating Ad Header Banner (لافتات الإعلانات الدوارة التلقائية)
        if (banners.isNotEmpty()) {
            item {
                var activeIndex by remember { mutableStateOf(0) }
                val currentAd = banners[activeIndex % banners.size]
                
                LaunchedEffect(activeIndex, currentAd.durationSeconds) {
                    delay((currentAd.durationSeconds * 1000L).coerceAtLeast(1000L))
                    activeIndex++
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clickable {
                            if (currentAd.redirectLink.isNotEmpty()) {
                                // redirect mock action / phone / website
                                Log.d("BannerRedirect", "Redirecting to ${currentAd.redirectLink}")
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = currentAd.imageUrl,
                            contentDescription = "Dynamic Ad Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient Overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (currentAd.redirectLink.isNotEmpty()) "${viewModel.translate("انقر للتفاصيل والاستفسار", "Click for details")}: ${currentAd.redirectLink}" else "",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Dynamic Greeting / Welcome Banner (رسالة الترحيب المخصصة من الإعدادات)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = viewModel.getPrimaryColor().copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, viewModel.getPrimaryColor().copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = viewModel.getPrimaryColor()
                    )
                    Text(
                        text = viewModel.translate(config.welcomeMessageAr, config.welcomeMessageEn),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 3. Recommended Featured Service Providers (المهنيين الموصى بهم في الأعلى)
        val recommendedList = activeProviders.filter { it.isRecommended }
        if (recommendedList.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = viewModel.translate("المهنيين الموصى بهم وممثلي الثقة ⭐", "Recommended & Trusted Professionals ⭐"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = viewModel.getPrimaryColor()
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recommendedList) { provider ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .width(180.dp)
                                    .clickable { /* Detail actions or reviews */ },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box {
                                        AsyncImage(
                                            model = provider.profilePic.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop" },
                                            contentDescription = provider.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFC107))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text(provider.rating.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(provider.district, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
                                    Text(
                                        text = provider.phone,
                                        color = viewModel.getPrimaryColor(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("recommended_call_${provider.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Main Category Selection Slider
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = viewModel.translate("تصنيفات الخدمات والأعمال", "Service Fields & Categories"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMainCategory == null,
                            onClick = { 
                                selectedMainCategory = null
                                selectedSubCategory = null
                            },
                            label = { Text(viewModel.translate("الجميع", "All")) }
                        )
                    }
                    items(mainListCategories) { cat ->
                        FilterChip(
                            selected = selectedMainCategory?.id == cat.id,
                            onClick = { 
                                selectedMainCategory = cat
                                selectedSubCategory = null
                            },
                            label = { Text(viewModel.translate(cat.nameAr, cat.nameEn)) }
                        )
                    }
                }
            }
        }

        // Subcategories slider based on main category selected
        selectedMainCategory?.let { mainCat ->
            val subCats = categories.filter { !it.isMain && it.parentId == mainCat.id }
            if (subCats.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSubCategory == null,
                                onClick = { selectedSubCategory = null },
                                label = { Text(viewModel.translate("جميع فروع ${mainCat.nameAr}", "All under ${mainCat.nameEn}")) }
                            )
                        }
                        items(subCats) { sub ->
                            FilterChip(
                                selected = selectedSubCategory?.id == sub.id,
                                onClick = { selectedSubCategory = sub },
                                label = { Text(viewModel.translate(sub.nameAr, sub.nameEn)) }
                            )
                        }
                    }
                }
            }
        }

        // Search options filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchProviderNameInput,
                    onValueChange = { searchProviderNameInput = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text(viewModel.translate("ابحث عن مهني بالاسم...", "Search professional name...")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = searchDistrictInput,
                    onValueChange = { searchDistrictInput = it },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    placeholder = { Text(viewModel.translate("المنطقة/الحي...", "District/Area...")) },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // 5. Grid list of matched providers (with Pinned/Starred items appearing first!)
        val filteredProviders = activeProviders.filter { prov ->
            // Category matches
            val catMatch = when {
                selectedSubCategory != null -> prov.categoryId == selectedSubCategory!!.id
                selectedMainCategory != null -> {
                    val matchingSubs = categories.filter { it.parentId == selectedMainCategory!!.id }.map { it.id }
                    prov.categoryId == selectedMainCategory!!.id || matchingSubs.contains(prov.categoryId)
                }
                else -> true
            }
            // Name matches
            val nameMatch = searchProviderNameInput.isEmpty() || prov.name.contains(searchProviderNameInput, ignoreCase = true)
            // District matches
            val distMatch = searchDistrictInput.isEmpty() || prov.district.contains(searchDistrictInput, ignoreCase = true)

            catMatch && nameMatch && distMatch
        }.sortedWith(compareByDescending { it.isPinned }) // PINNED providers go to the very top of their categories

        if (filteredProviders.isNotEmpty()) {
            item {
                Text(
                    text = viewModel.translate("المهنيين ومقدمي الخدمات النشطين (${filteredProviders.size})", "Active Professionals & Providers (${filteredProviders.size})"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(filteredProviders) { provider ->
                var showReviewPopup by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (provider.isPinned) BorderStroke(1.5.dp, Color(0xFFFF9800)) else null // orange highlight border for pinned
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar
                        Box {
                            AsyncImage(
                                model = provider.profilePic.ifEmpty { "https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=200&auto=format&fit=crop" },
                                contentDescription = provider.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (provider.isPinned) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFF9800))
                                        .padding(2.dp)
                                ) {
                                    Icon(Icons.Default.PushPin, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }

                        // Text details
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (provider.isRecommended) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, "Verified", tint = viewModel.getSecondaryColor(), modifier = Modifier.size(14.dp))
                                }
                            }
                            val catObj = categories.find { it.id == provider.categoryId }
                            val catName = catObj?.let { viewModel.translate(it.nameAr, it.nameEn) } ?: provider.categoryId
                            Text(
                                text = "${viewModel.translate("قطاع التخصص", "Field")}: $catName",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${viewModel.translate("الموقع والمقر الرئيسي", "Office & Address")}: ${provider.district} - ${provider.address}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("${provider.rating} (${provider.ratingCount} ${viewModel.translate("تقييمات", "reviews")})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Multi-actions
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { showReviewPopup = true },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = viewModel.getSecondaryColor())
                            ) {
                                Text(viewModel.translate("قيم", "Rate"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { /* Direct WhatsApp dial action mock simulation */ },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = viewModel.getPrimaryColor())
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(10.dp))
                                    Text(provider.phone, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Dialog rating / review popup
                if (showReviewPopup) {
                    var starValue by remember { mutableStateOf(5f) }
                    var commentInput by remember { mutableStateOf("") }
                    var ratingNameInput by remember { mutableStateOf("") }
                    Dialog(onDismissRequest = { showReviewPopup = false }) {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("${viewModel.translate("تقييم المهني/الشركة", "Rate provider")} • ${provider.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                Text("${viewModel.translate("عدد النجوم", "Stars count")}: ${starValue.toInt()}⭐", fontSize = 12.sp)
                                Slider(
                                    value = starValue,
                                    onValueChange = { starValue = it },
                                    valueRange = 1f..5f,
                                    steps = 3
                                )
                                OutlinedTextField(
                                    value = ratingNameInput,
                                    onValueChange = { ratingNameInput = it },
                                    label = { Text(viewModel.translate("اسمك كعميل", "Your name as client")) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = commentInput,
                                    onValueChange = { commentInput = it },
                                    label = { Text(viewModel.translate("اكتب تقييمك بالتفصيل...", "Write comment detail...")) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showReviewPopup = false }) { Text(viewModel.translate("إلغاء", "Cancel")) }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = viewModel.getSecondaryColor()),
                                        onClick = {
                                            if (ratingNameInput.trim().isEmpty() || commentInput.trim().isEmpty()) {
                                                Toast.makeText(context, viewModel.translate("الرجاء تعبئة الاسم وكتابة الملاحظة.", "Please enter name and write comment."), Toast.LENGTH_SHORT).show()
                                            } else {
                                                coroutineScope.launch {
                                                    val review = Review(
                                                        providerId = provider.id,
                                                        reviewerName = ratingNameInput,
                                                        rating = starValue,
                                                        comment = commentInput,
                                                        dateCreated = System.currentTimeMillis()
                                                    )
                                                    viewModel.submitManualProvider() // trigger local sync flows
                                                    // Add review
                                                    val repo = PortalRepository(context)
                                                    repo.submitReview(review)
                                                    Toast.makeText(context, viewModel.translate("شكرًا لتقييمك الذكي!", "Thanks for your smart review rating!"), Toast.LENGTH_SHORT).show()
                                                    showReviewPopup = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text(viewModel.translate("إرسال التقييم", "Submit review"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.translate("لا توجد نتائج بحث مطابقة للمواصفات أو لا يوجد مهنيين مسجلين بعد.", "No service providers found matching your parameters."),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 6. Citizen Service requests / complaints section (original functionality)
        item {
            CitizenSubmitFormSection(viewModel = viewModel)
        }

        item {
            RequestsDashboardSection(viewModel = viewModel)
        }
    }
}

// --- Register Service Provider Screen (👤 form) ---
@Composable
fun RegisterProviderScreen(viewModel: PortalViewModel, onSubmitted: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    // Inputs States
    val name by viewModel.regFullNameState.collectAsState()
    val phone by viewModel.regPhoneState.collectAsState()
    val selectedCategory by viewModel.regCategoryIdState.collectAsState()
    val address by viewModel.regAddressState.collectAsState()
    val district by viewModel.regDistrictState.collectAsState()
    val gps by viewModel.regGpsState.collectAsState()
    val profilePic by viewModel.regProfilePicState.collectAsState()
    val idCard by viewModel.regIdCardState.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = viewModel.translate("استمارة تسجيل مقدم خدمة جديد", "Professional Registration Request Form"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = viewModel.getPrimaryColor()
        )
        Text(
            text = viewModel.translate(
                "سجل مهنتك أو شركتك الخدمية الآن لتصل لآلاف العملاء في منطقتك الجغرافية بكل سهولة. سيتم مراجعة الطلب بواسطة لجان التدقيق الإدارية فوراً.",
                "List your business or professional craft today. Reach thousands of local clients. Requests are approved by regional administrators instantly."
            ),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.regFullNameState.value = it },
                    label = { Text(viewModel.translate("الاسم الثلاثي الكامل المعتمد للهوية", "Approved Full Name (Triple)")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.regPhoneState.value = it },
                    label = { Text(viewModel.translate("رقم الهاتف النشط / أو الواتساب", "Active Phone Number / WhatsApp")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Dropdown Category Select
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategory }?.let { viewModel.translate(it.nameAr, it.nameEn) } ?: "",
                        onValueChange = {},
                        label = { Text(viewModel.translate("قطاع التخصص الفني والمهني", "Field of Craft Specialization")) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        categories.filter { !it.isMain }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(viewModel.translate(cat.nameAr, cat.nameEn)) },
                                onClick = {
                                    viewModel.regCategoryIdState.value = cat.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { viewModel.regAddressState.value = it },
                    label = { Text(viewModel.translate("عنوان المكتب أو المقر الرسمي الفعلي", "Physical Office or Center Address")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // District / Area
                OutlinedTextField(
                    value = district,
                    onValueChange = { viewModel.regDistrictState.value = it },
                    label = { Text(viewModel.translate("المديرية والحي الجغرافي والمنطقة", "Residential Area / District / City")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // GPS Location Mock
                OutlinedTextField(
                    value = gps,
                    onValueChange = { viewModel.regGpsState.value = it },
                    label = { Text(viewModel.translate("إحداثيات الموقع على الخريطة GPS (اختياري)", "Map coordinates location GPS (optional)")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Profile URL and ID Card Mock select
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Quick auto seed profile photo link for prototyping
                            viewModel.regProfilePicState.value = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=250&auto=format&fit=crop"
                            Toast.makeText(context, viewModel.translate("تم رفع صورتك الشخصية بنجاح!", "Profile picture uploaded!"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = viewModel.getSecondaryColor())
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (profilePic.isEmpty()) viewModel.translate("ارفع صورتك", "Profile photo") else "✓ ${viewModel.translate("تم الرفع", "Uploaded")}", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.regIdCardState.value = "https://images.unsplash.com/photo-1554774853-aae0a22c8aa4?q=80&w=250&auto=format&fit=crop"
                            Toast.makeText(context, viewModel.translate("تم رفع صورة الهوية بنجاح!", "ID card uploaded!"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Icon(Icons.Default.CardMembership, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (idCard.isEmpty()) viewModel.translate("صورة الهوية", "ID Card photo") else "✓ ${viewModel.translate("تم الرفع", "Uploaded")}", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val status = viewModel.submitProviderRegistration()
                        if (status == "SUCCESS") {
                            Toast.makeText(context, viewModel.translate("تم إرسال طلب انضمامك بنجاح! جاري المراجعة الإدارية.", "Your list request has been submitted for admin verification."), Toast.LENGTH_LONG).show()
                            onSubmitted()
                        } else {
                            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_provider_registration_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = viewModel.getPrimaryColor())
                ) {
                    Text(viewModel.translate("إرسال طلب التسجيل للاعتماد", "Submit Application for Accreditation"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// --- Login / Admin Control Panel Screen ---
@Composable
fun AdminPanelScreen(viewModel: PortalViewModel, unlockedBackdoor: Boolean) {
    var adminLoggedIn by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val config by viewModel.globalConfig.collectAsState()

    if (!adminLoggedIn && !unlockedBackdoor) {
        // Simple Login Form
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin",
                tint = viewModel.getPrimaryColor(),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                viewModel.translate("بوابة تسجيل الدخول للمدراء وصيانة الموقع", "Administrative/Coordinator Login Portal"),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = "WAM2026",
                onValueChange = {},
                readOnly = true,
                label = { Text(viewModel.translate("اسم مستخدم الإدارة", "Coordinator Username (Fixed)")) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text(viewModel.translate("كلمة المرور المشفرة", "Administrative Password")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = viewModel.getPrimaryColor()),
                onClick = {
                    if (passwordInput == config.adminPassword) {
                        adminLoggedIn = true
                    } else {
                        Toast.makeText(context, viewModel.translate("خطأ في بيانات التسلل!", "Invalid administration credentials!"), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_login_submit")
            ) {
                Text(viewModel.translate("تسجيل الدخول كمنسق", "Log In as Coordinator"), fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // Show Admin tabbed system
        AdminTabsDashboard(viewModel = viewModel, isDirectBackdoorUnlocked = unlockedBackdoor)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminTabsDashboard(viewModel: PortalViewModel, isDirectBackdoorUnlocked: Boolean) {
    val context = LocalContext.current
    val config by viewModel.globalConfig.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    
    val tabs = if (isDirectBackdoorUnlocked) {
        listOf(
            viewModel.translate("المهن والفئات", "Categories"),
            viewModel.translate("تسجيل مهني", "Add Provider"),
            viewModel.translate("الطلبات المعلقة", "Requests Queue"),
            viewModel.translate("الاعلان واللافتات", "Ads & Banners"),
            viewModel.translate("شكاوى المواطنين", "Citizen Complaints"),
            viewModel.translate("الإعدادات السرية", "Secret Parameters") // ONLY visible in 5-tap backdoor unlock!
        )
    } else {
        listOf(
            viewModel.translate("المهن والفئات", "Categories"),
            viewModel.translate("تسجيل مهني", "Add Provider"),
            viewModel.translate("الطلبات المعلقة", "Requests Queue"),
            viewModel.translate("الاعلان واللافتات", "Ads & Banners"),
            viewModel.translate("شكاوى المواطنين", "Citizen Complaints")
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = activeTab == i,
                    onClick = { activeTab = i },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(14.dp)
        ) {
            when (activeTab) {
                0 -> ManageCategoriesTab(viewModel)
                1 -> AddProviderDirectlyTab(viewModel)
                2 -> ReviewPendingRequestsTab(viewModel)
                3 -> ManageActiveBannersTab(viewModel)
                4 -> ManageCitizenComplaintsTab(viewModel)
                5 -> if (isDirectBackdoorUnlocked) BackdoorSettingsTab(viewModel)
            }
        }
    }
}

// Admin Tab 1: Category Manager
@Composable
fun ManageCategoriesTab(viewModel: PortalViewModel) {
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var orderValue by remember { mutableStateOf("0") }
    var isMainSelected by remember { mutableStateOf(true) }
    var parentIdSelected by remember { mutableStateOf<String?>(null) }
    var expandedDrop by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(viewModel.translate("إضافة وتعديل الأقسام وتصنيفات المهن", "Manage Trade Fields & Categories"), fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text(viewModel.translate("التسمية العربية", "Arabic Name")) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(viewModel.translate("التسمية الإنجليزية", "English Name")) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(viewModel.translate("قسم رئيسي؟", "Is Main Field?"))
            Switch(checked = isMainSelected, onCheckedChange = { isMainSelected = it })

            if (!isMainSelected) {
                Box {
                    Button(onClick = { expandedDrop = true }) {
                        Text(categories.find { it.id == parentIdSelected }?.nameAr ?: viewModel.translate("اختر القسم الأب", "Select Parent"))
                    }
                    DropdownMenu(expanded = expandedDrop, onDismissRequest = { expandedDrop = false }) {
                        categories.filter { it.isMain }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nameAr) },
                                onClick = {
                                    parentIdSelected = cat.id
                                    expandedDrop = false
                                }
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = orderValue,
            onValueChange = { orderValue = it },
            label = { Text(viewModel.translate("ترتيب الوزن والتصنيف (رقم)", "Order (numeric weight)")) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = viewModel.getPrimaryColor()),
            onClick = {
                if (nameAr.isEmpty() || nameEn.isEmpty()) {
                    Toast.makeText(context, viewModel.translate("الرجاء إدخال الاسم بالجهتين.", "Please write names in both languages."), Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.insertCategoryDirect(
                        nameAr = nameAr,
                        nameEn = nameEn,
                        parentId = if (isMainSelected) null else parentIdSelected,
                        isMain = isMainSelected,
                        order = orderValue.toIntOrNull() ?: 0
                    )
                    Toast.makeText(context, viewModel.translate("تمت إضافة التصنيف بنجاح!", "Category added successfully!"), Toast.LENGTH_SHORT).show()
                    nameAr = ""
                    nameEn = ""
                    orderValue = "0"
                }
            }
        ) {
            Text(viewModel.translate("حفظ تصنيف المهنة", "Save Category Field"))
        }

        Divider()

        Text(viewModel.translate("أقسام الموقع الحالية", "Existing Fields in Directory"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { cat ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${viewModel.translate(cat.nameAr, cat.nameEn)} ${if (cat.isMain) " (رئيسي)" else " (فرعي لأب: ${cat.parentId})"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { viewModel.deleteCategoryDirect(cat) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// Admin Tab 2: Add Service Provider directly (Direct Quick Enrollment)
@Composable
fun AddProviderDirectlyTab(viewModel: PortalViewModel) {
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    val name by viewModel.adminManualNameState.collectAsState()
    val phone by viewModel.adminManualPhoneState.collectAsState()
    val categoryId by viewModel.adminManualCategoryIdState.collectAsState()
    val address by viewModel.adminManualAddressState.collectAsState()
    val profilePic by viewModel.adminManualProfilePicState.collectAsState()

    var dropExpand by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(viewModel.translate("إضافة مهني أو شركة مباشرة للتطبيق (من دون موافقة معلقة)", "Fast Service Provider Direct Enrollment"), fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.adminManualNameState.value = it },
            label = { Text(viewModel.translate("اسم المهني/الشركة", "Professional / Entity Name")) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { viewModel.adminManualPhoneState.value = it },
            label = { Text(viewModel.translate("رقم الهاتف أو جهة الاتصال", "Telephone / WhatsApp Mobile")) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = categories.find { it.id == categoryId }?.let { viewModel.translate(it.nameAr, it.nameEn) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(viewModel.translate("حدد مجال الصنع والعمل", "Select Craft / Trade Category")) },
                trailingIcon = { IconButton(onClick = { dropExpand = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(expanded = dropExpand, onDismissRequest = { dropExpand = false }) {
                categories.filter { !it.isMain }.forEach { sub ->
                    DropdownMenuItem(text = { Text(viewModel.translate(sub.nameAr, sub.nameEn)) }, onClick = {
                        viewModel.adminManualCategoryIdState.value = sub.id
                        dropExpand = false
                    })
                }
            }
        }
        OutlinedTextField(
            value = address,
            onValueChange = { viewModel.adminManualAddressState.value = it },
            label = { Text(viewModel.translate("الموقع أو الحي الجغرافي الدقيق", "Physical District & Accurate Address")) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = profilePic,
            onValueChange = { viewModel.adminManualProfilePicState.value = it },
            label = { Text(viewModel.translate("عنوان رابط الصورة الشخصية (أنسبلاش أو فارغ للرمز الافتراضي)", "Profile picture internet URL link")) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = viewModel.getPrimaryColor()),
            onClick = {
                val status = viewModel.submitManualProvider()
                if (status == "SUCCESS") {
                    Toast.makeText(context, viewModel.translate("تم تسجيل واعتماد المهني مباشرة في قاعدة البيانات النشطة!", "Accredited provider saved straight to live storage!"), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text(viewModel.translate("اعتماد وتنزيل للتطبيق فورا", "Submit and ACCREDIT immediately"))
        }
    }
}

// Admin Tab 3: Pending Registration Queue
@Composable
fun ReviewPendingRequestsTab(viewModel: PortalViewModel) {
    val pendingList by viewModel.pendingProviders.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    val unresolvedList = pendingList.filter { it.status == "PENDING" }

    if (unresolvedList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(viewModel.translate("لا توجد طلبات انضمام معلقة للمراجعة حالياً.", "No pending professional applications to verify."), color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(unresolvedList) { item ->
                var rejectReasonInput by remember { mutableStateOf("") }
                var showRejectBlock by remember { mutableStateOf(false) }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "حالة: المعاينة الإدارية",
                                color = viewModel.getPrimaryColor(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${viewModel.translate("المهنة المطلوبة", "Requested Field")}: ${
                                categories.find { it.id == item.categoryId }?.let { viewModel.translate(it.nameAr, it.nameEn) } ?: item.categoryId
                            }",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "هاتف: ${item.phone} | منطقة: ${item.district} - ${item.address}",
                            fontSize = 11.sp
                        )

                        // Attach preview images safely
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Profile picture
                            AsyncImage(
                                model = item.profilePic,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)
                            )
                            // ID card
                            AsyncImage(
                                model = item.idCardPic,
                                contentDescription = "ID Card Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)
                            )
                        }

                        if (showRejectBlock) {
                            OutlinedTextField(
                                value = rejectReasonInput,
                                onValueChange = { rejectReasonInput = it },
                                label = { Text(viewModel.translate("سبب الرفض الإداري التوضيحي", "Describe reject audit reason")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showRejectBlock = false }) { Text(viewModel.translate("تراجع", "Back")) }
                                TextButton(onClick = {
                                    if (rejectReasonInput.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء توضيح سبب الرفض.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.rejectRegistration(item.id, rejectReasonInput)
                                        Toast.makeText(context, "تم رفض الطلب وإعلام المهني بالسبب.", Toast.LENGTH_SHORT).show()
                                    }
                                }) { Text(viewModel.translate("تأكيد الرفض", "Confirm Reject"), color = Color.Red) }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    onClick = { showRejectBlock = true },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(viewModel.translate("رفض الطلب", "Reject"), fontSize = 11.sp)
                                }
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = viewModel.getSecondaryColor()),
                                    onClick = {
                                        viewModel.approveRegistration(item)
                                        Toast.makeText(context, viewModel.translate("تم تفعيل حساب المهني ونشره للجمهور بنجاح!", "Provider account approved & live!"), Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(viewModel.translate("قبول واعتماد", "Accept Application"), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Admin Tab 4: Ad Banner Management
@Composable
fun ManageActiveBannersTab(viewModel: PortalViewModel) {
    val banners by viewModel.banners.collectAsState()
    val context = LocalContext.current

    val imageLink by viewModel.bannerImageUrlState.collectAsState()
    val redirectLink by viewModel.bannerRedirectLinkState.collectAsState()
    val durationSeconds by viewModel.bannerSecondsState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(viewModel.translate("إدارة لافتات الإعلانات الدوارة في أعلى التطبيق", "Top Slider Rotating Banners Coordinator"), fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = imageLink,
            onValueChange = { viewModel.bannerImageUrlState.value = it },
            label = { Text(viewModel.translate("عنوان رابط صورة الإعلان (Unsplash URL)", "Advertisement Image Link URL")) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = redirectLink,
                onValueChange = { viewModel.bannerRedirectLinkState.value = it },
                label = { Text(viewModel.translate("رابط التحويل/تلفون (اختياري)", "Redirection link or phone (optional)")) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = durationSeconds,
                onValueChange = { viewModel.bannerSecondsState.value = it },
                label = { Text(viewModel.translate("وقت العرض بالثانية", "Slideshow rotation seconds")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.8f)
            )
        }

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = viewModel.getPrimaryColor()),
            onClick = {
                val status = viewModel.submitNewBanner()
                if (status == "SUCCESS") {
                    Toast.makeText(context, viewModel.translate("تم إدراج الإعلان ونشره فورا!", "Advertisement Banner activated!"), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text(viewModel.translate("إطلاق وتنشيط الإعلان", "Deploy Live Banner Advertisement"))
        }

        Divider()

        Text(viewModel.translate("اللافتات الإعلانية الفعالة حالياً", "Active Banners Queue"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(banners) { banner ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = banner.imageUrl,
                            contentDescription = "Ad banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(text = "ثواني: ${banner.durationSeconds}s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "رابط التوجيه: ${banner.redirectLink.ifEmpty { "لا يوجد" }}", fontSize = 10.sp, maxLines = 1)
                        }
                        IconButton(onClick = { viewModel.deleteBannerDirect(banner) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// Admin Tab 5: Citizen Complaints/Requests Manager
@Composable
fun ManageCitizenComplaintsTab(viewModel: PortalViewModel) {
    RequestsDashboardSection(viewModel = viewModel)
}

// Admin Tab 6: Secret Backdoor Customization Panel
@Composable
fun BackdoorSettingsTab(viewModel: PortalViewModel) {
    val config by viewModel.globalConfig.collectAsState()
    val context = LocalContext.current
    val primaryColor = viewModel.getPrimaryColor()

    var nameAr by remember { mutableStateOf(config.appNameAr) }
    var nameEn by remember { mutableStateOf(config.appNameEn) }
    var pColor by remember { mutableStateOf(config.primaryColor) }
    var sColor by remember { mutableStateOf(config.secondaryColor) }
    var footer by remember { mutableStateOf(config.promoFooter) }
    var welcomeAr by remember { mutableStateOf(config.welcomeMessageAr) }
    var welcomeEn by remember { mutableStateOf(config.welcomeMessageEn) }
    var phone by remember { mutableStateOf(config.supportPhone) }
    var email by remember { mutableStateOf(config.supportEmail) }
    var passAdmin by remember { mutableStateOf(config.adminPassword) }

    // Smart Assistant States
    var assistantEnabled by remember { mutableStateOf(config.assistantEnabled) }
    var assistantVisible by remember { mutableStateOf(config.assistantVisible) }
    var assistantPosition by remember { mutableStateOf(config.assistantPosition) }

    // Nav icons states
    var homeVisible by remember { mutableStateOf(config.navHomeVisible) }
    var homeIcon by remember { mutableStateOf(config.navHomeIcon) }
    var homeOrder by remember { mutableStateOf(config.navHomeOrder.toString()) }

    var registerVisible by remember { mutableStateOf(config.navRegisterVisible) }
    var registerIcon by remember { mutableStateOf(config.navRegisterIcon) }
    var registerOrder by remember { mutableStateOf(config.navRegisterOrder.toString()) }

    var adminVisible by remember { mutableStateOf(config.navAdminVisible) }
    var adminIcon by remember { mutableStateOf(config.navAdminIcon) }
    var adminOrder by remember { mutableStateOf(config.navAdminOrder.toString()) }

    var langVisible by remember { mutableStateOf(config.navLangVisible) }
    var langIcon by remember { mutableStateOf(config.navLangIcon) }
    var langOrder by remember { mutableStateOf(config.navLangOrder.toString()) }

    var refreshVisible by remember { mutableStateOf(config.navRefreshVisible) }
    var refreshIcon by remember { mutableStateOf(config.navRefreshIcon) }
    var refreshOrder by remember { mutableStateOf(config.navRefreshOrder.toString()) }

    val iconOptions = listOf(
        "Home", "PersonAdd", "Lock", "Language", "Refresh", 
        "Settings", "Star", "Build", "Call", "Info", 
        "Face", "Work", "Public", "Search", "Favorite", "ThumbUp"
    )

    val positionOptions = listOf(
        "bottom_right" to viewModel.translate("أسفل اليمين", "Bottom Right"),
        "bottom_left" to viewModel.translate("أسفل اليسار", "Bottom Left"),
        "top_right" to viewModel.translate("أعلى اليمين", "Top Right"),
        "top_left" to viewModel.translate("أعلى اليسار", "Top Left")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Brand & Logo Config
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = viewModel.translate("الهوية البصرية والألوان", "Visual Branding & Palette"),
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(viewModel.translate("اسم التطبيق بالعربي", "Arabic App Title")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(viewModel.translate("اسم التطبيق بالإنجليزية", "English App Title")) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pColor,
                        onValueChange = { pColor = it },
                        label = { Text(viewModel.translate("رمز لون أساسي Hex", "Primary Hex Code")) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sColor,
                        onValueChange = { sColor = it },
                        label = { Text(viewModel.translate("رمز لون ثانوي Hex", "Secondary Hex Code")) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = footer,
                    onValueChange = { footer = it },
                    label = { Text(viewModel.translate("التذييل الترويجي / الهاتف والدعم", "Promo Footer / Support Label")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = welcomeAr,
                    onValueChange = { welcomeAr = it },
                    label = { Text(viewModel.translate("رسالة الترحيب بالعربية", "Welcome Message (AR)")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = welcomeEn,
                    onValueChange = { welcomeEn = it },
                    label = { Text(viewModel.translate("رسالة الترحيب بالإنجليزية", "Welcome Message (EN)")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(viewModel.translate("هاتف المنسقين", "Coordinator Phone")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(viewModel.translate("بريد المنسقين", "Coordinator Email")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passAdmin,
                    onValueChange = { passAdmin = it },
                    label = { Text(viewModel.translate("كلمة مرور المشرفين الجديدة", "Coordinator Login Password")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 2: Floating Assistant Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = viewModel.translate("إعدادات المساعد الذكي الطائر", "Floating Smart Assistant Config"),
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.translate("تفعيل المساعد الذكي", "Enable Smart Assistant"),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = viewModel.translate("إخفاء أو حذف المساعد تماماً من التطبيق", "Delete or disable the FAB entirely"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = assistantEnabled,
                        onCheckedChange = { assistantEnabled = it }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.translate("رؤية المساعد الذكي", "Assistant Visibility"),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = viewModel.translate("إخفاء مؤقت للزر العائم للمساعد", "Temporarily hide the assistant button"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = assistantVisible,
                        onCheckedChange = { assistantVisible = it }
                    )
                }

                HorizontalDivider()

                Text(
                    text = viewModel.translate("تعديل مكان المساعد الذكي", "Assistant Position Placement"),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    positionOptions.forEach { (posKey, label) ->
                        val isSelected = assistantPosition == posKey
                        val itemColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                        val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(itemColor)
                                .clickable { assistantPosition = posKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Customizable Top Bar Icons
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = viewModel.translate("تخصيص أيقونات شريط التنقل العلوي", "Custom Top Navigation Icons"),
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = viewModel.translate("قم بتحديد ظهور وأيقونة وترشيح ترتيب كل زر في الشريط العلوي:", "Customize the visibility, symbol, and display order of each nav icon:"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Render customization for each of the 5 icons
                // 1. Home Icon
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = homeVisible, onCheckedChange = { homeVisible = it ?: true })
                            Text(viewModel.translate("شاشة البداية / الرئيسية", "Home Screen Button"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = homeOrder,
                            onValueChange = { homeOrder = it },
                            label = { Text(viewModel.translate("الترتيب", "Order")) },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    if (homeVisible) {
                        var isExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = homeIcon,
                                onValueChange = {},
                                label = { Text(viewModel.translate("شكل الأيقونة", "Select Icon")) },
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { isExp = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = isExp, onDismissRequest = { isExp = false }) {
                                iconOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(getIconFromName(opt), null, modifier = Modifier.size(18.dp))
                                                Text(opt)
                                            }
                                        },
                                        onClick = { homeIcon = opt; isExp = false }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 2. Register Icon
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = registerVisible, onCheckedChange = { registerVisible = it ?: true })
                            Text(viewModel.translate("تسجيل مزود الخدمة (👤)", "Register Professional (👤)"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = registerOrder,
                            onValueChange = { registerOrder = it },
                            label = { Text(viewModel.translate("الترتيب", "Order")) },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    if (registerVisible) {
                        var isExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = registerIcon,
                                onValueChange = {},
                                label = { Text(viewModel.translate("شكل الأيقونة", "Select Icon")) },
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { isExp = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = isExp, onDismissRequest = { isExp = false }) {
                                iconOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(getIconFromName(opt), null, modifier = Modifier.size(18.dp))
                                                Text(opt)
                                            }
                                        },
                                        onClick = { registerIcon = opt; isExp = false }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 3. Admin Icon
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = adminVisible, onCheckedChange = { adminVisible = it ?: true })
                            Text(viewModel.translate("لوحة تحكم الإدارة (🔐)", "Admin Shield Link (🔐)"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = adminOrder,
                            onValueChange = { adminOrder = it },
                            label = { Text(viewModel.translate("الترتيب", "Order")) },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    if (adminVisible) {
                        var isExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = adminIcon,
                                onValueChange = {},
                                label = { Text(viewModel.translate("شكل الأيقونة", "Select Icon")) },
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { isExp = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = isExp, onDismissRequest = { isExp = false }) {
                                iconOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(getIconFromName(opt), null, modifier = Modifier.size(18.dp))
                                                Text(opt)
                                            }
                                        },
                                        onClick = { adminIcon = opt; isExp = false }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 4. Lang Icon
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = langVisible, onCheckedChange = { langVisible = it ?: true })
                            Text(viewModel.translate("تبديل اللغة (🌐)", "Language Switcher (🌐)"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = langOrder,
                            onValueChange = { langOrder = it },
                            label = { Text(viewModel.translate("الترتيب", "Order")) },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    if (langVisible) {
                        var isExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = langIcon,
                                onValueChange = {},
                                label = { Text(viewModel.translate("شكل الأيقونة", "Select Icon")) },
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { isExp = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = isExp, onDismissRequest = { isExp = false }) {
                                iconOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(getIconFromName(opt), null, modifier = Modifier.size(18.dp))
                                                Text(opt)
                                            }
                                        },
                                        onClick = { langIcon = opt; isExp = false }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 5. Refresh Icon
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = refreshVisible, onCheckedChange = { refreshVisible = it ?: true })
                            Text(viewModel.translate("زر التحديث الفوري (🔄)", "Refresh Portal Button (🔄)"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = refreshOrder,
                            onValueChange = { refreshOrder = it },
                            label = { Text(viewModel.translate("الترتيب", "Order")) },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    if (refreshVisible) {
                        var isExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = refreshIcon,
                                onValueChange = {},
                                label = { Text(viewModel.translate("شكل الأيقونة", "Select Icon")) },
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { isExp = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = isExp, onDismissRequest = { isExp = false }) {
                                iconOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(getIconFromName(opt), null, modifier = Modifier.size(18.dp))
                                                Text(opt)
                                            }
                                        },
                                        onClick = { refreshIcon = opt; isExp = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Save Everything
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            onClick = {
                val updatedConfig = config.copy(
                    appNameAr = nameAr,
                    appNameEn = nameEn,
                    primaryColor = pColor,
                    secondaryColor = sColor,
                    promoFooter = footer,
                    welcomeMessageAr = welcomeAr,
                    welcomeMessageEn = welcomeEn,
                    supportPhone = phone,
                    supportEmail = email,
                    adminPassword = passAdmin,
                    
                    assistantEnabled = assistantEnabled,
                    assistantVisible = assistantVisible,
                    assistantPosition = assistantPosition,
                    
                    navHomeVisible = homeVisible,
                    navHomeIcon = homeIcon,
                    navHomeOrder = homeOrder.toIntOrNull() ?: 1,
                    
                    navRegisterVisible = registerVisible,
                    navRegisterIcon = registerIcon,
                    navRegisterOrder = registerOrder.toIntOrNull() ?: 2,
                    
                    navAdminVisible = adminVisible,
                    navAdminIcon = adminIcon,
                    navAdminOrder = adminOrder.toIntOrNull() ?: 3,
                    
                    navLangVisible = langVisible,
                    navLangIcon = langIcon,
                    navLangOrder = langOrder.toIntOrNull() ?: 4,
                    
                    navRefreshVisible = refreshVisible,
                    navRefreshIcon = refreshIcon,
                    navRefreshOrder = refreshOrder.toIntOrNull() ?: 5
                )
                viewModel.saveGlobalConfig(updatedConfig)
                Toast.makeText(context, viewModel.translate("تم تحديث هوية التطبيق وتخصيص الأزرار والمساعد الطائر فورا!", "Dynamic Branding, Floating Assistant and top bar customization updated successfully!"), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(viewModel.translate("حفظ كافة إعدادات الهوية والتنقل والمساعد", "Save Branding, Navigation & Assistant Controls"))
        }
    }
}

// --- ORIGINAL COMPLAINT / REQUEST COMPONENT WITH String ID ADAPTATION ---
@Composable
fun CitizenSubmitFormSection(viewModel: PortalViewModel) {
    val title by viewModel.titleState.collectAsState()
    val category by viewModel.categoryState.collectAsState()
    val desc by viewModel.descriptionState.collectAsState()
    val name by viewModel.citizenNameState.collectAsState()
    val phone by viewModel.citizenPhoneState.collectAsState()

    val categories by viewModel.categories.collectAsState()
    var showDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = viewModel.translate("رفع بلاغ/طلب خدمة صيانة عام للمدير المباشر", "Report Public Utility Issue / Maintenance Request"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.titleState.value = it },
                label = { Text(viewModel.translate("عنوان المشكلة العام باختصار", "Inquiry/Complaint Outline Heading")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    label = { Text(viewModel.translate("القسم التقريبي للبلاغ", "Department Field")) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    categories.filter { it.isMain }.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(viewModel.translate(cat.nameAr, cat.nameEn)) },
                            onClick = {
                                viewModel.categoryState.value = cat.nameAr
                                showDropdown = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = desc,
                onValueChange = { viewModel.descriptionState.value = it },
                label = { Text(viewModel.translate("تفاصيل المشكلة والعنوان الكامل الجغرافي والظروف الميدانية", "Write fully detailed conditions & observations...")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.citizenNameState.value = it },
                label = { Text(viewModel.translate("اسم المواطن الكامل المبلّغ", "Submitter Full Name")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.citizenPhoneState.value = it },
                label = { Text(viewModel.translate("رقم هاتف المواطن للتواصل والمتابعة", "Contact Mobile of Submitter")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                colors = ButtonDefaults.buttonColors(containerColor = viewModel.getPrimaryColor()),
                onClick = { viewModel.submitServiceRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_service_request_button")
            ) {
                Text(viewModel.translate("إرسال البلاغ فورياً للتحليل بقسم العمليات", "Submit Issue to Operations Control Cell"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RequestsDashboardSection(viewModel: PortalViewModel) {
    val requests by viewModel.allRequests.collectAsState()
    val filterStat by viewModel.filterStatus.collectAsState()
    val filterCat by viewModel.filterCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val citizenPhoneFilter by viewModel.citizenPhoneFilter.collectAsState()

    // Filter Logic
    val displayRequests = requests.filter { req ->
        val statusMatch = when (filterStat) {
            "الكل", "All" -> true
            "جديد", "قيد الانتظار", "PENDING" -> req.status == "PENDING"
            "قيد التنفيذ", "IN_PROGRESS" -> req.status == "IN_PROGRESS"
            "تم الحل", "RESOLVED" -> req.status == "RESOLVED"
            else -> true
        }
        val catMatch = when (filterCat) {
            "الكل", "All" -> true
            else -> req.category == filterCat || req.suggestedCategory == filterCat
        }
        val phoneMatch = citizenPhoneFilter.isEmpty() || req.citizenPhone.contains(citizenPhoneFilter)
        
        statusMatch && catMatch && phoneMatch
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${viewModel.translate("بلاغات وشكاوى المواطنين", "Citizen Complaints Panel")} (${displayRequests.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = viewModel.getPrimaryColor()
            )
            if (citizenPhoneFilter.isNotEmpty()) {
                Text(
                    text = viewModel.translate("مصفى برقم الهاتف", "Filtered by Phone"),
                    fontSize = 11.sp,
                    color = Color.Red,
                    modifier = Modifier.clickable { viewModel.citizenPhoneFilter.value = "" }
                )
            }
        }

        // Filters UI Rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status Filters
            listOf("الكل", "جديد", "قيد التنفيذ", "تم الحل").forEach { stat ->
                FilterChip(
                    selected = filterStat == stat,
                    onClick = { viewModel.filterStatus.value = stat },
                    label = { Text(stat, fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (displayRequests.isEmpty()) {
                item {
                    Card {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(viewModel.translate("لا توجد بلاغات مرسلة في هذه القائمة حالياً.", "No issues reported in this list section."), color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(displayRequests) { request ->
                    RequestCard(request = request, viewModel = viewModel, onClick = { viewModel.selectRequest(request) })
                }
            }
        }
    }
}

@Composable
fun RequestCard(request: ServiceRequest, viewModel: PortalViewModel, onClick: () -> Unit) {
    val dateString = remember(request.dateCreated) {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())
        sdf.format(Date(request.dateCreated))
    }

    val categoryColor = when (request.priority) {
        "CRITICAL" -> Color(0xFFD32F2F)
        "HIGH" -> Color(0xFFE64A19)
        "MEDIUM" -> Color(0xFFF57C00)
        "LOW" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    val statusString = when (request.status) {
        "PENDING" -> viewModel.translate("جديد/للتحقق والبحث", "New Entry")
        "IN_PROGRESS" -> viewModel.translate("تحت التنفيذ الميداني", "Being Resolved")
        "RESOLVED" -> viewModel.translate("تم المعالجة والإغلاق بنجاح", "Successfully Resolved")
        else -> request.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Tag with Dynamic Color representation
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = request.priority,
                        color = categoryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(dateString, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(request.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(request.description, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${viewModel.translate("المواطن", "Citizen")}: ${request.citizenName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (request.status) {
                                "PENDING" -> Color.Gray.copy(alpha = 0.15f)
                                "IN_PROGRESS" -> viewModel.getPrimaryColor().copy(alpha = 0.15f)
                                "RESOLVED" -> viewModel.getSecondaryColor().copy(alpha = 0.15f)
                                else -> Color.LightGray
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusString,
                        color = when (request.status) {
                            "PENDING" -> Color.Gray
                            "IN_PROGRESS" -> viewModel.getPrimaryColor()
                            "RESOLVED" -> viewModel.getSecondaryColor()
                            else -> Color.DarkGray
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RequestDetailsDialog(request: ServiceRequest, viewModel: PortalViewModel, onDismiss: () -> Unit) {
    val summaryState by viewModel.summaryState.collectAsState()
    val isCitizen by viewModel.isCitizenRole.collectAsState()
    val primaryColor = viewModel.getPrimaryColor()
    val secondaryColor = viewModel.getSecondaryColor()

    var resNoteInput by remember { mutableStateOf(request.resolutionNote) }
    var changeStatusExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss() }
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Headline Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        viewModel.translate("متابعة البلاغ وحالة الصيانة", "Complaint Progress Tracking"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(request.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${viewModel.translate("المواطن المبلّغ", "Citizen name")}: ${request.citizenName} | ${request.citizenPhone}", fontSize = 11.sp)
                        Text("${viewModel.translate("القسم الموجه", "Target Field")}: ${request.category}", fontSize = 11.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        Divider()
                        Text(request.description, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // AI Priority Analysis response
                Card(
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.translate("توجيه العمليات الفوري بواسطة AI", "Automatic AI Operations Routing"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                        Text("${viewModel.translate("الأولوية التقديرية", "Calculated Priority")}: ${request.priority}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(request.priorityReason.ifEmpty { viewModel.translate("تم مطابقة البلاغ وتوجيهه فورا.", "Auto-routed appropriately.") }, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // AI Progress Summary Road Map (استخلاص المذكرات للمواطن)
                Card(
                    colors = CardDefaults.cardColors(containerColor = secondaryColor.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, "Workflow", tint = secondaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.translate("خطة العمل ومذكرة المعالجة الذكية", "AI Smart Resolution Roadmap"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
                        }

                        when (summaryState) {
                            is SmartSummaryState.Loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = secondaryColor)
                                    Text(viewModel.translate("جاري تفوير خطة الاستخلاص بصيغة مبرمجة...", "Generating personalized summary progress dots..."), fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            is SmartSummaryState.Success -> {
                                Text((summaryState as SmartSummaryState.Success).summary, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            is SmartSummaryState.Error -> {
                                Text((summaryState as SmartSummaryState.Error).message, fontSize = 11.sp, color = Color.Red)
                            }
                            else -> {}
                        }
                    }
                }

                // If admin logged in: Status & Note updating fields
                if (!isCitizen) {
                    Divider()
                    Text(viewModel.translate("صلاحيات التنسيق والصيانة للمنسق", "Coordinator Resolution Audit Actions"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = resNoteInput,
                        onValueChange = { resNoteInput = it },
                        label = { Text(viewModel.translate("مذكرة الصيانة / تدوين الإجراءات الفنية المنفذة", "Technical maintenance work execution notes...")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Button(onClick = { changeStatusExpanded = true }) {
                                Text("${viewModel.translate("تعديل حالة البلاغ", "Edit Status")} (${request.status})")
                            }
                            DropdownMenu(
                                expanded = changeStatusExpanded,
                                onDismissRequest = { changeStatusExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(viewModel.translate("جديد قيد الانتظار", "New/PENDING")) },
                                    onClick = {
                                        viewModel.updateRequestStatus(request.id, "PENDING", resNoteInput)
                                        changeStatusExpanded = false
                                        Toast.makeText(viewModel.getApplication(), "تم نقل حالة الشكوى إلى المعاينة والانتظار.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(viewModel.translate("قيد المعالجة الميدانية", "IN_PROGRESS")) },
                                    onClick = {
                                        viewModel.updateRequestStatus(request.id, "IN_PROGRESS", resNoteInput)
                                        changeStatusExpanded = false
                                        Toast.makeText(viewModel.getApplication(), "تم تعيين طاقم التدخل الفني الميداني ومباشرة المشكلة.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(viewModel.translate("تم المعالجة والإغلاق بنجاح", "RESOLVED")) },
                                    onClick = {
                                        viewModel.updateRequestStatus(request.id, "RESOLVED", resNoteInput)
                                        changeStatusExpanded = false
                                        Toast.makeText(viewModel.getApplication(), "تم إصلاح الخلل الفني وتم إغلاق البلاغ بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.deleteRequest(request)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Delete, "Delete File", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// Composition local placeholder for extended design coordinates
val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }
data class ExtendedColors(val customPrimary: Color = Color(0xFF007AFF), val customSecondary: Color = Color(0xFF34C759))
