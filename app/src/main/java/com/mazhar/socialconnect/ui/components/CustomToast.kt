package com.mazhar.socialconnect.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ToastType {
    SUCCESS, INFO, ERROR
}

data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 3000L
)

object CustomToastManager {
    private val _toastFlow = MutableStateFlow<ToastMessage?>(null)
    val toastFlow = _toastFlow.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    fun showToast(message: String, type: ToastType = ToastType.INFO, durationMs: Long = 3000L) {
        scope.launch {
            _toastFlow.value = ToastMessage(message, type, durationMs)
            delay(durationMs)
            if (_toastFlow.value?.message == message) {
                _toastFlow.value = null
            }
        }
    }

    fun dismiss() {
        _toastFlow.value = null
    }

    fun success(message: String, durationMs: Long = 3000L) = showToast(message, ToastType.SUCCESS, durationMs)
    fun info(message: String, durationMs: Long = 3000L) = showToast(message, ToastType.INFO, durationMs)
    fun error(message: String, durationMs: Long = 3000L) = showToast(message, ToastType.ERROR, durationMs)
}

@Composable
fun BoxScope.CustomToastOverlay() {
    val currentToast by CustomToastManager.toastFlow.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(currentToast) {
        visible = currentToast != null
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it - 200 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it - 200 },
            animationSpec = tween(durationMillis = 350)
        ) + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        currentToast?.let { toast ->
            val accentColor = when (toast.type) {
                ToastType.SUCCESS -> Color(0xFFFF6A00) // Premium orange success color matching theme
                ToastType.INFO -> Color(0xFFFF6A00)    // Premium orange accent color matching theme
                ToastType.ERROR -> Color(0xFFE71D36)   // Premium deep red error color
            }

            val icon = when (toast.type) {
                ToastType.SUCCESS -> Icons.Default.CheckCircle
                ToastType.INFO -> Icons.Default.Info
                ToastType.ERROR -> Icons.Default.Error
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { CustomToastManager.dismiss() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Accent indicator color block spanning full height on the left
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(accentColor)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Toast Icon",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = toast.message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (toast.type == ToastType.SUCCESS) Color(0xFFFF6A00) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
