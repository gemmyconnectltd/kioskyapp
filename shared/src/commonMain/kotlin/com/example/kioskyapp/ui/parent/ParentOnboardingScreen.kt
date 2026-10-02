package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import com.example.kioskyapp.shared.generated.resources.Res
import com.example.kioskyapp.shared.generated.resources.customiseicon
import com.example.kioskyapp.shared.generated.resources.safeicon
import com.example.kioskyapp.shared.generated.resources.locationicon

@Composable
fun ParentOnboardingScreen(onFinish: () -> Unit, onSkip: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    
    val pages = listOf(
        OnboardingPageData(
            image = Res.drawable.customiseicon,
            title = "Customize Your Child's Online Experience",
            description = "Our Content Filtering Options Put You in Control What you Kids access."
        ),
        OnboardingPageData(
            image = Res.drawable.safeicon,
            title = "Safe and Secure Browsing",
            description = "Safe Search Filters Out Explicit Content from Search Results."
        ),
        OnboardingPageData(
            image = Res.drawable.locationicon,
            title = "Know Where Your Child Is",
            description = "Geolocation Functionality Keeps you updated with your Child Location in Real-Time"
        )
    )

    val currentPageData = pages[currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00BFA5))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Image Section
        Image(
            painter = painterResource(currentPageData.image),
            contentDescription = null,
            modifier = Modifier
                .size(250.dp),
            contentScale = ContentScale.Fit
        )

        // Text Section
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentPageData.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentPageData.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        // Indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == currentPage) 10.dp else 8.dp)
                        .background(
                            color = if (index == currentPage) Color.White else Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        }

        // Buttons Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, contentColor = Color.White)
            ) {
                Text("Skip", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
            ) {
                Text(
                    text = if (currentPage < pages.size - 1) "Next" else "Finish",
                    color = Color(0xFF00BFA5),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class OnboardingPageData(
    val image: org.jetbrains.compose.resources.DrawableResource,
    val title: String,
    val description: String
)
