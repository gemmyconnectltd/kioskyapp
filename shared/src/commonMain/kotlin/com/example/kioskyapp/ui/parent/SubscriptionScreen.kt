package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0F2F1))) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF00BFA5))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE0F2F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF00BFA5), modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Subscribe To Pro", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "To access all functionality of kioskyApp you have to subscribe to pro, you can access a whole lot more features.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Choose a plan for Yourself", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Medium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SubscriptionPlanItem("Pro", "$2.99/month  ($31/Year)")
            Spacer(modifier = Modifier.height(12.dp))
            SubscriptionPlanItem("Plus", "$4.99/month  ($51/Year)")
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { /* Handle subscription */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5))
            ) {
                Text("Subscribe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun SubscriptionPlanItem(title: String, price: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBDEFB)),
        elevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFD1E4F5)).padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(price, fontSize = 16.sp)
            }
        }
    }
}
