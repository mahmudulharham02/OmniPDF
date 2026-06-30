package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val activeLang by viewModel.activeLang.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()
    val useMonospace by viewModel.useMonospace.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.t("settings"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = viewModel.t("back"),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = viewModel.t("appearance"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section 1: Themes (Classic Light vs. Pitch Dark)
            Text(
                text = viewModel.t("theme_label"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ThemeSelectorItem(
                title = viewModel.t("theme_dark"),
                isSelected = isDark,
                onClick = { viewModel.setPitchDark(true) },
                isDark = isDark,
                testTag = "theme_dark_option"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ThemeSelectorItem(
                title = viewModel.t("theme_light"),
                isSelected = !isDark,
                onClick = { viewModel.setPitchDark(false) },
                isDark = isDark,
                testTag = "theme_light_option"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Typography Style
            Text(
                text = viewModel.t("typography_label"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ThemeSelectorItem(
                title = viewModel.t("typo_sans"),
                isSelected = !useMonospace,
                onClick = { viewModel.setUseMonospace(false) },
                isDark = isDark,
                testTag = "typo_sans_option"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ThemeSelectorItem(
                title = viewModel.t("typo_mono"),
                isSelected = useMonospace,
                onClick = { viewModel.setUseMonospace(true) },
                isDark = isDark,
                testTag = "typo_mono_option"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Localization Language Options
            Text(
                text = viewModel.t("language_label"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LanguageSelectorItem(
                langName = viewModel.t("lang_en"),
                langCode = "en",
                isSelected = activeLang == "en",
                onClick = { viewModel.setLanguage("en") },
                isDark = isDark
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LanguageSelectorItem(
                langName = viewModel.t("lang_es"),
                langCode = "es",
                isSelected = activeLang == "es",
                onClick = { viewModel.setLanguage("es") },
                isDark = isDark
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LanguageSelectorItem(
                langName = viewModel.t("lang_fr"),
                langCode = "fr",
                isSelected = activeLang == "fr",
                onClick = { viewModel.setLanguage("fr") },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(40.dp))
            
            // Design compliance footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isDark) PitchDarkSurface else ClassicLightSurface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "OmniPDF Utility Engine v1.0.0",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A11y Compliant • Fully Secure • 100% Offline",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isDark) PitchDarkTextSecondary else ClassicLightTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ThemeSelectorItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isDark) PitchDarkSurface else ClassicLightSurface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageSelectorItem(
    langName: String,
    langCode: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isDark) PitchDarkSurface else ClassicLightSurface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag("lang_option_$langCode"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = langName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
