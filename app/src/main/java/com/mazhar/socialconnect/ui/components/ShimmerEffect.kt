package com.mazhar.socialconnect.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mazhar.socialconnect.ui.theme.CardBackground
import com.mazhar.socialconnect.ui.theme.PrimaryPurple

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )
    )
}

@Composable
fun PostCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).shimmerEffect())
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
        }
    }
}

@Composable
fun ProfileHeaderSkeleton() {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(PrimaryPurple)
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .width(80.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .shimmerEffect()
            )
        }

        // Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 110.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(45.dp))
                Box(modifier = Modifier.width(150.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.width(200.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats Skeleton
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.width(40.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
            }
        }

        // Profile Image Skeleton
        Box(
            modifier = Modifier
                .padding(top = 60.dp)
                .size(100.dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(Color.LightGray)
                .shimmerEffect()
        )
    }
}

@Composable
fun HomeHeaderSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(PrimaryPurple)
            .padding(24.dp)
            .padding(top = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Box(modifier = Modifier.width(60.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f)).shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.width(120.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.3f)).shimmerEffect())
            }
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).shimmerEffect())
        }
    }
}

@Composable
fun FeedHeaderSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(100.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Box(modifier = Modifier.width(80.dp).height(36.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
    }
}
