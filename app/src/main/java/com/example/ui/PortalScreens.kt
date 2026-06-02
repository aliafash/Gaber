package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Applicant
import com.example.data.Setting
import com.example.ui.theme.getActiveFontColor
import java.io.File

// Navigation Routes
object Routes {
    const val WELCOME = "welcome"
    const val APPLY = "apply"
    const val ADMIN = "admin"
    const val ASSISTANT = "assistant"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    viewModel: PortalViewModel,
    fontColor: Color,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Portal Title with optional Share button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share icon button if public share link is configured
                    if (!settingsState.shareLink.trim().isEmpty()) {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, settingsState.shareLink)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "مشاركة البوابة"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل الإجراء", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة",
                                tint = fontColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Text(
                        text = "بوابة الطلبات الذكية",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = fontColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Text(
                    text = "تقديم الطلبات ومتابعتها مدعوماً بالذكاء الاصطناعي من جيميني",
                    fontSize = 13.sp,
                    color = fontColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            // Custom Banner image loaded dynamically as requested for synchronization with all clients
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(settingsState.bannerImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "صورة البوابة المزامنة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Elegant Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        )
                        
                        Text(
                            text = "تسهيل الإجراءات برؤيتها الحديثة رقمياً",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }
            }

            // Introduction Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "مرحباً بك في البوابة",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "أيقونة الترحيب",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Text(
                            text = "نحن نسهل عليك تعبئة البيانات وتقديم الوثائق والمستندات بمرونة تامة. يمكنك استخدام المساعد الذكي المدمج لمساعدتك في صياغة تفاصيل طلبك أو توجيهك بالخطوات المطلوبة.",
                            fontSize = 14.sp,
                            color = fontColor.copy(alpha = 0.9f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }

            // Fast Navigation Buttons
            item {
                Button(
                    onClick = { onNavigate(Routes.APPLY) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "تقديم طلب جديد",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تقديم طلب جديد",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { onNavigate(Routes.ADMIN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = fontColor),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "دخول المسؤول (الآدمن)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "دخول المسؤول",
                            tint = fontColor
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyScreen(
    viewModel: PortalViewModel,
    fontColor: Color,
    onSuccessSubmit: () -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var selectedAppType by remember { mutableStateOf("عام ومراجعة") }
    var messageDetail by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // List of application types
    val appTypes = listOf("عام ومراجعة", "طلب تصديق وثائق", "استفسار طارئ", "معاملة تجارية / شركات")

    // Image Picker Launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageUri = uri
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "تقديم طلب جديد",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "يرجى تعبئة الحقول بعناية تامة. وتأكيد التفاصيل.",
                    fontSize = 13.sp,
                    color = fontColor.copy(alpha = 0.7f)
                )
            }

            // Name Field (Highlighting typed characters with dynamic chosen font color + bold)
            item {
                Text(
                    text = "الاسم الكامل للمتقدم",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("اكتب اسمك الثلاثي هنا", color = fontColor.copy(alpha = 0.5f)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Right
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // App Type Option Chips
            item {
                Text(
                    text = "نوع الطلب / المعاملة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    appTypes.forEach { type ->
                        val isSelected = selectedAppType == type
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clickable { selectedAppType = type }
                        ) {
                            Text(
                                text = type,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else fontColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Message / Detail text area (High contrast)
            item {
                Text(
                    text = "تفاصيل الطلب والرسالة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = messageDetail,
                    onValueChange = { messageDetail = it },
                    placeholder = { Text("اكتب تفاصيل معاملتك وشرح مبسط هنا لحاجة تقديمها للادمن", color = fontColor.copy(alpha = 0.5f)) },
                    minLines = 4,
                    maxLines = 6,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Upload Image / Pick Attachment Row
            item {
                Text(
                    text = "إرفاق صورة المستند أو البطاقة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (imageUri != null) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "المرفق المختار",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف المرفق",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, fontColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "لا يوجد صورة",
                                tint = fontColor.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("اختر صورة مرفق", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Add, contentDescription = "معرض الصور", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Button to submit details (High contrast)
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.submitApplication(
                            context = context,
                            fullName = fullName,
                            appType = selectedAppType,
                            message = messageDetail,
                            imageUri = imageUri,
                            onSuccess = {
                                isSubmitting = false
                                Toast.makeText(context, "تم تقديم المعاملة بنجاح!", Toast.LENGTH_LONG).show()
                                fullName = ""
                                messageDetail = ""
                                imageUri = null
                                onSuccessSubmit()
                            },
                            onError = { error ->
                                isSubmitting = false
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "إرسال المعاملة الرسمية", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Done, contentDescription = "إرسال")
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: PortalViewModel,
    fontColor: Color,
    currentTheme: String,
    currentFontName: String
) {
    val applicants by viewModel.allApplicants.collectAsState()
    var appFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, ACCEPTED, REJECTED
    var searchQuery by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Filter and search logic combined
    val filteredApplicants = applicants.filter { app ->
        val matchesSearch = app.fullName.contains(searchQuery, ignoreCase = true) ||
                app.message.contains(searchQuery, ignoreCase = true) ||
                app.applicationType.contains(searchQuery, ignoreCase = true)
        
        val matchesFilter = when (appFilter) {
            "PENDING" -> app.status == "PENDING"
            "ACCEPTED" -> app.status == "ACCEPTED"
            "REJECTED" -> app.status == "REJECTED"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Header with Theme Customizer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button for custom theme
                IconButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "تخصيص السمة والمظهر",
                        tint = fontColor
                    )
                }

                Text(
                    text = "لوحة تحكم المسؤول",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor
                )
            }

            Text(
                text = "إدارة ومراجعة المعاملات المقدمة وتغيير سمات وتنسيق التطبيق بالكامل.",
                fontSize = 12.sp,
                color = fontColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Search text field (Letters are bold and pure white for readability)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في الاسم أو نوع الطلب...", color = fontColor.copy(alpha = 0.5f)) },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Right
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "أيقونة البحث",
                        tint = fontColor.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val filterOptions = listOf(
                    "PENDING" to "قيد المراجعة",
                    "ACCEPTED" to "المقبولة",
                    "REJECTED" to "المرفوضة",
                    "ALL" to "الكل"
                )
                filterOptions.forEach { (key, label) ->
                    val isSelected = appFilter == key
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clickable { appFilter = key }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else fontColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Applicants list view
            if (filteredApplicants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد طلبات تطابق التصنيف المختار",
                        color = fontColor.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredApplicants) { applicant ->
                        ApplicantItem(
                            applicant = applicant,
                            fontColor = fontColor,
                            onAccept = { viewModel.updateStatus(applicant, "ACCEPTED") },
                            onReject = { viewModel.updateStatus(applicant, "REJECTED") },
                            onDelete = { viewModel.deleteApplication(applicant.id) }
                        )
                    }
                }
            }
        }
        
        // Custom Theme Customizer Overlay Dialog
        if (showThemeDialog) {
            val settingsState by viewModel.settingState.collectAsState()
            val context = LocalContext.current

            var welcomeBannerUrl by remember { mutableStateOf(settingsState.bannerImageUrl) }
            var appShareLink by remember { mutableStateOf(settingsState.shareLink) }
            var isAssistantVisible by remember { mutableStateOf(settingsState.showAssistant) }
            var assistantIconSizeVal by remember { mutableStateOf(settingsState.assistantIconSize) }
            var assistantIconTypeVal by remember { mutableStateOf(settingsState.assistantIconType) }
            var assistantLabelVal by remember { mutableStateOf(settingsState.assistantLabel) }

            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = {
                    Text(
                        text = "بوابة الإدارة: تخصيص مظهر وميزات التطبيق بالكامل",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = fontColor,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Theme Selector
                        Text(
                            text = "اختر السمة الجمالية (التصميم):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        val themes = listOf(
                            "SLATE" to "كوزميك سيلفر",
                            "GOLD" to "الذهبي الفاخر",
                            "EMERALD" to "الزمردي الراقي"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            themes.forEach { (key, name) ->
                                val isSelected = currentTheme == key
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clickable { viewModel.updateTheme(key, currentFontName) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = name,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else fontColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Custom font colors
                        Text(
                            text = "لون خطوط تعبئة البيانات والحديث:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val fontColors = listOf(
                            "WHITE" to "أبيض ناصع",
                            "GOLD" to "ذهبي فاتح",
                            "SILVER" to "فضي متوهج"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            fontColors.forEach { (key, name) ->
                                val isSelected = currentFontName == key
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clickable { viewModel.updateTheme(currentTheme, key) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = name,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else fontColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = fontColor.copy(alpha = 0.2f))

                        // Welcome Custom Banner imageUrl (Synchronized with all client devices)
                        Text(
                            text = "رابط الصورة الترحيبية (تتزامن مع الجميع):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = welcomeBannerUrl,
                            onValueChange = { welcomeBannerUrl = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Right
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = fontColor.copy(alpha = 0.4f),
                                cursorColor = Color.White
                            )
                        )

                        // Public Share link parameter
                        Text(
                            text = "رابط مشاركة التطبيق:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = appShareLink,
                            onValueChange = { appShareLink = it },
                            placeholder = { Text("مثال: https://google.com", color = fontColor.copy(alpha = 0.4f)) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Right
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = fontColor.copy(alpha = 0.4f),
                                cursorColor = Color.White
                            )
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = fontColor.copy(alpha = 0.2f))

                        // Assistant preferences toggle
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = isAssistantVisible,
                                onCheckedChange = { isAssistantVisible = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            )
                            Text(
                                text = "تفعيل المساعد الذكي في التذييل",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor
                            )
                        }

                        if (isAssistantVisible) {
                            // Assistant bottom label
                            Text(
                                text = "لقب/اسم المساعد الذكي في التذييل:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = assistantLabelVal,
                                onValueChange = { assistantLabelVal = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Right
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = fontColor.copy(alpha = 0.4f),
                                    cursorColor = Color.White
                                )
                            )

                            // Assistant icon custom options
                            Text(
                                text = "اختر شكل أيقونة المساعد:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val iconOptions = listOf(
                                "STAR" to "نجمة",
                                "NOTIFICATIONS" to "تنبيهات",
                                "INFO" to "معلومات",
                                "FACE" to "مساعد ذكي"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                iconOptions.forEach { (type, name) ->
                                    val isSelected = assistantIconTypeVal.uppercase() == type
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier
                                            .padding(start = 6.dp)
                                            .clickable { assistantIconTypeVal = type },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else fontColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Assistant icon custom size text field
                            Text(
                                text = "حجم أيقونة المساعد بالـ (dp) [من 12 إلى 32]:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fontColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = assistantIconSizeVal.toString(),
                                onValueChange = { input ->
                                    val size = input.toIntOrNull() ?: 18
                                    if (size in 12..32) {
                                        assistantIconSizeVal = size
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Right
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = fontColor.copy(alpha = 0.4f),
                                    cursorColor = Color.White
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = fontColor.copy(alpha = 0.2f))

                        // Cloud & Backup Restorer Actions Pane
                        Text(
                            text = "نظام الحماية من الحفظ المباشر (نسخة احتياطية):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val restored = viewModel.checkAndRestoreFromBackup()
                                    if (restored) {
                                        Toast.makeText(context, "تم استرجاع ومزامنة البيانات وتصميم الأدمن من النسخة السحابية بنجاح!", Toast.LENGTH_LONG).show()
                                        showThemeDialog = false
                                    } else {
                                        Toast.makeText(context, "لا توجد نسخة احتياطية محفوظة حالياً", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("استرجاع البيانات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "جاري إرسال الإعدادات وتأمين المزامنة الذكية على كافة أجهزة المستخدمين...", Toast.LENGTH_SHORT).show()
                                    viewModel.updateFullConfig(
                                        settingsState.copy(
                                            bannerImageUrl = welcomeBannerUrl,
                                            shareLink = appShareLink,
                                            showAssistant = isAssistantVisible,
                                            assistantIconSize = assistantIconSizeVal,
                                            assistantIconType = assistantIconTypeVal,
                                            assistantLabel = assistantLabelVal
                                        )
                                    )
                                    Toast.makeText(context, "تمت المزامنة السحابية وقفل النسخ الاحتياطي بنجاح!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("مزامنة سحابية وقفل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateFullConfig(
                                settingsState.copy(
                                    bannerImageUrl = welcomeBannerUrl,
                                    shareLink = appShareLink,
                                    showAssistant = isAssistantVisible,
                                    assistantIconSize = assistantIconSizeVal,
                                    assistantIconType = assistantIconTypeVal,
                                    assistantLabel = assistantLabelVal
                                )
                            )
                            showThemeDialog = false
                            Toast.makeText(context, "تم حفظ ومزامنة التخصيص!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("تم التحديث واحفظ القفل")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun ApplicantItem(
    applicant: Applicant,
    fontColor: Color,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (applicant.status) {
                            "ACCEPTED" -> Color(0xFF1B5E20)
                            "REJECTED" -> Color(0xFFB71C1C)
                            else -> Color(0xFFFF6F00)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = when (applicant.status) {
                            "ACCEPTED" -> "مقبول"
                            "REJECTED" -> "مرفوض"
                            else -> "تحت المراجعة"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = applicant.fullName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            // Subcategory / application type
            Text(
                text = "نوع الطلب: ${applicant.applicationType}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Application details
            Text(
                text = applicant.message,
                fontSize = 14.sp,
                color = fontColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Sycned Image section from local path (Solves URI issue perfectly so image always renders to Admin!)
            applicant.imagePath?.let { path ->
                Spacer(modifier = Modifier.height(10.dp))
                val imageFile = File(path)
                if (imageFile.exists()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "المرفق المرفوع من المتقدم",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Decision Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete Application Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الطلب",
                        tint = Color.Red
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("رفض الطلب", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Close, contentDescription = "رفض", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("قبول الطلب", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Check, contentDescription = "قبول", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAssistantScreen(
    viewModel: PortalViewModel,
    fontColor: Color
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    var userQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.clearChat() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "مسح المحادثة",
                        tint = fontColor.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = "المساعد الذكي (Gemini)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor
                )
            }

            Text(
                text = "اسألني عن متطلبات أي معاملة أو صياغة طلبك بطريقة رسمية.",
                fontSize = 12.sp,
                color = fontColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Conversation Chat View
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    val isAi = message.sender == "AI"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isAi) 0.dp else 12.dp,
                                bottomEnd = if (isAi) 12.dp else 0.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAi) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isAi) "المساعد" else "أنت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    textAlign = if (isAi) TextAlign.Left else TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    fontSize = 14.sp,
                                    color = if (isAi) fontColor else MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جاري توليد الرد من الذكاء الاصطناعي...", fontSize = 12.sp, color = fontColor)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Chat Input Bar (Character letters are fully visible matching the Font Theme choice)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.askGeminiAssistant(userQuery)
                        userQuery = ""
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال السؤال المعاملة",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    placeholder = { Text("اكتب سؤالك هنا بوضوح للاستفسار...", color = fontColor.copy(alpha = 0.5f)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Right
                    ),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}
