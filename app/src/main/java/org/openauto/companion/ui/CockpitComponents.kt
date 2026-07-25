package org.openauto.companion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openauto.companion.ui.theme.CockpitBlue
import org.openauto.companion.ui.theme.CockpitBlueText
import org.openauto.companion.ui.theme.CockpitMuted
import org.openauto.companion.ui.theme.CockpitOutline
import org.openauto.companion.ui.theme.CockpitSurface
import org.openauto.companion.ui.theme.CockpitSurfaceRaised
import org.openauto.companion.ui.theme.CockpitWarning
import java.util.Locale

@Composable
fun CockpitTopBar(
    title: String,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = backEnabled, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun ProdigySectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        color = CockpitMuted,
        modifier = modifier,
    )
}

@Composable
fun CockpitCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = if (highlighted) CockpitSurfaceRaised else CockpitSurface
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = color,
            border = BorderStroke(1.dp, CockpitOutline),
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = color,
            border = BorderStroke(1.dp, CockpitOutline),
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}

@Composable
fun ConnectionDot(
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(12.dp)
            .semantics { this.contentDescription = contentDescription },
        color = color,
        shape = CircleShape,
        content = {},
    )
}

@Composable
fun CockpitTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = CockpitBlueText,
        ),
        content = content,
    )
}

@Composable
fun toneColor(tone: UiTone): Color = when (tone) {
    UiTone.Positive -> CockpitBlue
    UiTone.Warning -> CockpitWarning
    UiTone.Neutral -> CockpitMuted
}

@Composable
fun toneTextColor(tone: UiTone): Color = when (tone) {
    UiTone.Positive -> CockpitBlueText
    UiTone.Warning -> CockpitWarning
    UiTone.Neutral -> CockpitMuted
}
