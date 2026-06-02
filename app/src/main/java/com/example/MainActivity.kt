package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.PortalDatabase
import com.example.data.PortalRepository
import com.example.ui.*
import com.example.ui.theme.PortalTheme
import com.example.ui.theme.getActiveFontColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Room Database
        val db = Room.databaseBuilder(
            applicationContext,
            PortalDatabase::class.java,
            "portal_database"
        ).fallbackToDestructiveMigration().build()

        val repository = PortalRepository(db.portalDao())

        // ViewModel Factory setup
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PortalViewModel(repository) as T
            }
        }

        setContent {
            val portalViewModel: PortalViewModel = viewModel(factory = viewModelFactory)
            val settingsState by portalViewModel.settingState.collectAsState()
            
            val activeFontColor = getActiveFontColor(settingsState.selectedFontColor)

            PortalTheme(themeName = settingsState.selectedTheme) {
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        CustomBottomNavigation(
                            navController = navController,
                            fontColor = activeFontColor
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.WELCOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(Routes.WELCOME) {
                            WelcomeScreen(
                                fontColor = activeFontColor,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }
                        composable(Routes.APPLY) {
                            ApplyScreen(
                                viewModel = portalViewModel,
                                fontColor = activeFontColor,
                                onSuccessSubmit = {
                                    navController.navigate(Routes.WELCOME) {
                                        popUpTo(Routes.WELCOME) { inclusive = false }
                                    }
                                }
                            )
                        }
                        composable(Routes.ADMIN) {
                            AdminDashboardScreen(
                                viewModel = portalViewModel,
                                fontColor = activeFontColor,
                                currentTheme = settingsState.selectedTheme,
                                currentFontName = settingsState.selectedFontColor
                            )
                        }
                        composable(Routes.ASSISTANT) {
                            SmartAssistantScreen(
                                viewModel = portalViewModel,
                                fontColor = activeFontColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    navController: NavController,
    fontColor: Color
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Menu Item
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "الرئيسية",
                isSelected = currentRoute == Routes.WELCOME,
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = fontColor.copy(alpha = 0.6f),
                onClick = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            // Submit Menu Item
            BottomNavItem(
                icon = Icons.Default.Edit,
                label = "تقديم طلب",
                isSelected = currentRoute == Routes.APPLY,
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = fontColor.copy(alpha = 0.6f),
                onClick = {
                    navController.navigate(Routes.APPLY) {
                        popUpTo(Routes.WELCOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            // Admin Menu Item
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "المسؤول",
                isSelected = currentRoute == Routes.ADMIN,
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = fontColor.copy(alpha = 0.6f),
                onClick = {
                    navController.navigate(Routes.ADMIN) {
                        popUpTo(Routes.WELCOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            // Customized AI Assistant bottom item: smaller size and lowered down y-offset
            val isAssistantSelected = currentRoute == Routes.ASSISTANT
            Column(
                modifier = Modifier
                    .clickable {
                        navController.navigate(Routes.ASSISTANT) {
                            popUpTo(Routes.WELCOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star, // Beautiful star icon
                    contentDescription = "المساعد الذكي",
                    tint = if (isAssistantSelected) MaterialTheme.colorScheme.primary else fontColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(18.dp) // Smaller size as requested (Standard icons are 24dp)
                        .offset(y = 2.dp) // Lowered down slightly as requested
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "المساعد",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAssistantSelected) MaterialTheme.colorScheme.primary else fontColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val color = if (isSelected) activeColor else inactiveColor
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
