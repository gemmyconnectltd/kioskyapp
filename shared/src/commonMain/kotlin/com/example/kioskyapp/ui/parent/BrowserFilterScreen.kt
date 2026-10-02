package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.BackHandler
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.kioskyapp.models.*
import com.example.kioskyapp.apiServices.WebFilterApi
import com.example.kioskyapp.data.Result
import kotlinx.coroutines.launch

@Composable
fun BrowserFilterScreen(
    token: String,
    childId: String,
    parentId: String,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    BackHandler(onBack = onBack)

    var urlInput by remember { mutableStateOf("") }
    var showingForbiddenTab by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val filterItems: SnapshotStateList<ContentFilterDTO> = remember { mutableStateListOf() }
    val api: WebFilterApi = remember { WebFilterApi() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(childId) {
        isLoading = true
        val result: Result<List<ContentFilterDTO>> = api.getContentFilters(token, childId)
        isLoading = false
        if (result is Result.Success) {
            filterItems.clear()
            filterItems.addAll(result.data)
        } else if (result is Result.Error) {
            errorMessage = result.exception.message
        }
    }

    val onAddFilter: () -> Unit = {
        val normalizedValue = normalizeWebsiteFilterInput(urlInput)
        if (normalizedValue == null) {
            errorMessage = "Enter a website like livescore.com or a keyword like bet"
        } else {
            errorMessage = null
            scope.launch {
                isLoading = true
                val filterType = inferWebsiteFilterType(normalizedValue)
                val request = CreateContentFilterRequest(
                    parent_id = parentId,
                    child_id = childId,
                    filter_type = filterType,
                    value = normalizedValue,
                    is_blocked = showingForbiddenTab
                )
                val result = api.createContentFilter(token, request)
                isLoading = false
                if (result is Result.Success) {
                    val existingIndex = filterItems.indexOfFirst { it.id == result.data.id }
                    if (existingIndex >= 0) {
                        filterItems[existingIndex] = result.data
                    } else {
                        filterItems.add(0, result.data)
                    }
                    urlInput = ""
                } else if (result is Result.Error) {
                    errorMessage = result.exception.message
                }
            }
        }
    }

    val onDeleteFilter: (String) -> Unit = { filterId ->
        scope.launch {
            val result: Result<Boolean> = api.deleteContentFilter(token, filterId)
            if (result is Result.Success) {
                filterItems.removeAll { it.id == filterId }
            }
        }
    }

    val onToggleFilter: (ContentFilterDTO) -> Unit = { filter ->
        scope.launch {
            val request = UpdateContentFilterRequest(is_blocked = !filter.is_blocked)
            val result: Result<ContentFilterDTO> = api.updateContentFilter(token, filter.id!!, request)
            if (result is Result.Success) {
                val index = filterItems.indexOfFirst { it.id == filter.id }
                if (index != -1) {
                    filterItems[index] = result.data
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browsers", color = Color.Black, fontWeight = FontWeight.Bold) },
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
                BottomNavigationItem(
                    selected = false,
                    onClick = onHomeClick,
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                BottomNavigationItem(selected = false, onClick = onMapClick, icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") }, label = { Text("Map") })
                BottomNavigationItem(selected = false, onClick = onAlertsClick, icon = { Icon(Icons.Default.FlashOn, contentDescription = "Alerts") }, label = { Text("Alerts") })
                BottomNavigationItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Color(0xFF00BFA5))
            }
            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusCard("Allowed", Color(0xFF00BFA5), !showingForbiddenTab, onClick = { showingForbiddenTab = false })
                StatusCard("Forbidden", Color(0xFFF44336), showingForbiddenTab, onClick = { showingForbiddenTab = true })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Add Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (showingForbiddenTab) "Block Website Or Keyword" else "Allow Website Or Keyword",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Use a clean domain like livescore.com, or a keyword like bet to match domains consistently.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("livescore.com or bet") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = if (showingForbiddenTab) Color.Red else Color(0xFF00BFA5)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onAddFilter,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (showingForbiddenTab) Color.Red else Color(0xFF00BFA5)),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                if (showingForbiddenTab) "Forbidden Websites" else "Allowed Websites",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val itemsToShow = filterItems.filter {
                    it.is_blocked == showingForbiddenTab &&
                        (it.filter_type == FilterType.WEBSITE || it.filter_type == FilterType.KEYWORD)
                }
                items(itemsToShow) { item ->
                    WebsiteFilterItem(
                        item = item,
                        onToggleStatus = { onToggleFilter(item) },
                        onDelete = { onDeleteFilter(item.id!!) }
                    )
                }
            }
            
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5))
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}


private fun normalizeWebsiteFilterInput(rawValue: String): String? {
    var normalized = rawValue.trim().lowercase()
    if (normalized.isBlank()) return null

    normalized = normalized.removePrefix("view-source:")
    normalized = normalized.substringAfter("://", normalized)
    normalized = normalized.substringAfter("@", normalized)
    normalized = normalized.substringBefore('/')
    normalized = normalized.substringBefore('?')
    normalized = normalized.substringBefore('#')
    normalized = normalized.substringBefore(':')
    normalized = normalized.removePrefix("www.")
    normalized = normalized.removePrefix("*.")
    normalized = normalized.trim('.')

    return normalized.takeIf { it.isNotBlank() }
}

private fun inferWebsiteFilterType(value: String): FilterType {
    return if (value.contains('.')) FilterType.WEBSITE else FilterType.KEYWORD
}

@Composable
fun RowScope.StatusCard(title: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (isSelected) color else Color.LightGray, RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.05f) else Color.White)
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Medium, color = if (isSelected) color else Color.Gray)
        }
    }
}

@Composable
fun WebsiteFilterItem(item: ContentFilterDTO, onToggleStatus: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        backgroundColor = Color(0xFFF5F9FF)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (item.is_blocked) Color.Red else Color(0xFF00BFA5))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                normalizeWebsiteFilterInput(item.value) ?: item.value,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(onClick = onToggleStatus) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Change Status",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
