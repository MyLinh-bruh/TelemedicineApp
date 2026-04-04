
    package com.example.telemedicineapp.ui.components

    import androidx.compose.animation.core.*
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.unit.dp

    @Composable
    fun DoctorShimmer() {
        val transition = rememberInfiniteTransition(label = "")
        val alpha = transition.animateFloat(
            initialValue = 0.2f, targetValue = 0.5f,
            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
            label = ""
        )

        Column(modifier = Modifier.padding(16.dp)) {
            repeat(4) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(20.dp)).background(Color.LightGray.copy(alpha = alpha.value)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Box(modifier = Modifier.width(150.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.LightGray.copy(alpha = alpha.value)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.LightGray.copy(alpha = alpha.value)))
                    }
                }
            }
        }
    }
