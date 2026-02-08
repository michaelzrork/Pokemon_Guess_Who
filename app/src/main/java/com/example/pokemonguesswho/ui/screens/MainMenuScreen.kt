package com.example.pokemonguesswho.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onMultiplayer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.foundation.background(
                    color = Color(0xFF6200EE)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title
            Text(
                text = "Pokemon",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Guess Who",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFEB3B)
            )
            
            // Spacing
            Box(modifier = Modifier.size(32.dp))
            
            // Start Game Button
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3700B3)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Single Player",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White
                )
            }
            
            // Multiplayer Button
            Button(
                onClick = onMultiplayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF03DAC5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Multiplayer (Coming Soon)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp),
                    color = Color.Black
                )
            }
        }
    }
}
