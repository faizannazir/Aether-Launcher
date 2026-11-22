package com.aether.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.launcher.ui.components.glassEdge

@Composable
internal fun LauncherSearchBar(
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier,
    value: String = "",
    onChange: ((String) -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
    glass: Boolean = false
) {
    val shape = RoundedCornerShape(22.dp)
    val edge = if (glass) glassEdge(shape, darkSurface = background.luminance() < .5f) else Modifier
    val surface = Modifier.fillMaxWidth().height(44.dp).clip(shape).background(background).then(edge)
    val style = TextStyle(color = foreground, fontSize = 14.sp)
    if (onChange == null) {
        Row(
            modifier.then(surface).clickable { onOpen?.invoke() }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = foreground)
            Text("Search apps", style = style)
        }
    } else {
        BasicTextField(
            value, onChange,
            modifier = modifier.then(surface),
            singleLine = true,
            textStyle = style,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
            cursorBrush = SolidColor(foreground),
            decorationBox = { field ->
                Row(
                    Modifier.fillMaxSize().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = foreground)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text("Search apps", style = style.copy(color = foreground.copy(alpha = 0.67f)))
                        field()
                    }
                    if (onClose != null || value.isNotEmpty()) {
                        IconButton(onClick = {
                            if (value.isNotEmpty()) onChange("") else onClose?.invoke()
                        }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Rounded.Close, if (value.isNotEmpty()) "Clear search" else "Close search",
                                Modifier.size(18.dp), tint = foreground)
                        }
                    } else Spacer(Modifier.width(12.dp))
                }
            }
        )
    }
}


@Composable
internal fun SearchProviderActions(onGoogleSearch: () -> Unit, onGemini: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(onClick = onGoogleSearch) { Text("Google") }
        TextButton(onClick = onGemini) { Text("Gemini") }
    }
}
