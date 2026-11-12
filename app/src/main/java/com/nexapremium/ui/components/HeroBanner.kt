package com.nexapremium.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.data.api.model.SubjectItemDto
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBanner(
    banners: List<SubjectItemDto>,
    onItemClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTelevision = (configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val featuredBanners = banners.take(4)
    if (featuredBanners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { featuredBanners.size })

    LaunchedEffect(featuredBanners.size, isTelevision) {
        while (featuredBanners.size > 1 && !isTelevision) {
            delay(3_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % featuredBanners.size)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(PureBlack)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val banner = featuredBanners[page]
            val imageUrl = banner.backdropUrl ?: banner.effectiveCoverUrl
            val bannerInteraction = remember(page) { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = bannerInteraction,
                        indication = null
                    ) {
                        val path = banner.detailPath
                        if (!path.isNullOrEmpty()) {
                            onItemClick(path, banner.effectiveSubjectType)
                        }
                    }
                    .tvFocusOutline(bannerInteraction, RoundedCornerShape(8.dp))
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = banner.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ShimmerPlaceholder(modifier = Modifier.fillMaxSize()) }
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, PureBlack.copy(alpha = 0.8f)),
                                startY = 100f
                            )
                        )
                )

                // Title and info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = banner.title ?: "",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Pagination dots
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(featuredBanners.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PureWhite else PureWhite.copy(alpha = 0.4f))
                )
            }
        }
    }
}
