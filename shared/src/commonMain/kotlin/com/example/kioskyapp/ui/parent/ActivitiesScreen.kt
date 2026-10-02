package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.AppManagementApi
import com.example.kioskyapp.apiServices.SearchLogApi
import com.example.kioskyapp.apiServices.SearchLogResponse
import com.example.kioskyapp.apiServices.ScreenTimeApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.AppUsageSummaryDto
import com.example.kioskyapp.models.AppUsageTopAppDto
import com.example.kioskyapp.models.ScreenTimeUsageResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class ActivityPeriod(val label: String, val apiValue: String) {
    DAY("Day", "day"),
    WEEK("Week", "week"),
    MONTH("Month", "month")
}

private data class ScreenTimeSummary(
    val totalMinutes: Int = 0,
    val averageMinutes: Int = 0,
    val breakdown: List<Pair<String, Int>> = emptyList()
)

@Composable
fun ActivitiesScreen(
    token: String,
    childId: String?,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSetScreenTime: () -> Unit
) {
    val screenTimeApi = remember { ScreenTimeApi() }
    val appManagementApi = remember { AppManagementApi() }
    val searchLogApi = remember { SearchLogApi() }

    var selectedPeriod by remember { mutableStateOf(ActivityPeriod.DAY) }
    var screenTimeSummary by remember { mutableStateOf(ScreenTimeSummary()) }
    var appUsageSummary by remember {
        mutableStateOf(
            AppUsageSummaryDto(
                period = ActivityPeriod.DAY.apiValue,
                total_minutes = 0,
                total_apps_used = 0,
                top_apps = emptyList(),
                daily_breakdown = emptyList()
            )
        )
    }
    var searchLogs by remember { mutableStateOf<List<SearchLogResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token, childId, selectedPeriod) {
        if (childId.isNullOrBlank()) {
            errorMessage = "No child selected"
            screenTimeSummary = ScreenTimeSummary()
            searchLogs = emptyList()
            appUsageSummary = appUsageSummary.copy(
                period = selectedPeriod.apiValue,
                total_minutes = 0,
                total_apps_used = 0,
                top_apps = emptyList(),
                daily_breakdown = emptyList()
            )
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        when (val screenResult = screenTimeApi.getUsageHistory(token, childId)) {
            is Result.Success -> {
                screenTimeSummary = buildScreenTimeSummary(screenResult.data, selectedPeriod)
            }
            is Result.Error -> {
                screenTimeSummary = ScreenTimeSummary()
                errorMessage = screenResult.exception.message ?: "Failed to load screen time activity"
            }
        }

        when (val appResult = appManagementApi.getAppUsageSummary(token, childId, selectedPeriod.apiValue)) {
            is Result.Success -> {
                appUsageSummary = appResult.data
            }
            is Result.Error -> {
                appUsageSummary = AppUsageSummaryDto(
                    period = selectedPeriod.apiValue,
                    total_minutes = 0,
                    total_apps_used = 0,
                    top_apps = emptyList(),
                    daily_breakdown = emptyList()
                )
                if (errorMessage == null) {
                    errorMessage = appResult.exception.message ?: "Failed to load app activity"
                }
            }
        }

        when (val searchResult = searchLogApi.getSearchLogsByChild(token, childId)) {
            is Result.Success -> {
                searchLogs = searchResult.data
            }
            is Result.Error -> {
                searchLogs = emptyList()
                if (errorMessage == null) {
                    errorMessage = searchResult.exception.message ?: "Failed to load search activity"
                }
            }
        }

        isLoading = false
    }

    val filteredSearches = remember(searchLogs, selectedPeriod) {
        searchLogs
            .filter { isInSelectedPeriod(it.recorded_at, selectedPeriod) }
            .sortedByDescending { it.recorded_at }
            .take(12)
    }

    val topApp = appUsageSummary.top_apps.firstOrNull()
    val appBreakdown = remember(appUsageSummary) {
        appUsageSummary.daily_breakdown
            .sortedBy { it.date }
            .map { shortDateLabel(it.date) to it.total_minutes }
    }

    Scaffold(
        backgroundColor = Color(0xFFF5F9FF),
        topBar = {
            TopAppBar(
                title = { Text("Activities & Monitoring", color = Color.Black, fontWeight = FontWeight.Bold) },
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
                    icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                    label = { Text("Home") }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onMapClick,
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Map") }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onAlertsClick,
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                    label = { Text("Alerts") }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00BFA5))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F9FF)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Button(
                            onClick = onSetScreenTime,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5))
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Screen Time", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (errorMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = Color(0xFFFFF3E0),
                                elevation = 0.dp
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFB26A00),
                                    modifier = Modifier.padding(14.dp),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        PeriodSelector(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { selectedPeriod = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Screen time",
                                value = formatMinutes(screenTimeSummary.totalMinutes),
                                subtitle = when (selectedPeriod) {
                                    ActivityPeriod.DAY -> "Used today"
                                    ActivityPeriod.WEEK -> "Last 7 days"
                                    ActivityPeriod.MONTH -> "Last 30 days"
                                },
                                icon = Icons.Default.Timer
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Apps used",
                                value = appUsageSummary.total_apps_used.toString(),
                                subtitle = "Apps opened",
                                icon = Icons.Default.Apps
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Daily average",
                                value = formatMinutes(screenTimeSummary.averageMinutes),
                                subtitle = "Average usage",
                                icon = Icons.Default.HourglassBottom
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Top app",
                                value = topApp?.app_name ?: "None yet",
                                subtitle = topApp?.let { formatMinutes(it.total_minutes) } ?: "No app activity",
                                icon = Icons.Default.BarChart,
                                valueFontSize = if (topApp == null) 18.sp else 16.sp
                            )
                        }
                    }
                }

                item {
                    SectionHeader("Screen Time Trend", Icons.Default.Timer, Modifier.padding(horizontal = 16.dp))
                }

                item {
                    BreakdownCard(
                        items = screenTimeSummary.breakdown,
                        emptyMessage = "No screen time recorded for this period.",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    SectionHeader("Most Used Apps", Icons.Default.Apps, Modifier.padding(horizontal = 16.dp))
                }

                if (appUsageSummary.top_apps.isEmpty()) {
                    item {
                        EmptyStateCard(
                            message = "No app activity has been synced yet.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(appUsageSummary.top_apps) { app ->
                        AppUsageCard(
                            app = app,
                            maxMinutes = appUsageSummary.top_apps.maxOfOrNull { it.total_minutes } ?: 1,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item {
                    SectionHeader("App Activity Trend", Icons.Default.BarChart, Modifier.padding(horizontal = 16.dp))
                }

                item {
                    BreakdownCard(
                        items = appBreakdown,
                        emptyMessage = "No app activity recorded for this period.",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    SectionHeader("Recent Searches", Icons.Default.Search, Modifier.padding(horizontal = 16.dp))
                }

                if (filteredSearches.isEmpty()) {
                    item {
                        EmptyStateCard(
                            message = "No searches were recorded for this period.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(filteredSearches) { log ->
                        SearchLogItem(log = log, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: ActivityPeriod,
    onPeriodSelected: (ActivityPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityPeriod.values().forEach { period ->
            val selected = period == selectedPeriod
            TextButton(
                onClick = { onPeriodSelected(period) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0xFF00BFA5) else Color.Transparent)
            ) {
                Text(
                    period.label,
                    color = if (selected) Color.White else Color(0xFF607D8B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    valueFontSize: androidx.compose.ui.unit.TextUnit = 22.sp
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F7F4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00BFA5))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color(0xFF607D8B), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = valueFontSize, color = Color(0xFF263238))
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BreakdownCard(
    items: List<Pair<String, Int>>,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        if (items.isEmpty()) {
            Text(
                text = emptyMessage,
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val maxMinutes = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                items.forEach { (label, minutes) ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontWeight = FontWeight.Medium, color = Color(0xFF37474F))
                            Text(formatMinutes(minutes), color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = (minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            color = Color(0xFF00BFA5),
                            backgroundColor = Color(0xFFE0F2F1)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    app: AppUsageTopAppDto,
    maxMinutes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F7F4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = Color(0xFF00BFA5))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(app.app_name, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    Text(formatMinutes(app.total_minutes), color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (app.total_minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = Color(0xFF00BFA5),
                    backgroundColor = Color(0xFFE0F2F1)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(app.package_name, color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF00BFA5), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
    }
}

@Composable
private fun EmptyStateCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            Text(message, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SearchLogItem(log: SearchLogResponse, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (log.engine == "youtube") Color(0xFFFFEBEE) else Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.engine == "youtube") Icons.Default.BarChart else Icons.Default.Search,
                    contentDescription = null,
                    tint = if (log.engine == "youtube") Color.Red else Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.query,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = "${log.engine.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} Search • ${formatDate(log.recorded_at)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun buildScreenTimeSummary(
    history: List<ScreenTimeUsageResponse>,
    period: ActivityPeriod
): ScreenTimeSummary {
    val filtered = history
        .filter { isInSelectedPeriod(it.usage_date, period) }
        .sortedBy { it.usage_date }

    if (filtered.isEmpty()) {
        return ScreenTimeSummary()
    }

    val grouped = linkedMapOf<String, Int>()
    filtered.forEach { item ->
        val dateKey = item.usage_date.take(10)
        grouped[dateKey] = (grouped[dateKey] ?: 0) + item.total_minutes
    }

    val total = grouped.values.sum()
    val average = if (grouped.isNotEmpty()) total / grouped.size else 0

    return ScreenTimeSummary(
        totalMinutes = total,
        averageMinutes = average,
        breakdown = grouped.map { (date, minutes) ->
            shortDateLabel(date) to minutes
        }
    )
}

private fun isInSelectedPeriod(dateText: String, period: ActivityPeriod): Boolean {
    val date = try {
        LocalDate.parse(dateText.take(10))
    } catch (_: Exception) {
        return false
    }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val dayDifference = today.toEpochDays() - date.toEpochDays()

    return when (period) {
        ActivityPeriod.DAY -> dayDifference == 0
        ActivityPeriod.WEEK -> dayDifference in 0..6
        ActivityPeriod.MONTH -> dayDifference in 0..29
    }
}

private fun shortDateLabel(dateText: String): String {
    return try {
        val date = LocalDate.parse(dateText.take(10))
        "${date.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)} ${date.dayOfMonth}"
    } catch (_: Exception) {
        dateText.take(10)
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        totalMinutes <= 0 -> "0m"
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val datePart = isoDate.split("T")[0]
        val timePart = isoDate.split("T").getOrNull(1)?.substring(0, 5) ?: ""
        if (timePart.isBlank()) datePart else "$datePart $timePart"
    } catch (_: Exception) {
        isoDate.take(16)
    }
}
