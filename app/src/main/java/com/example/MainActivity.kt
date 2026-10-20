package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.FocusTab
import com.example.ui.screens.calculator.CalculatorScreen
import com.example.ui.screens.calculator.CalculatorViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.limits.AppLimitsScreen
import com.example.ui.screens.limits.AppLimitsViewModel
import com.example.ui.screens.notes.NotesScreen
import com.example.ui.screens.notes.NotesViewModel
import com.example.ui.theme.StudyFocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyFocusTheme {
                StudyFocusMainApp()
            }
        }
    }
}

@Composable
fun StudyFocusMainApp() {
    var currentTab by rememberSaveable { mutableStateOf(FocusTab.DASHBOARD) }

    // Intercept hardware back button to return to Dashboard tab
    BackHandler(enabled = currentTab != FocusTab.DASHBOARD) {
        currentTab = FocusTab.DASHBOARD
    }

    val dashboardViewModel: DashboardViewModel = viewModel()
    val appLimitsViewModel: AppLimitsViewModel = viewModel()
    val notesViewModel: NotesViewModel = viewModel()
    val calculatorViewModel: CalculatorViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                FocusTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) { tab ->
            when (tab) {
                FocusTab.DASHBOARD -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTab = { newTab -> currentTab = newTab }
                )
                FocusTab.APP_LIMITS -> AppLimitsScreen(
                    viewModel = appLimitsViewModel,
                    onNavigateToTab = { newTab -> currentTab = newTab }
                )
                FocusTab.STUDY_NOTES -> NotesScreen(
                    viewModel = notesViewModel
                )
                FocusTab.CALCULATOR -> CalculatorScreen(
                    viewModel = calculatorViewModel
                )
            }
        }
    }
}
