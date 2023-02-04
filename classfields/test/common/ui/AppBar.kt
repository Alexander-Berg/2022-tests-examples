package ru.auto.test.common.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import ru.auto.core_ui.compose.components.TopAppBarDefaults
import ru.auto.core_ui.compose.theme.tokens.ElevationTokens
import ru.auto.core_ui.compose.components.CenterAlignedTopAppBar as AutoCenterAlignedTopAppBar

@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val elevationModifier = if (scrollBehavior != null) {
        val scrollFraction = scrollBehavior.scrollFraction
        val containerColor by colors.containerColor(scrollFraction = scrollBehavior.scrollFraction)
        val elevation by animateDpAsState(
            // Check if scrollFraction is slightly over zero to overcome float precision issues.
            targetValue = if (scrollFraction > 0.01f) ElevationTokens.Level2 else ElevationTokens.Level0,
            animationSpec = tween(
                durationMillis = 500,
                easing = LinearOutSlowInEasing
            )
        )
        Modifier
            .shadow(elevation)
            .background(containerColor)
    } else {
        Modifier.background(colors.containerColor(scrollFraction = 0f).value)
    }

    AutoCenterAlignedTopAppBar(
        modifier = elevationModifier.then(modifier),
        colors = colors,
        scrollBehavior = scrollBehavior,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions
    )
}
