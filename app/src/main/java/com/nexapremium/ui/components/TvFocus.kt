package com.nexapremium.ui.components

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexapremium.core.ui.theme.PureWhite

fun isTelevisionDevice(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

/** A clear outline for controls that can be reached with a TV remote. */
@Composable
fun Modifier.tvFocusOutline(
    interactionSource: InteractionSource,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    return border(
        width = if (focused) 2.dp else 0.dp,
        color = if (focused) PureWhite else Color.Transparent,
        shape = shape
    )
}

@Composable
fun TvIconButton(
    image: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PureWhite,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .size(buttonSize)
            .tvFocusOutline(interactionSource, CircleShape)
    ) {
        Icon(
            imageVector = image,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
