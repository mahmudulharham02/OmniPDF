package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolScreenController
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PDFManager
import com.example.viewmodel.MainViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDark by viewModel.isPitchDark.collectAsState()
            val useMonospace by viewModel.useMonospace.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val selectedFile by viewModel.selectedFile.collectAsState()

            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            // 1. DISPOSABLE LIFECYCLE OBSERVER
            // Detects when the user returns to the app from the Android system settings
            // and automatically refreshes permission statuses instantly.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.updatePermissionStatus()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MyApplicationTheme(
                isPitchDark = isDark,
                useMonospace = useMonospace
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Password Challenge state for AES-256 protected files
                    var showPasswordDialog by remember { mutableStateOf(false) }
                    var pendingEncryptedFile by remember { mutableStateOf<File?>(null) }
                    var challengePassword by remember { mutableStateOf("") }

                    // Monitor file changes to prompt for password if encrypted
                    LaunchedEffect(selectedFile) {
                        if (selectedFile != null && PDFManager.isOmniPdfEncrypted(selectedFile!!)) {
                            pendingEncryptedFile = selectedFile
                            showPasswordDialog = true
                        }
                    }

                    // Secure File Locker Password Dialog
                    if (showPasswordDialog && pendingEncryptedFile != null) {
                        AlertDialog(
                            onDismissRequest = {
                                showPasswordDialog = false
                                pendingEncryptedFile = null
                                challengePassword = ""
                            },
                            title = {
                                Text(
                                    text = "🔒 Secure Vault Password",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "This PDF file is protected inside the OmniPDF AES secure vault. Enter the password key to decrypt and view standard document assets.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    OutlinedTextField(
                                        value = challengePassword,
                                        onValueChange = { challengePassword = it },
                                        label = { Text("Locker Password") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().testTag("vault_password_input")
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.attemptDecryptAndOpen(
                                            pendingEncryptedFile!!,
                                            challengePassword,
                                            onSuccess = {
                                                showPasswordDialog = false
                                                pendingEncryptedFile = null
                                                challengePassword = ""
                                                Toast.makeText(context, "Decrypted successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, "Invalid password key. Decryption failed.", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier.testTag("vault_confirm_button")
                                ) {
                                    Text("Decrypt")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showPasswordDialog = false
                                        pendingEncryptedFile = null
                                        challengePassword = ""
                                    }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Screen router navigation logic
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToTool = { toolId -> viewModel.navigateTo(toolId) },
                            onNavigateToSettings = { viewModel.navigateTo("settings") }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("dashboard") }
                        )
                        else -> ToolScreenController(
                            toolId = currentScreen,
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("dashboard") }
                        )
                    }
                }
            }
        }
    }
}
