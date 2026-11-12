package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.FloatingBubbleScreen
import com.example.ui.screens.LiveMicScreen
import com.example.ui.screens.RecordModulateScreen
import com.example.ui.screens.SavedAudiosScreen
import com.example.ui.screens.TelegramSettingsDialog
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TeleVoiceTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.VoiceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppNavTab(
    val route: String,
    val title: String,
    val hindiTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    RECORD(
        route = "record",
        title = "Record",
        hindiTitle = "रिकॉर्ड",
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.Mic
    ),
    BUBBLE(
        route = "bubble",
        title = "TG Bubble",
        hindiTitle = "फ्लोटिंग बबल",
        selectedIcon = Icons.Filled.Layers,
        unselectedIcon = Icons.Outlined.Layers
    ),
    LIVE(
        route = "live",
        title = "Live Mic",
        hindiTitle = "लाइव मॉड्यूलेशन",
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq
    ),
    SAVED(
        route = "saved",
        title = "Audios",
        hindiTitle = "सेव्ड ऑडियो",
        selectedIcon = Icons.Filled.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote
    )
}

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TeleVoiceTheme {
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(AppNavTab.RECORD) }
                var showTelegramSettings by remember { mutableStateOf(false) }

                // Listen for feedback messages
                LaunchedEffect(Unit) {
                    viewModel.userMessage.collectLatest { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                }

                // Handle back press
                if (currentTab != AppNavTab.RECORD) {
                    BackHandler {
                        currentTab = AppNavTab.RECORD
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = StudioCardBg,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioCardBg)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar")
                        ) {
                            AppNavTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) {
                                                if (tab == AppNavTab.BUBBLE) TelegramBlue else NeonPink
                                            } else TextMuted,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TextPrimary else TextMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = if (tab == AppNavTab.BUBBLE) TelegramBlue else NeonPink,
                                        selectedTextColor = TextPrimary,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = (if (tab == AppNavTab.BUBBLE) TelegramBlue else NeonPink).copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_item_${tab.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppNavTab.RECORD -> RecordModulateScreen(
                                viewModel = viewModel,
                                onOpenTelegramSettings = { showTelegramSettings = true }
                            )
                            AppNavTab.BUBBLE -> FloatingBubbleScreen(
                                viewModel = viewModel
                            )
                            AppNavTab.LIVE -> LiveMicScreen(
                                viewModel = viewModel
                            )
                            AppNavTab.SAVED -> SavedAudiosScreen(
                                viewModel = viewModel,
                                onNavigateToRecord = { currentTab = AppNavTab.RECORD }
                            )
                        }
                    }

                    if (showTelegramSettings) {
                        TelegramSettingsDialog(
                            viewModel = viewModel,
                            onDismiss = { showTelegramSettings = false }
                        )
                    }
                }
            }
        }
    }
}
