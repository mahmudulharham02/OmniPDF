package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

data class PDFTool(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTool: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val activeLang by viewModel.activeLang.collectAsState()
    val isDark by viewModel.isPitchDark.collectAsState()

    val tools = remember {
        listOf(
            PDFTool("scanner", "tool_scanner_title", "tool_scanner_desc", Icons.Default.CameraAlt, EmeraldGreen),
            PDFTool("viewer", "tool_viewer_title", "tool_viewer_desc", Icons.Default.MenuBook, RoyalBlue),
            PDFTool("split", "tool_split_title", "tool_split_desc", Icons.Default.ContentCut, WarmOrange),
            PDFTool("merge", "tool_merge_title", "tool_merge_desc", Icons.Default.MergeType, CrimsonRed),
            PDFTool("sign", "tool_sign_title", "tool_sign_desc", Icons.Default.Gesture, DeepIndigo),
            PDFTool("encrypt", "tool_encrypt_title", "tool_encrypt_desc", Icons.Default.EnhancedEncryption, CrimsonRed),
            PDFTool("watermark", "tool_watermark_title", "tool_watermark_desc", Icons.Default.BrandingWatermark, WarmOrange),
            PDFTool("pdftoimg", "tool_pdftoimg_title", "tool_pdftoimg_desc", Icons.Default.Image, EmeraldGreen),
            PDFTool("extract", "tool_extract_title", "tool_extract_desc", Icons.Default.CropOriginal, DeepIndigo)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.t("app_name"),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = viewModel.t("settings"),
                            tint = MaterialTheme.colorScheme.primary
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Responsive 2-column grid of tools
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("tools_grid")
            ) {
                items(tools) { tool ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) PitchDarkSurface else ClassicLightSurface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDark) 0.dp else 2.dp
                        ),
                        modifier = Modifier
                            .height(115.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToTool(tool.id) }
                            .testTag("tool_card_${tool.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Circular icon container with 15% opacity tint of signature color
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        color = tool.accentColor.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = viewModel.t(tool.titleKey),
                                    tint = tool.accentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = viewModel.t(tool.titleKey),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
