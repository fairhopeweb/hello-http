package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun AppImageButton(
    modifier: Modifier = Modifier,
    resource: String,
    size: Dp = 32.dp,
    color: Color = LocalColor.current.image,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var modifierToUse = modifier.size(size)
        .let {
            if (enabled) {
                it.clickable(onClick = onClick)
            } else {
                it
            }
        }

    AppImage(
        resource = resource,
        color = color,
        size = size,
        enabled = enabled,
        modifier = modifierToUse
    )
}