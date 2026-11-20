package com.nova.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.studio.EditorState
import com.nova.studio.LogLevel

@Composable
fun TerminalPane(state: EditorState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NovaColors.text)

    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) listState.scrollToItem(state.logs.lastIndex)
    }

    Column(modifier.background(NovaColors.panelDark)) {
        HDivider()
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            state = listState
        ) {
            items(state.logs) { l ->
                val c = when (l.level) {
                    LogLevel.INFO -> NovaColors.text
                    LogLevel.WARN -> NovaColors.yellow
                    LogLevel.ERROR -> NovaColors.red
                    LogLevel.CMD -> NovaColors.green
                }
                Text("${l.time}  ${l.text}", style = mono.copy(color = c))
            }
        }
        Row(
            Modifier.fillMaxWidth().background(NovaColors.bg).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", style = mono.copy(color = NovaColors.green))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = mono,
                cursorBrush = SolidColor(NovaColors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    state.runCommand(input)
                    input = ""
                }),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
