package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.ScreenTimeApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.ScreenTimeRuleRequest
import kotlinx.coroutines.launch

@Composable
fun ScreenTimeScreen(
    token: String,
    parentId: String,
    childId: String,
    onBack: () -> Unit
) {
    var hours by remember { mutableStateOf(1) }
    var minutes by remember { mutableStateOf(30) }
    var unlockHours by remember { mutableStateOf(0) }
    var unlockMinutes by remember { mutableStateOf(30) }
    var notifyEnabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeRuleId by remember { mutableStateOf<String?>(null) }
    var usedTodayMinutes by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val screenTimeApi = remember { ScreenTimeApi() }

    suspend fun saveRuleAndRefresh(stayOnScreenAfterSave: Boolean = false) {
        isLoading = true
        errorMessage = null

        val request = ScreenTimeRuleRequest(
            parent_id = parentId,
            child_id = childId,
            daily_limit_min = (hours * 60) + minutes,
            unlock_after_min = (unlockHours * 60) + unlockMinutes
        )
        val result = screenTimeApi.saveScreenTimeRule(token, request)

        if (result is Result.Success) {
            activeRuleId = result.data.id
            usedTodayMinutes = 0
            if (!stayOnScreenAfterSave) {
                onBack()
            }
        } else if (result is Result.Error) {
            errorMessage = result.exception.message ?: "Failed to save rule"
        }

        isLoading = false
    }

    LaunchedEffect(childId) {
        val result = screenTimeApi.getActiveRuleForChild(token, childId)
        if (result is Result.Success && result.data.isNotEmpty()) {
            val rule = result.data.first()
            activeRuleId = rule.id
            hours = rule.daily_limit_min / 60
            minutes = rule.daily_limit_min % 60
            val unlockTotal = rule.unlock_after_min ?: 30
            unlockHours = unlockTotal / 60
            unlockMinutes = unlockTotal % 60
        } else {
            activeRuleId = null
            hours = 0
            minutes = 0
            unlockHours = 0
            unlockMinutes = 30
        }

        val usage = screenTimeApi.getTodayUsageMinutes(token, childId)
        if (usage is Result.Success) {
            usedTodayMinutes = usage.data
        }
    }

    val totalMinutes = (hours * 60) + minutes
    val totalUnlockMinutes = (unlockHours * 60) + unlockMinutes
    val remainingMinutes = (totalMinutes - usedTodayMinutes).coerceAtLeast(0)
    val remainingH = remainingMinutes / 60
    val remainingM = remainingMinutes % 60
    val limitSummary = if (totalMinutes > 0) "${hours}h ${minutes}m" else "Unlimited time"
    val remainingSummary = if (totalMinutes > 0) "${remainingH}h ${remainingM}m" else "Unlimited"
    val unlockSummary = if (totalUnlockMinutes > 0) {
        "${unlockHours}h ${unlockMinutes}m"
    } else {
        "Until parent resets"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duration", color = Color.Black, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back", tint = Color.Black)
                    }
                },
                backgroundColor = Color.White,
                elevation = 0.dp
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp,
                backgroundColor = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Today's limit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = limitSummary,
                        color = Color(0xFF00BFA5),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Time remaining today", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = remainingSummary,
                        color = if (remainingMinutes == 0 && totalMinutes > 0) Color.Red else Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Auto unlock after", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = unlockSummary,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (totalMinutes > 0) "Used today: ${usedTodayMinutes} min" else "No screen time limit is active",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Phone usage time", color = Color(0xFFB2DFDB), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "How long the child can use the phone before kioskyApp locks it.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Text("hours", color = Color(0xFFB2DFDB), fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { if (hours > 0) hours-- }) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Decrease hours", tint = Color.LightGray)
                }
                Text(hours.toString().padStart(2, '0'), color = Color(0xFF00BFA5), fontSize = 80.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (hours < 23) hours++ }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase hours", tint = Color.LightGray)
                }
            }

            Text("minutes", color = Color(0xFFB2DFDB), fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { if (minutes >= 5) minutes -= 5 else if (minutes == 0) minutes = 55 }) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Decrease minutes", tint = Color.LightGray)
                }
                Text(minutes.toString().padStart(2, '0'), color = Color(0xFF00BFA5), fontSize = 80.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (minutes < 55) minutes += 5 else minutes = 0 }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase minutes", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Divider(color = Color(0xFFE5ECEC), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Lock duration", color = Color(0xFFB2DFDB), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "When the limit is reached, keep the phone locked for this long, then unlock it automatically with a fresh timer.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Text("hours", color = Color(0xFFB2DFDB), fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { if (unlockHours > 0) unlockHours-- }) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Decrease unlock hours", tint = Color.LightGray)
                }
                Text(unlockHours.toString().padStart(2, '0'), color = Color(0xFF00695C), fontSize = 72.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (unlockHours < 23) unlockHours++ }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase unlock hours", tint = Color.LightGray)
                }
            }

            Text("minutes", color = Color(0xFFB2DFDB), fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { if (unlockMinutes >= 5) unlockMinutes -= 5 else if (unlockMinutes == 0) unlockMinutes = 55 }) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Decrease unlock minutes", tint = Color.LightGray)
                }
                Text(unlockMinutes.toString().padStart(2, '0'), color = Color(0xFF00695C), fontSize = 72.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (unlockMinutes < 55) unlockMinutes += 5 else unlockMinutes = 0 }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase unlock minutes", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp,
                backgroundColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Notify if hours exceed", fontSize = 14.sp)
                    Switch(
                        checked = notifyEnabled,
                        onCheckedChange = { notifyEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BFA5))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Saving starts the phone timer fresh from now. When time is up, the phone will lock for the selected lock duration and then unlock automatically.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { scope.launch { saveRuleAndRefresh() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { scope.launch { saveRuleAndRefresh(stayOnScreenAfterSave = true) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading && totalMinutes > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00BFA5))
            ) {
                Text("Reset Today's Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null

                        val currentRuleId = activeRuleId
                        if (currentRuleId != null) {
                            val deactivateResult = screenTimeApi.deactivateScreenTimeRule(token, currentRuleId)
                            if (deactivateResult is Result.Error) {
                                errorMessage = deactivateResult.exception.message ?: "Failed to set unlimited time"
                                isLoading = false
                                return@launch
                            }
                        }

                        activeRuleId = null
                        hours = 0
                        minutes = 0
                        unlockHours = 0
                        unlockMinutes = 30
                        isLoading = false
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF008080))
            ) {
                Text("Set Unlimited Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
