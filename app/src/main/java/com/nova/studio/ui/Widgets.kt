package com.nova.studio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    color: Color = NovaColors.accent
) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) color else NovaColors.control)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            color = if (selected) NovaColors.text else NovaColors.dim,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
        Box(
            Modifier.padding(top = 3.dp).height(2.dp).width(24.dp)
                .background(if (selected) NovaColors.accent else Color.Transparent)
        )
    }
}

@Composable
fun HDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(NovaColors.border))
}

@Composable
fun VDivider() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(NovaColors.border))
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = NovaColors.dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 3.dp)
    )
}

@Composable
fun MiniSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var w by remember { mutableFloatStateOf(1f) }
    val cb by rememberUpdatedState(onChange)
    val span = range.endInclusive - range.start
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    Box(
        modifier
            .height(22.dp)
            .onSizeChanged { w = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(range) {
                detectTapGestures { o -> cb(range.start + (o.x / w).coerceIn(0f, 1f) * span) }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    cb(range.start + (change.position.x / w).coerceIn(0f, 1f) * span)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cy = size.height / 2f
            val th = 4.dp.toPx()
            drawRoundRect(NovaColors.control, Offset(0f, cy - th / 2), Size(size.width, th), CornerRadius(th / 2))
            drawRoundRect(NovaColors.accent, Offset(0f, cy - th / 2), Size(size.width * frac, th), CornerRadius(th / 2))
            drawCircle(Color.White, 6.dp.toPx(), Offset(size.width * frac, cy))
        }
    }
}

@Composable
fun PropSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = NovaColors.dim, fontSize = 10.sp)
            Text("%.2f".format(value), color = NovaColors.text, fontSize = 10.sp)
        }
        MiniSlider(value, range, onChange, Modifier.fillMaxWidth())
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
                .background(if (checked) NovaColors.accent else NovaColors.control),
            contentAlignment = Alignment.Center
        ) { if (checked) Text("✓", color = Color.White, fontSize = 10.sp) }
        Spacer(Modifier.width(8.dp))
        Text(label, color = NovaColors.text, fontSize = 11.sp)
    }
}

@Composable
fun MiniTextField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = NovaColors.text, fontSize = 12.sp),
        cursorBrush = SolidColor(NovaColors.accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(NovaColors.bg)
            .padding(horizontal = 6.dp, vertical = 5.dp)
    )
}
