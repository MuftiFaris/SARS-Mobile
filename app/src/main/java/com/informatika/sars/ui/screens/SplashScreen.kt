package com.informatika.sars.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatika.sars.R
import com.informatika.sars.ui.theme.DarkBlue
import com.informatika.sars.ui.theme.PrimaryBlue
import com.informatika.sars.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBlue, PrimaryBlue))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Black rounded container with logo inside
            Surface(
                modifier = Modifier
                    .size(280.dp)
                    .padding(20.dp),
                shape = RoundedCornerShape(40.dp),
                color = Color.Black
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.sars_logo),
                        contentDescription = "SARS Logo",
                        modifier = Modifier.size(160.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SARS text
                    Text(
                        text = "SARS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Loading indicator
            CircularProgressIndicator(
                color = SurfaceWhite.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
