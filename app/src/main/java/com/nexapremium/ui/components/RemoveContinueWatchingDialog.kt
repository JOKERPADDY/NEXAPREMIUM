package com.nexapremium.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapremium.core.ui.theme.DarkSurface
import com.nexapremium.core.ui.theme.MutedGray
import com.nexapremium.core.ui.theme.PrimaryRed
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.data.db.entity.ContinueWatchingEntity

@Composable
fun RemoveContinueWatchingDialog(
    item: ContinueWatchingEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayTitle = item.title.ifBlank { "this content" }
    val episodeInfo = if (item.seasonNumber > 0 || item.episodeNumber > 0) {
        " (S${item.seasonNumber} E${item.episodeNumber})"
    } else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "Remove item",
                tint = PrimaryRed,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Remove from Continue Watching?",
                color = PureWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Are you sure you want to remove \"$displayTitle$episodeInfo\" from your Continue Watching list?",
                    color = MutedGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Remove", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = PureWhite)
            }
        }
    )
}
