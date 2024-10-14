package com.google.mediapipe.examples.chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mediapipe.examples.chatbot.ui.theme.LLMInferenceTheme

const val START_SCREEN = "start_screen"
const val CHAT_SCREEN = "chat_screen"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LLMInferenceTheme {
                val navController = rememberNavController()
                val hasPermission = PermissionUtils.hasUsageStatsPermission(this)

                if (!hasPermission) {
                    PermissionRequestScreen(onGrantPermission = {
                        PermissionUtils.openUsageAccessSettings(this)
                    })
                } else {
                    Scaffold(
                        topBar = { AppBar() }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = START_SCREEN
                            ) {
                                composable(START_SCREEN) {
                                    LoadingRoute(
                                        onModelLoaded = {
                                            navController.navigate(CHAT_SCREEN) {
                                                popUpTo(START_SCREEN) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                composable(CHAT_SCREEN) {
                                    ChatRoute()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppBar() {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    }

    @Composable
    fun PermissionRequestScreen(onGrantPermission: () -> Unit) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "This app requires access to your usage statistics to monitor the number of running applications. Please, grant access then restart the app.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Grant Usage Access")
                }
            }
        }
    }
}
