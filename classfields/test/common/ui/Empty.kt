package ru.auto.test.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import ru.auto.core_ui.compose.components.CircularProgressIndicator
import ru.auto.core_ui.compose.components.Text
import ru.auto.core_ui.compose.theme.AutoTheme
import ru.auto.core_ui.compose.theme.contentEmphasisMedium
import ru.auto.core_ui.compose.theme.tokens.DimenTokens
import ru.auto.test.R

@Composable
fun Loading(
    containerColor: Color? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .run { if (containerColor != null) background(containerColor) else this },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun Error(
    error: Throwable,
    painter: Painter = painterResource(id = R.drawable.empty_error),
) {
    Empty(
        text = error.run { message ?: toString() },
        painter = painter
    )
}

@Composable
fun Empty(
    text: String,
    painter: Painter = painterResource(id = R.drawable.empty_nothing_found),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoTheme.colorScheme.surface)
            .padding(DimenTokens.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(DimenTokens.x4))
        Text(
            text = text,
            style = AutoTheme.typography.subtitle,
            color = AutoTheme.colorScheme.onSurface.contentEmphasisMedium,
            textAlign = TextAlign.Center
        )
    }
}
