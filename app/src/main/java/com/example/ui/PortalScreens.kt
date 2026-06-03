package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Routes {
    const val WELCOME = "welcome"
    const val APPLY = "apply"
    const val ADMIN = "admin"
    const val ASSISTANT = "assistant"
}

@Composable
fun WelcomeScreen(
    viewModel: PortalViewModel,
    fontColor: Color,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingState.collectAsState()
    val providers by viewModel.allServiceProviders.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()
    val commentsList by viewModel.allReviews.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf("") }
    
    // Filtering States
    var minRatingFilter by remember { mutableStateOf(0) }
    var priceFilterLevel by remember { mutableStateOf("") } // "", "LOW", "MEDIUM", "HIGH"
    var selectedRegionStr by remember { mutableStateOf("") }
    var showOnlyRecommended by remember { mutableStateOf(false) }
    var isDataSaverOn by remember { mutableStateOf(false) }

    // Map Toggle
    var showMapView by remember { mutableStateOf(false) }

    // Active detail/profile viewer
    var selectedProviderForDetail by remember { mutableStateOf<ServiceProvider?>(null) }

    // Voice search simulation
    var isListeningVoice by remember { mutableStateOf(false) }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                searchQuery = results[0]
                Toast.makeText(context, "البحث الصوتي: ${results[0]}", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Hidden backdoor taps
    var backdoorTapsCount by remember { mutableStateOf(0) }
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var backdoorPasswordInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // Filter providers
    val filteredProviders = remember(providers, searchQuery, selectedCategoryName, minRatingFilter, priceFilterLevel, selectedRegionStr, showOnlyRecommended) {
        providers.filter { provider ->
            val matchesSearch = provider.fullName.contains(searchQuery, ignoreCase = true) ||
                    provider.phone.contains(searchQuery) ||
                    provider.workAddress.contains(searchQuery, ignoreCase = true)
            
            val matchesCategory = selectedCategoryName.isEmpty() || provider.categoryName == selectedCategoryName
            val matchesRating = provider.ratingAvg >= minRatingFilter
            val matchesRegion = selectedRegionStr.isEmpty() || provider.residenceArea.contains(selectedRegionStr, ignoreCase = true)
            val matchesRec = !showOnlyRecommended || provider.isRecommended

            matchesSearch && matchesCategory && matchesRating && matchesRegion && matchesRec
        }.sortedWith(compareByDescending<ServiceProvider> { it.isPinnedToTop }.thenByDescending { it.ratingAvg })
    }

    // Top Bar Colors custom
    val appBackground = when (settingsState.selectedTheme.uppercase()) {
        "SLATE" -> Color(0xFF0F172A)
        "EMERALD" -> Color(0xFF022C22)
        else -> Color(0xFF121212) // Charcoal Accent Gold
    }

    val cardBgColor = when (settingsState.selectedTheme.uppercase()) {
        "SLATE" -> Color(0xFF1E293B)
        "EMERALD" -> Color(0xFF064E3B)
        else -> Color(0xFF1C1C1E)
    }

    // Backdoor Entrance
    if (showBackdoorDialog) {
        AlertDialog(
            onDismissRequest = { showBackdoorDialog = false },
            title = { Text("البوابة الخلفية السرية", color = fontColor, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("أدخل رمز تسجيل المالك لتخطي عقبة تسجيل الدخول ومزامنة الإعدادات:", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backdoorPasswordInput,
                        onValueChange = { backdoorPasswordInput = it },
                        singleLine = true,
                        placeholder = { Text("••••••••") },
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backdoorPasswordInput == "maher--736462") {
                            showBackdoorDialog = false
                            Toast.makeText(context, "تم تفعيل صلاحيات مالك التطبيق بنجاح!", Toast.LENGTH_LONG).show()
                            onNavigate(Routes.ADMIN)
                        } else {
                            Toast.makeText(context, "الرمز خاطئ!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("تحقق ودخول")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackdoorDialog = false }) { Text("إلغاء", color = Color.Red) }
            },
            containerColor = cardBgColor
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 2. STAGE: Custom RESTRICTED Top App Bar containing exactly requested Order and logic
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(top = 28.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)) {
                    // Title Header with logo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "AR") "بوابة الخدمات" else "Services Portal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = fontColor
                        )

                        // Top bar actions ordered exactly as requested from Right to Left!
                        // Right to Left Order: 1. 🔄 | 2. 🌐 | 3. 🌙 | 4. ⚙️ | 5. 👤 | 6. 🏠
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. 🔄 Refresh
                            if (settingsState.iconRefreshVisible) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    selectedCategoryName = ""
                                    minRatingFilter = 0
                                    priceFilterLevel = ""
                                    selectedRegionStr = ""
                                    showOnlyRecommended = false
                                    Toast.makeText(context, "تم التحديث وإعادة تعيين التصفية", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = settingsState.iconRefreshText, tint = fontColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            // 2. 🌐 Language Switcher
                            if (settingsState.iconLangVisible) {
                                IconButton(onClick = {
                                    viewModel.toggleLanguage()
                                    Toast.makeText(context, "Language updated!", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = settingsState.iconLangText, tint = fontColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            // 3. 🌙 Mode switch (toggles cosmic style / charcoal Gold)
                            if (settingsState.iconNightVisible) {
                                IconButton(onClick = {
                                    val nextTheme = if (settingsState.selectedTheme == "GOLD") "SLATE" else "GOLD"
                                    viewModel.updateTheme(nextTheme, settingsState.selectedFontColor)
                                    Toast.makeText(context, "تم تغيير تباين الألوان: $nextTheme", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = settingsState.iconNightText, tint = fontColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            // 4. ⚙️ Admin & Supervisor gate
                            if (settingsState.iconAdminVisible) {
                                IconButton(onClick = { onNavigate(Routes.ADMIN) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Settings, contentDescription = settingsState.iconAdminText, tint = fontColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            // 5. 👤 Pro Register Intake Screen
                            if (settingsState.iconRegisterVisible) {
                                IconButton(onClick = { onNavigate(Routes.APPLY) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Person, contentDescription = settingsState.iconRegisterText, tint = fontColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            // 6. 🏠 Home logo with 5-tap hidden doorway password prompt
                            if (settingsState.iconHomeVisible) {
                                IconButton(
                                    onClick = {
                                        backdoorTapsCount++
                                        if (backdoorTapsCount >= 5) {
                                            backdoorTapsCount = 0
                                            showBackdoorDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = settingsState.iconHomeText, tint = fontColor, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Screen contents
            if (settingsState.isMaintenanceMode) {
                // Maintenance Lock Screen
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "الصيانة", tint = Color.Red, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("وضع صيانة تحت الترقية", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(settingsState.maintenanceMessage, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Welcome Custom Display Banner Sized/colored by Admin
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.8f)),
                            border = BorderStroke(1.dp, fontColor.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (settingsState.bannerImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = settingsState.bannerImageUrl,
                                        contentDescription = "Welcome Banner Picture",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = settingsState.welcomeText,
                                    fontSize = settingsState.welcomeTextSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fontColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Advanced Search Bar & Filters Section
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = fontColor) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Voice recognition button
                                        IconButton(onClick = {
                                            try {
                                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث للبحث عن المهنيين في البوابة...")
                                                }
                                                speechLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "البحث الصوتي غير مدعوم على هذا الجهاز محاكاة فقط.", Toast.LENGTH_SHORT).show()
                                                searchQuery = "كهرباء"
                                            }
                                        }) {
                                            Icon(Icons.Default.Face, contentDescription = "البحث بالصوت", tint = fontColor)
                                        }
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Red)
                                            }
                                        }
                                    }
                                },
                                placeholder = { Text(settingsState.searchPlaceholder, color = Color.Gray, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = fontColor.copy(alpha = 0.5f)
                                ),
                                textStyle = TextStyle(color = Color.White)
                            )
                        }
                    }

                    // Multi-Dimensional Filtering Row
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("خيارات الفلترة والتصفية والبحث المتقدم:", fontSize = 12.sp, color = fontColor, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Recommended Toggle
                            FilterChip(
                                selected = showOnlyRecommended,
                                onClick = { showOnlyRecommended = !showOnlyRecommended },
                                label = { Text("الموصى بهم ⭐", fontSize = 11.sp, color = Color.White) }
                            )

                            // Rating Filters
                            listOf(0, 3, 4, 5).forEach { star ->
                                FilterChip(
                                    selected = minRatingFilter == star,
                                    onClick = { minRatingFilter = star },
                                    label = { Text(if (star == 0) "الكل ★" else "$star ★ وأكثر", fontSize = 11.sp, color = Color.White) }
                                )
                            }

                            // Price options
                            listOf("", "قريب", "متوسط", "بعيد").forEach { dist ->
                                FilterChip(
                                    selected = selectedRegionStr == dist,
                                    onClick = { selectedRegionStr = if (dist == "الكل") "" else dist },
                                    label = { Text(if (dist.isEmpty()) "كل المسافات" else "مسافة: $dist", fontSize = 11.sp, color = Color.White) }
                                )
                            }
                        }
                    }

                    // Map Toggle button
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("أقسام المهن والخدمات:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            Button(
                                onClick = { showMapView = !showMapView },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (showMapView) Icons.Default.List else Icons.Default.Place, contentDescription = "خريطة", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showMapView) "جدول القوائم" else "عرض الخريطة التفاعلية", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Interactive Custom Map View Integration
                    if (showMapView) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            InteractiveCustomMap(
                                providers = filteredProviders,
                                fontColor = fontColor,
                                cardBgBg = cardBgColor,
                                onSelectProvider = { selectedProviderForDetail = it }
                            )
                        }
                    }

                    // Horizontal Scrolling Categories list
                    if (!showMapView) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedCategoryName.isEmpty()) MaterialTheme.colorScheme.primary else cardBgColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable { selectedCategoryName = "" }
                                    ) {
                                        Text(
                                            text = "الكل (${providers.size})",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                items(categories) { cat ->
                                    val isSelected = selectedCategoryName == cat.name
                                    val count = providers.count { it.categoryName == cat.name }
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else cardBgColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable { selectedCategoryName = cat.name }
                                    ) {
                                        Text(
                                            text = "${cat.name} ($count)",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // List of registered Service Providers
                    if (!showMapView) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "مقدمو الخدمات المعتمدون والمقترحات لك:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor
                            )
                        }

                        if (filteredProviders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا يوجد مقدمي خدمة يطابقون خيارات البحث الحالية.", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(filteredProviders) { provider ->
                                ServiceProviderCard(
                                    provider = provider,
                                    fontColor = fontColor,
                                    cardBg = cardBgColor,
                                    onDetailsClick = {
                                        selectedProviderForDetail = provider
                                        viewModel.incrementClicks(provider) // increment popular clicks
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Universal Sized Center Footer Text customizable/editable by Creator
            if (settingsState.footerTextVisible && settingsState.footerText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = settingsState.footerText,
                        fontSize = 10.sp, // 50% smaller by default
                        color = fontColor.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Floating Assistant Smart Button - customizable layout size/color
        if (settingsState.showAssistant) {
            FloatingActionButton(
                onClick = { onNavigate(Routes.ASSISTANT) },
                containerColor = when (settingsState.assistantColor.uppercase()) {
                    "SILVER" -> Color(0xFFECEFF1)
                    "WHITE" -> Color.White
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 20.dp)
                    .size(settingsState.assistantIconSize.dp * 3) // Customizable scaling factor
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = settingsState.assistantLabel,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(settingsState.assistantLabel, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Sheet showing all Details, reviews, and interactive direct messaging
    selectedProviderForDetail?.let { provider ->
        ServiceProviderDetailsDialog(
            provider = provider,
            viewModel = viewModel,
            fontColor = fontColor,
            cardBg = cardBgColor,
            commentsList = commentsList.filter { it.providerId == provider.id },
            onDismiss = { selectedProviderForDetail = null }
        )
    }
}

@Composable
fun InteractiveCustomMap(
    providers: List<ServiceProvider>,
    fontColor: Color,
    cardBgBg: Color,
    onSelectProvider: (ServiceProvider) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBgBg),
        border = BorderStroke(1.dp, fontColor.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Draw interactive dots representation
            Canvas(modifier = Modifier
                .fillMaxSize()
                .clickable {
                    // Click somewhere simulated to highlight closest
                }) {
                // Background simulated grid
                drawRect(color = Color.Black.copy(alpha = 0.2f))
            }

            // Pins layout representation overlay
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    "خريطة المعاينة المباشرة (النقاط)", 
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                    fontSize = 11.sp, color = fontColor
                )

                // Simulated pins positioning randomly on Canvas layout for viewability!
                providers.forEachIndexed { index, provider ->
                    val offsetValX = (50 + (index * 75) % 300).dp
                    val offsetValY = (40 + (index * 50) % 150).dp
                    
                    Box(
                        modifier = Modifier
                            .offset(x = offsetValX, y = offsetValY)
                            .size(36.dp)
                            .background(Color.Red, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { onSelectProvider(provider) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "Pin", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    
                    // Float Tag above pin
                    Box(
                        modifier = Modifier
                            .offset(x = offsetValX - 20.dp, y = offsetValY - 25.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(provider.fullName.substringBefore(" "), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceProviderCard(
    provider: ServiceProvider,
    fontColor: Color,
    cardBg: Color,
    onDetailsClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onDetailsClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (provider.isPinnedToTop) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(modifier = Modifier.size(68.dp)) {
                if (provider.profileImagePath != null && File(provider.profileImagePath).exists()) {
                    AsyncImage(
                        model = File(provider.profileImagePath),
                        contentDescription = "صورة مقدم الخدمة",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "No photo", tint = Color.LightGray)
                    }
                }
                
                // Verified Badge representation
                if (provider.isVerified) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Blue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "موثق", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = provider.fullName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = fontColor
                    )
                    
                    if (provider.isPinnedToTop) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD4AF37), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("مثبت 📌", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "${provider.categoryName} » ${provider.subCategoryName}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, contentDescription = "موقع", tint = fontColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${provider.residenceArea} | ${provider.workAddress}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Clicks & popular counter badge
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "تقييم", tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(String.format("%.1f", provider.ratingAvg), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("الزيارات: ${provider.clicksCount}", fontSize = 9.sp, color = fontColor)
                    }
                }
            }

            // Contact shortcuts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Call Buttons shortcut
                IconButton(onClick = {
                    try {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                        context.startActivity(callIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "لا يمكن الاتصال برقم ${provider.phone}", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color.Green, modifier = Modifier.size(18.dp))
                }

                // WhatsApp message trigger
                IconButton(onClick = {
                    try {
                        val waUrl = "https://wa.me/${provider.phone}"
                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                        context.startActivity(waIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "لا يمكن إرسال واتساب برقم ${provider.phone}", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "واتس", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// Dialog sheet showing pro profile, previous comments, new comment composer, inside chat
@Composable
fun ServiceProviderDetailsDialog(
    provider: ServiceProvider,
    viewModel: PortalViewModel,
    fontColor: Color,
    cardBg: Color,
    commentsList: List<Review>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("PROFILE") } // "PROFILE", "REVIEWS", "CHAT"

    // Chat states
    val activeChatMessages by viewModel.chatMessagesWithProvider.collectAsState()
    var chatMessageText by remember { mutableStateOf("") }

    // Review states
    var reviewerNameIn by remember { mutableStateOf("") }
    var selectedRatingVal by remember { mutableStateOf(5) }
    var reviewCommentText by remember { mutableStateOf("") }

    // Load messages on launch
    LaunchedEffect(provider.id) {
        viewModel.loadChatForProvider(provider.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(provider.fullName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = fontColor)
                Text("${provider.categoryName} » ${provider.subCategoryName}", fontSize = 11.sp, color = Color.Gray)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                // Tab controllers Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = { activeTab = "PROFILE" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "PROFILE") MaterialTheme.colorScheme.primary else Color.DarkGray),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text("الملف", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { activeTab = "REVIEWS" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "REVIEWS") MaterialTheme.colorScheme.primary else Color.DarkGray),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text("التقييمات", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { activeTab = "CHAT" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == "CHAT") MaterialTheme.colorScheme.primary else Color.DarkGray),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text("محادثة", fontSize = 10.sp)
                    }
                }

                HorizontalDivider(color = fontColor.copy(alpha = 0.3f))

                when (activeTab) {
                    "PROFILE" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                            item {
                                // Profile Images Gallery
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    if (provider.profileImagePath != null && File(provider.profileImagePath).exists()) {
                                        AsyncImage(
                                            model = File(provider.profileImagePath),
                                            contentDescription = "صورة شخصية",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Gray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = "ID", modifier = Modifier.size(48.dp))
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("تفاصيل ومعلومات التواصل المباشرة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fontColor)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("رقم جوال الاتصال: ${provider.phone}", fontSize = 12.sp, color = Color.White)
                                Text("موقع ورشة ومقر العمل: ${provider.workAddress}", fontSize = 12.sp, color = Color.White)
                                Text("محيط ومحافظة السكن: ${provider.residenceArea}", fontSize = 12.sp, color = Color.White)
                                Text("ساعات التوفر والدوام: يتوفر يومياً من الساعة ٩ صباحاً حتى ١٠ مساءً", fontSize = 12.sp, color = Color.Gray)
                            }

                            // Share Action containing Web Share simulation link customizable by admin
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "مشاركة مقدم خدمات متميز")
                                            putExtra(Intent.EXTRA_TEXT, "أوصي بالتواصل مع ${provider.fullName} متخصص في ${provider.categoryName}.\nجوال: ${provider.phone}\nيمكنك تحميل التطبيق عبر الرابط: https://ai.studio/build")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الموفر"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("مشاركة ملف مقدم الخدمة", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    "REVIEWS" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                            // Submitter new Review Composer form
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("أضف تفييمك وتجربتك للعلن:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fontColor)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("النجوم:", fontSize = 10.sp, color = Color.White)
                                            listOf(1, 2, 3, 4, 5).forEach { star ->
                                                IconButton(onClick = { selectedRatingVal = star }, modifier = Modifier.size(24.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Star",
                                                        tint = if (star <= selectedRatingVal) Color(0xFFD4AF37) else Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = reviewerNameIn,
                                            onValueChange = { reviewerNameIn = it },
                                            placeholder = { Text("اسم المقيّم ثلاثي", fontSize = 11.sp) },
                                            singleLine = true,
                                            textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = fontColor)
                                        )
                                        OutlinedTextField(
                                            value = reviewCommentText,
                                            onValueChange = { reviewCommentText = it },
                                            placeholder = { Text("تفاصيل وتجربتك عن تعامل المهني...", fontSize = 11.sp) },
                                            minLines = 2,
                                            maxLines = 3,
                                            textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = fontColor)
                                        )
                                        Button(
                                            onClick = {
                                                if (reviewerNameIn.trim().isEmpty() || reviewCommentText.trim().isEmpty()) {
                                                    Toast.makeText(context, "الرجاء تعبئة الاسم والتعليق", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.addReview(provider.id, reviewerNameIn, selectedRatingVal, reviewCommentText)
                                                    reviewerNameIn = ""
                                                    reviewCommentText = ""
                                                    Toast.makeText(context, "تم حفظ تعليقك بنجاح ومزامنته!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("تقديم النجمات والتقييم للادمن وحفظه", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            items(commentsList) { review ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(review.reviewerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = fontColor)
                                            Row {
                                                repeat(review.rating) {
                                                    Icon(Icons.Default.Star, contentDescription = "*", tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                        Text(review.comment, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    "CHAT" -> {
                        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                            // Messages stream
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                if (activeChatMessages.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("لا توجد رسائل سابقة. ابدأ المحادثة الفورية الآمنة الآن!", fontSize = 11.sp, color = Color.Gray)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(activeChatMessages) { msg ->
                                            val isMe = msg.senderType == "USER"
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                            ) {
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isMe) MaterialTheme.colorScheme.primary else Color.DarkGray
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(msg.messageText, color = Color.White, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Chat input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatMessageText,
                                    onValueChange = { chatMessageText = it },
                                    placeholder = { Text("اكتب رسالة...", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = fontColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (chatMessageText.trim().isNotEmpty()) {
                                            viewModel.sendChatMessageToProvider(provider.id, chatMessageText, "USER")
                                            chatMessageText = ""
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("إغلاق") }
        },
        containerColor = cardBg
    )
}

// 👤 Professional Registration Intake form screen
@Composable
fun ApplyScreen(
    viewModel: PortalViewModel,
    fontColor: Color,
    onSuccessSubmit: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var phoneNo by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("") }
    var selectedSub by remember { mutableStateOf("") }
    var currentWorkCenterAddress by remember { mutableStateOf("") }
    var currentResidenceDistrict by remember { mutableStateOf("") }
    var mockCoordinates by remember { mutableStateOf("") }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var idCardUri by remember { mutableStateOf<Uri?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }

    // Picker launch configurations to local storage memory
    val profileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) profileImageUri = uri }
    )

    val idCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) idCardUri = uri }
    )

    val cardBg = when (viewModel.settingState.value.selectedTheme.uppercase()) {
        "SLATE" -> Color(0xFF1E293B)
        "EMERALD" -> Color(0xFF064E3B)
        else -> Color(0xFF1C1C1E)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(vertical = 16.dp, horizontal = 16.dp)
            ) {
                Text("استمارة تسجيل أصحاب المهن ومقدمي الخدمات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fontColor, modifier = Modifier.align(Alignment.Center))
            }
        },
        containerColor = when (viewModel.settingState.value.selectedTheme.uppercase()) {
            "SLATE" -> Color(0xFF0F172A)
            "EMERALD" -> Color(0xFF022C22)
            else -> Color(0xFF121212)
        }
    ) { innerPack ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPack)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("شروط وطلبات التقديم الرسمية:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                Text("يرجى إدخال البيانات بدقة كاملة لتجنب رفض طلب المحترفين والمشرفين للملف الخاص بك.", fontSize = 11.sp, color = Color.Gray)
            }

            // Full Name (Triple required)
            item {
                Text("الاسم الثلاثي الكامل (إجباري) *", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("مثال: ماهر محمد طاهر") },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Phone
            item {
                Text("رقم الهاتف الفعال / واتساب (إجباري) *", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = phoneNo,
                    onValueChange = { phoneNo = it },
                    placeholder = { Text("مثال: 777644670") },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category & Service type drop down simulated
            item {
                Text("القسم والخدمة الرئيسية (إجباري) *", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                
                // Show grid of buttons for categories selection
                Text("اختر القسم العملي رئيسياً:", fontSize = 11.sp, color = Color.White)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCat == cat.name,
                            onClick = {
                                selectedCat = cat.name
                                selectedSub = cat.subCategoriesCsv.split(",").firstOrNull() ?: ""
                            },
                            label = { Text(cat.name, fontSize = 10.sp, color = Color.White) }
                        )
                    }
                }

                if (selectedCat.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("اختر التخصص الفرعي:", fontSize = 11.sp, color = Color.White)
                    val listSubs = categories.find { it.name == selectedCat }?.subCategoriesCsv?.split(",") ?: emptyList()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listSubs) { sub ->
                            FilterChip(
                                selected = selectedSub == sub,
                                onClick = { selectedSub = sub },
                                label = { Text(sub, fontSize = 10.sp, color = Color.White) }
                            )
                        }
                    }
                }
            }

            // Current work address
            item {
                Text("مكان وعنوان مركز/مكتب العمل الحالي (إجباري) *", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = currentWorkCenterAddress,
                    onValueChange = { currentWorkCenterAddress = it },
                    placeholder = { Text("مثال: شارع حدة - صنعاء") },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // District/Residence
            item {
                Text("منطقة الدائرة السكنية الحالية (إجباري) *", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = currentResidenceDistrict,
                    onValueChange = { currentResidenceDistrict = it },
                    placeholder = { Text("مثال: مديرية السبعين") },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Map Coordinates GPS simulated
            item {
                Text("إحداثيات وموقع الخريطة GPS (اختياري)", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = mockCoordinates,
                    onValueChange = { mockCoordinates = it },
                    placeholder = { Text("مثال: 15.3694,44.1910") },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Personal photo uploading image
            item {
                Text("تحميل صورتك الشخصية للملف (إجباري) *", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        profileLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("رفع صورة شخصية")
                    }
                    if (profileImageUri != null) {
                        Text("تم اختيار الصورة ✓", color = Color.Green, fontSize = 11.sp)
                    } else {
                        Text("لم ترفع أي صورة", color = Color.Red, fontSize = 11.sp)
                    }
                }
            }

            // ID card photo uploading
            item {
                Text("صورة بطاقة الهوية الشخصية (اختياري)", fontSize = 13.sp, color = fontColor, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        idCardLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("رفع بطاقة الهوية")
                    }
                    if (idCardUri != null) {
                        Text("تم اختيار الصورة ✓", color = Color.Green, fontSize = 11.sp)
                    } else {
                        Text("اختياري", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            // Submission triggers
            item {
                Spacer(modifier = Modifier.height(12.dp))
                if (isSubmitting) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            isSubmitting = true
                            viewModel.submitCandidateApplication(
                                context = context,
                                fullName = fullName,
                                phone = phoneNo,
                                category = selectedCat,
                                subCategory = selectedSub,
                                profileImageUri = profileImageUri,
                                workAddress = currentWorkCenterAddress,
                                residenceArea = currentResidenceDistrict,
                                mapLocation = mockCoordinates,
                                idCardUri = idCardUri,
                                onSuccess = {
                                    isSubmitting = false
                                    Toast.makeText(context, "تم رفع استمارة تسجيلك بنجاح! للمراجعة من الإدارة.", Toast.LENGTH_LONG).show()
                                    onSuccessSubmit()
                                },
                                onError = { err ->
                                    isSubmitting = false
                                    Toast.makeText(context, "خطأ: $err", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("تقديم طلب الانضمام للمراجعة الفورية", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ⚙️ Admin & Supervisor Control Panel Dashboard UI
@Composable
fun AdminDashboardScreen(
    viewModel: PortalViewModel,
    fontColor: Color,
    currentTheme: String,
    currentFontName: String
) {
    val context = LocalContext.current
    val supervisors by viewModel.allSupervisors.collectAsState()
    val applicants by viewModel.allApplicants.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val serviceProviders by viewModel.allServiceProviders.collectAsState()
    val activityLogs by viewModel.allActivityLogs.collectAsState()
    val reviewsAll by viewModel.allReviews.collectAsState()

    var isLoggedIn by remember { mutableStateOf(false) }
    var activeAdminUsername by remember { mutableStateOf("") }
    var activeAdminType by remember { mutableStateOf("") } // "SUPERADMIN", "SUPERVISOR"

    // Inputs form login
    var inputUser by remember { mutableStateOf("") }
    var inputPass by remember { mutableStateOf("") }
    var inputOtfaCode by remember { mutableStateOf("") }
    var inputSaveLogin by remember { mutableStateOf(true) }

    // Forms management triggers
    var adminTabSelected by remember { mutableStateOf("SETTINGS") } // "SETTINGS", "PROVIDERS", "APPLICANTS", "SUPERVISORS", "CATEGORIES", "AUDIT"

    // New Supervisor Forms states
    var newSupUser by remember { mutableStateOf("") }
    var newSupPass by remember { mutableStateOf("") }
    var isNewSup2Fa by remember { mutableStateOf(false) }

    // New Category states
    var newCatName by remember { mutableStateOf("") }
    var newCatSubsCsv by remember { mutableStateOf("") }

    // Configurations state edits
    val settingsState by viewModel.settingState.collectAsState()
    var editWelcomeText by remember { mutableStateOf(settingsState.welcomeText) }
    var editWelcomeSize by remember { mutableStateOf(settingsState.welcomeTextSize) }
    var editBannerUrl by remember { mutableStateOf(settingsState.bannerImageUrl) }
    var editFooterText by remember { mutableStateOf(settingsState.footerText) }
    var isMaintenanceOn by remember { mutableStateOf(settingsState.isMaintenanceMode) }
    var maintenanceMsgText by remember { mutableStateOf(settingsState.maintenanceMessage) }

    val scope = rememberCoroutineScope()

    val cardBg = when (currentTheme.uppercase()) {
        "SLATE" -> Color(0xFF1E293B)
        "EMERALD" -> Color(0xFF064E3B)
        else -> Color(0xFF1C1C1E)
    }

    if (!isLoggedIn) {
        // Render Login Gateway box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when (currentTheme.uppercase()) {
                        "SLATE" -> Color(0xFF0F172A)
                        "EMERALD" -> Color(0xFF022C22)
                        else -> Color(0xFF121212)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("بوابة تسجيل دخول المشرفين والمدراء", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fontColor)
                    
                    OutlinedTextField(
                        value = inputUser,
                        onValueChange = { inputUser = it },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputPass,
                        onValueChange = { inputPass = it },
                        label = { Text("رمز المرور") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputOtfaCode,
                        onValueChange = { inputOtfaCode = it },
                        label = { Text("رمز الهاتف الثنائي 2FA (اختياري للمشرف)") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حفظ بيانات تسجيل الدخول", fontSize = 11.sp, color = Color.White)
                        Switch(checked = inputSaveLogin, onCheckedChange = { inputSaveLogin = it })
                    }

                    Button(
                        onClick = {
                            // Check SuperAdmin Credentials
                            if (inputUser == "admin" && inputPass == "maher736462") {
                                isLoggedIn = true
                                activeAdminUsername = "admin"
                                activeAdminType = "SUPERADMIN"
                                viewModel.logAction("admin", "LOGIN", "سجل دخول كمدير رئيسي للنظام")
                                Toast.makeText(context, "أهلاً بك يا مدير النظام!", Toast.LENGTH_SHORT).show()
                            } else {
                                // Check Supervisor record in storage
                                val match = supervisors.find { it.username == inputUser && it.password == inputPass }
                                if (match != null) {
                                    isLoggedIn = true
                                    activeAdminUsername = match.username
                                    activeAdminType = "SUPERVISOR"
                                    viewModel.logAction(match.username, "LOGIN", "سجل دخول كمعاون مشرف")
                                    Toast.makeText(context, "أهلاً بك يا مشرف البوابة: ${match.username}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "البيانات المدخلة غير صحيحة!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("دخول للوحة التحكم")
                    }
                }
            }
        }
    } else {
        // Authenticated Admin Dashboard Screen
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("لوحة إدارة النظام والمشرفين", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            Text("المسؤول النشط: $activeAdminUsername (${if (activeAdminType == "SUPERADMIN") "مالك التطبيق" else "مشرف فرعي"})", fontSize = 10.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                isLoggedIn = false
                                inputPass = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("خروج", fontSize = 11.sp)
                        }
                    }
                }
            },
            containerColor = when (currentTheme.uppercase()) {
                "SLATE" -> Color(0xFF0F172A)
                "EMERALD" -> Color(0xFF022C22)
                else -> Color(0xFF121212)
            }
        ) { innerPad ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPad)
            ) {
                // Tab switcher Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = if (activeAdminType == "SUPERADMIN") {
                        listOf(
                            "SETTINGS" to "الواجهات والعروض",
                            "PROVIDERS" to "مقدمو الخدمات",
                            "APPLICANTS" to "طلبات الانضمام",
                            "SUPERVISORS" to "إدارة المشرفين",
                            "CATEGORIES" to "المهن والأقسام",
                            "AUDIT" to "سجلات النشاط"
                        )
                    } else {
                        // Supervisor constraints
                        listOf(
                            "PROVIDERS" to "مقدمو الخدمات",
                            "APPLICANTS" to "طلبات الانضمام"
                        )
                    }

                    tabs.forEach { (route, label) ->
                        Button(
                            onClick = { adminTabSelected = route },
                            colors = ButtonDefaults.buttonColors(containerColor = if (adminTabSelected == route) MaterialTheme.colorScheme.primary else Color.DarkGray)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = fontColor.copy(alpha = 0.3f))

                // Active Display tab body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (adminTabSelected) {
                        "SETTINGS" -> {
                            item {
                                Text("تعديل العروض والإسنادات العامة والترحيبية:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            }
                            item {
                                OutlinedTextField(
                                    value = editWelcomeText,
                                    onValueChange = { editWelcomeText = it },
                                    label = { Text("النص الترحيبي للدليل") },
                                    textStyle = TextStyle(color = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = editWelcomeSize.toString(),
                                    onValueChange = { editWelcomeSize = it.toIntOrNull() ?: 18 },
                                    label = { Text("حجم الخط الترحيبي (بكسل sp)") },
                                    textStyle = TextStyle(color = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = editBannerUrl,
                                    onValueChange = { editBannerUrl = it },
                                    label = { Text("رابط صورة بنر الترحيب العائم (مميز)") },
                                    textStyle = TextStyle(color = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = editFooterText,
                                    onValueChange = { editFooterText = it },
                                    label = { Text("شعار التذييل البرمجي (مثال: WAM777644670)") },
                                    textStyle = TextStyle(color = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تفعيل وضع الصيانة العام بالقفل", color = Color.White, fontSize = 12.sp)
                                    Switch(checked = isMaintenanceOn, onCheckedChange = { isMaintenanceOn = it })
                                }
                                if (isMaintenanceOn) {
                                    OutlinedTextField(
                                        value = maintenanceMsgText,
                                        onValueChange = { maintenanceMsgText = it },
                                        label = { Text("رسالة وضع المعاينة والصيانة") },
                                        textStyle = TextStyle(color = Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            item {
                                Button(
                                    onClick = {
                                        viewModel.updateFullConfig(
                                            settingsState.copy(
                                                welcomeText = editWelcomeText,
                                                welcomeTextSize = editWelcomeSize,
                                                bannerImageUrl = editBannerUrl,
                                                footerText = editFooterText,
                                                isMaintenanceMode = isMaintenanceOn,
                                                maintenanceMessage = maintenanceMsgText
                                            )
                                        )
                                        Toast.makeText(context, "تم حفظ الإعدادات ومزامنتها على جميع الأجهزة فوراً!", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ المزامنة الآن")
                                }
                            }

                            // Database Backup panel
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("إدارة نسخ الحفظ والنسخة الاحتياطية سحابياً:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        val path = viewModel.backupDatabaseToDownload()
                                        Toast.makeText(context, "تم حفظ نسخة احتياطية في Download بنجاح! المسار: $path", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("أخذ نسخة احتياطية إلى بطاقة الذاكرة")
                                }
                            }
                        }

                        "PROVIDERS" -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("التحكم بمقدمي المهن والتثبيت في الصدارة:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                                    Button(onClick = {
                                        val csvPath = viewModel.exportProvidersToCsv(context)
                                        Toast.makeText(context, "تم تصدير مقدمي الخدمة بنجاح إلى CSV! المسار: $csvPath", Toast.LENGTH_LONG).show()
                                    }) {
                                        Text("تصدير إلى Excel", fontSize = 10.sp)
                                    }
                                }
                            }

                            items(serviceProviders) { provider ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(provider.fullName, fontWeight = FontWeight.Bold, color = fontColor)
                                        Text("مهنة: ${provider.categoryName} - هاتف: ${provider.phone}", color = Color.White)
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = provider.isPinnedToTop,
                                                    onCheckedChange = { viewModel.updateProviderPinStatus(provider, it, activeAdminUsername) }
                                                )
                                                Text("تثبيت بصدارة البحث", fontSize = 10.sp, color = Color.White)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = provider.isRecommended,
                                                    onCheckedChange = { viewModel.updateProviderRecommendedStatus(provider, it, activeAdminUsername) }
                                                )
                                                Text("توصية المشرف", fontSize = 10.sp, color = Color.White)
                                            }
                                            IconButton(onClick = { viewModel.deleteProvider(provider.id, activeAdminUsername) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "APPLICANTS" -> {
                            item {
                                Text("طلبات الانضمام المعلقة من المتقدمين بالمهن:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            }
                            if (applicants.isEmpty()) {
                                item {
                                    Text("لا توجد طلبات انضمام معلقة حالياً في السيرفر التخزيني.", color = Color.Gray, fontSize = 12.sp)
                                }
                            } else {
                                items(applicants) { applicant ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = cardBg)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Applicant Personal profile image inspection BEFORE accept
                                                if (applicant.profileImagePath != null && File(applicant.profileImagePath).exists()) {
                                                    AsyncImage(
                                                        model = File(applicant.profileImagePath),
                                                        contentDescription = "inspection profile",
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                }
                                                Column {
                                                    Text(applicant.fullName, fontWeight = FontWeight.Bold, color = fontColor)
                                                    Text("القسم المقترح: ${applicant.category}", fontSize = 12.sp, color = Color.White)
                                                    Text("منطقة الإقامة: ${applicant.residenceArea}", fontSize = 11.sp, color = Color.LightGray)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("عنوان مقر ورشة العمل: ${applicant.workAddress}", fontSize = 11.sp, color = Color.White)
                                            Text("هاتف التواصل المباشر: ${applicant.phone}", fontSize = 11.sp, color = Color.White)

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Button(
                                                    onClick = { viewModel.processRegistrationRequest(applicant, true, activeAdminUsername) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                                ) {
                                                    Text("قبول الطلب وتوثيقه", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = { viewModel.processRegistrationRequest(applicant, false, activeAdminUsername) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                                ) {
                                                    Text("رفض وحذف", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "SUPERVISORS" -> {
                            item {
                                Text("إضافة مشرفين مساعدين وتعيين رمز المرور والصلاحية:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            }
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("إضافة حساب مشرف جديد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fontColor)
                                        OutlinedTextField(
                                            value = newSupUser,
                                            onValueChange = { newSupUser = it },
                                            placeholder = { Text("المعرف (مثال: supervisor1)") },
                                            textStyle = TextStyle(color = Color.White)
                                        )
                                        OutlinedTextField(
                                            value = newSupPass,
                                            onValueChange = { newSupPass = it },
                                            placeholder = { Text("الرمز السري") },
                                            textStyle = TextStyle(color = Color.White)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("تفعيل توثيق هاتف 2FA للمشرف", fontSize = 11.sp, color = Color.White)
                                            Switch(checked = isNewSup2Fa, onCheckedChange = { isNewSup2Fa = it })
                                        }
                                        Button(onClick = {
                                            if (newSupUser.trim().isNotEmpty() && newSupPass.trim().isNotEmpty()) {
                                                viewModel.addManagerSupervisor(
                                                    Supervisor(username = newSupUser, password = newSupPass, is2FaEnabled = isNewSup2Fa),
                                                    activeAdminUsername
                                                )
                                                newSupUser = ""
                                                newSupPass = ""
                                                Toast.makeText(context, "تمت إضافة المشرف بنجاح!", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Text("حفظ المشرف الجديد")
                                        }
                                    }
                                }
                            }

                            items(supervisors) { sup ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("الحساب: ${sup.username}", fontWeight = FontWeight.Bold, color = fontColor)
                                            Text("صلاحية قبول الطلبات: نعم | إضافة موفرين: نعم", fontSize = 10.sp, color = Color.White)
                                        }
                                        IconButton(onClick = { viewModel.deleteManagerSupervisor(sup.id, activeAdminUsername) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        "CATEGORIES" -> {
                            item {
                                Text("إدارة الهياكل والأقسام المهنية والخدمية بالفهرس:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                            }
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("إضافة قسم وفروع فرعية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fontColor)
                                        OutlinedTextField(
                                            value = newCatName,
                                            onValueChange = { newCatName = it },
                                            placeholder = { Text("اسم القسم الجديد (مثال: محاسبة وقانون)") },
                                            textStyle = TextStyle(color = Color.White)
                                        )
                                        OutlinedTextField(
                                            value = newCatSubsCsv,
                                            onValueChange = { newCatSubsCsv = it },
                                            placeholder = { Text("الخدمات الفرعية مفرقة بفاصلة (مثال: محاماة,تدقيق)") },
                                            textStyle = TextStyle(color = Color.White)
                                        )
                                        Button(onClick = {
                                            if (newCatName.trim().isNotEmpty()) {
                                                viewModel.addMainCategory(
                                                    Category(name = newCatName, subCategoriesCsv = newCatSubsCsv.ifEmpty { "عام" }),
                                                    activeAdminUsername
                                                )
                                                newCatName = ""
                                                newCatSubsCsv = ""
                                                Toast.makeText(context, "تمت إضافة القسم الخدماتي بنجاح ومزامنته للجميع!", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Text("حفظ القسم")
                                        }
                                    }
                                }
                            }

                            items(categories) { cat ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cat.name, fontWeight = FontWeight.Bold, color = fontColor)
                                            Text("الفروع: ${cat.subCategoriesCsv}", fontSize = 10.sp, color = Color.White)
                                        }
                                        IconButton(onClick = { viewModel.deleteMainCategory(cat.id, activeAdminUsername) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        "AUDIT" -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("سجل نشاطات وعمليات المشرفين (خاص بالمالك):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fontColor)
                                    Button(onClick = {
                                        val p = viewModel.exportReviewsToCsv(context)
                                        Toast.makeText(context, "تم تصدير التقييمات كأرشيف PDF/CSV! المسار: $p", Toast.LENGTH_LONG).show()
                                    }) {
                                        Text("تصدير التقييمات", fontSize = 10.sp)
                                    }
                                }
                            }

                            items(activityLogs) { log ->
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("قام به: ${log.username}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = fontColor)
                                            Text(date, fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Text(log.details, fontSize = 11.sp, color = Color.White)
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

// Smart assistant conversation with Gemini API AI helper
@Composable
fun SmartAssistantScreen(
    viewModel: PortalViewModel,
    fontColor: Color
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    var userMessageState by remember { mutableStateOf("") }

    val cardBg = when (viewModel.settingState.value.selectedTheme.uppercase()) {
        "SLATE" -> Color(0xFF1E293B)
        "EMERALD" -> Color(0xFF064E3B)
        else -> Color(0xFF1C1C1E)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المساعد والذكي لبوابة الخدمات", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fontColor)
                    Button(onClick = { viewModel.clearChat() }) {
                        Text("مسح", fontSize = 11.sp)
                    }
                }
            }
        },
        containerColor = when (viewModel.settingState.value.selectedTheme.uppercase()) {
            "SLATE" -> Color(0xFF0F172A)
            "EMERALD" -> Color(0xFF022C22)
            else -> Color(0xFF121212)
        }
    ) { innerPack ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPack)
                .padding(16.dp)
        ) {
            Text("مرحباً بك! اطرح أي استفسار حول المهن، كيفية الانضمام، أو شروط قبول المحترفين:", fontSize = 11.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))

            // Message list stream
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { chat ->
                    val isMe = chat.sender == "USER"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primary else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = chat.text,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                if (isChatLoading) {
                    item {
                        Row(modifier = Modifier.fillWithMaxWidthRow(), horizontalArrangement = Arrangement.Start) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("جاري معالجة وتوليد الرد الذكي الموثق...", modifier = Modifier.padding(10.dp), fontSize = 12.sp, color = fontColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User input composer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userMessageState,
                    onValueChange = { userMessageState = it },
                    placeholder = { Text("مثال: كيف يمكن تقديم اعتراض أو طلب تصديق وثائق؟", fontSize = 12.sp) },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = fontColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (userMessageState.trim().isNotEmpty()) {
                            viewModel.askGeminiAssistant(userMessageState)
                            userMessageState = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "أرسل")
                }
            }
        }
    }
}

// Helper Extension for simple grid rows wrapping nicely
fun Modifier.fillWithMaxWidthRow() = this.fillMaxWidth()
