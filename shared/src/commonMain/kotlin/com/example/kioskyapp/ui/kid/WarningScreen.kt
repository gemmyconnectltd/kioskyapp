package com.example.kioskyapp.ui.kid

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.shared.generated.resources.Res
import com.example.kioskyapp.shared.generated.resources.mobiletab
import com.example.kioskyapp.shared.generated.resources.sickface
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun WarningScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD1D5F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF008080))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Warning",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You have no access to this feature. Consult your parent first",
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.mobiletab),
                    contentDescription = "Mobile Frame",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .size(400.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.sickface),
                        contentDescription = "Sick Face",
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}
