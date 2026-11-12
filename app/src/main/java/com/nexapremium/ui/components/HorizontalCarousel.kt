package com.nexapremium.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.core.ui.theme.PrimaryRed
import com.nexapremium.core.ui.theme.PureWhite

@Composable
fun HorizontalCarousel(
    title: String,
    items: List<SubjectItemDto>,
    onItemClick: (String, String) -> Unit,
    onSectionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    if (items.isEmpty()) return

    val cardWidth = if (isLandscape) 145.dp else 107.dp
    val aspectRatio = if (isLandscape) 1.6f else 0.71f

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onSectionClick != null) {
                    val sectionInteraction = remember(title) { MutableInteractionSource() }
                    Modifier
                        .clickable(interactionSource = sectionInteraction, indication = null) { onSectionClick(title) }
                        .tvFocusOutline(sectionInteraction, RoundedCornerShape(4.dp))
                } else Modifier)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    color = PrimaryRed,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View All",
                    tint = PrimaryRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items) { item ->
                FocusableCard(
                    imageUrl = item.effectiveCoverUrl,
                    title = item.title ?: "",
                    badgeText = item.badgeText,
                    aspectRatio = aspectRatio,
                    onClick = {
                        val path = item.detailPath
                        if (!path.isNullOrEmpty()) {
                            onItemClick(path, item.effectiveSubjectType)
                        }
                    },
                    modifier = Modifier.width(cardWidth)
                )
            }
        }
    }
}
