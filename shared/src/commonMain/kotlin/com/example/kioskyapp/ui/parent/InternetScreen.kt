package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.WebFilterApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.CreateContentFilterRequest
import com.example.kioskyapp.models.ContentFilterDTO
import com.example.kioskyapp.models.FilterType
import com.example.kioskyapp.models.UpdateContentFilterRequest
import kotlinx.coroutines.launch

@Composable
fun InternetScreen(
    token: String,
    childId: String,
    parentId: String,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBrowsersClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val api = remember { WebFilterApi() }

    val categoryByValue = remember { mutableStateMapOf<String, ContentFilterDTO>() }

    var safeSearchEnabled by remember { mutableStateOf(false) }
    var youtubeSafeSearchEnabled by remember { mutableStateOf(false) }
    var youtubeMonitoringEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch existing settings
    LaunchedEffect(token, childId) {
        if (childId.isBlank()) return@LaunchedEffect
        isLoading = true
        errorMessage = null
        val result = api.getContentFilters(token, childId)
        if (result is Result.Success) {
            categoryByValue.clear()
            result.data
                .filter { it.filter_type == FilterType.CATEGORY }
                .forEach { filter ->
                    val existing = categoryByValue[filter.value]
                    if (existing == null) {
                        categoryByValue[filter.value] = filter
                    } else {
                        val existingCreatedAt = existing.created_at
                        val newCreatedAt = filter.created_at
                        val shouldReplace = existingCreatedAt == null ||
                            (newCreatedAt != null && newCreatedAt > existingCreatedAt)
                        if (shouldReplace) categoryByValue[filter.value] = filter
                    }
                }

            safeSearchEnabled = categoryByValue["SAFE_SEARCH"]?.is_blocked ?: false
            youtubeSafeSearchEnabled = categoryByValue["YOUTUBE_SAFE_SEARCH"]?.is_blocked ?: false
            youtubeMonitoringEnabled = categoryByValue["YOUTUBE_MONITORING"]?.is_blocked ?: false
        } else if (result is Result.Error) {
            errorMessage = result.exception.message ?: "Failed to load internet settings"
        }
        isLoading = false
    }

    val upsertCategorySetting = { key: String, enabled: Boolean, onRollback: () -> Unit, onApply: (Boolean) -> Unit ->
        scope.launch {
            errorMessage = null
            val existing = categoryByValue[key]

            val result: Result<ContentFilterDTO> = if (!existing?.id.isNullOrBlank()) {
                api.updateContentFilter(
                    token = token,
                    filterId = existing!!.id!!,
                    request = UpdateContentFilterRequest(is_blocked = enabled)
                )
            } else {
                api.createContentFilter(
                    token = token,
                    request = CreateContentFilterRequest(
                        parent_id = parentId,
                        child_id = childId,
                        filter_type = FilterType.CATEGORY,
                        value = key,
                        is_blocked = enabled
                    )
                )
            }

            if (result is Result.Success) {
                categoryByValue[key] = result.data
                onApply(result.data.is_blocked)
            } else if (result is Result.Error) {
                errorMessage = result.exception.message ?: "Failed to update setting"
                onRollback()
            }
        }
    }

    Scaffold(
        backgroundColor = Color(0xFFF5F9FF),
        topBar = {
            TopAppBar(
                title = { Text("Internet", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Back", tint = Color.Black)
                    }
                },
                backgroundColor = Color.White,
                elevation = 0.dp
            )
        },
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp) {
                BottomNavigationItem(selected = false, onClick = onHomeClick, icon = { Icon(Icons.Default.GridView, contentDescription = null) }, label = { Text("Home") })
                BottomNavigationItem(selected = false, onClick = onMapClick, icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }, label = { Text("Map") })
                BottomNavigationItem(selected = false, onClick = { }, icon = { Icon(Icons.Default.FlashOn, contentDescription = null) }, label = { Text("Alerts") })
                BottomNavigationItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, contentDescription = null) }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF00BFA5))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = onBrowsersClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5))
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browser Filters", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFF00BFA5),
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Web Monitoring", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MonitoringToggleItem(
                        "Safe search", 
                        "Block Inappropriate content from search results", 
                        safeSearchEnabled,
                        onCheckedChange = { 
                            val previous = safeSearchEnabled
                            safeSearchEnabled = it
                            upsertCategorySetting(
                                "SAFE_SEARCH",
                                it,
                                { safeSearchEnabled = previous },
                                { applied -> safeSearchEnabled = applied }
                            )
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    MonitoringToggleItem(
                        "YouTube Safe search", 
                        "Block Inappropriate content from YouTube", 
                        youtubeSafeSearchEnabled,
                        onCheckedChange = { 
                            val previous = youtubeSafeSearchEnabled
                            youtubeSafeSearchEnabled = it
                            upsertCategorySetting(
                                "YOUTUBE_SAFE_SEARCH",
                                it,
                                { youtubeSafeSearchEnabled = previous },
                                { applied -> youtubeSafeSearchEnabled = applied }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("YouTube", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color.White,
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YouTube Monitoring", fontWeight = FontWeight.Bold)
                        Text("Check what your children watch on youtube", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = youtubeMonitoringEnabled, 
                        onCheckedChange = { 
                            val previous = youtubeMonitoringEnabled
                            youtubeMonitoringEnabled = it
                            upsertCategorySetting(
                                "YOUTUBE_MONITORING",
                                it,
                                { youtubeMonitoringEnabled = previous },
                                { applied -> youtubeMonitoringEnabled = applied }
                            )
                        }, 
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BFA5))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            WebSettingItem("Settings for Categories", "With exclusion", onClick = onBrowsersClick)
        }
    }
}

@Composable
fun MonitoringToggleItem(title: String, subtitle: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        }
        Switch(
            checked = isChecked, 
            onCheckedChange = onCheckedChange, 
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, 
                checkedTrackColor = Color.White.copy(alpha = 0.5f),
                uncheckedThumbColor = Color(0xFFE0E0E0),
                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun WebSettingItem(title: String, subtitle: String, value: String? = null, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            if (value != null) {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
