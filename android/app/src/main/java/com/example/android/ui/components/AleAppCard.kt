package com.example.android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.ui.theme.AleAppTheme

/**
 * Карточка CallApp.
 *
 * Из card.tsx:
 *  - bg-card  text-card-foreground  rounded-xl  border
 *  - shadow-sm  (shadowElevation = 1dp)
 *
 * Внутренние отступы и spacing задаются вызывающим кодом.
 */
@Composable
fun AleAppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AleAppTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp), // rounded-xl
        color = colors.card,
        contentColor = colors.cardForeground,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.dp,
    ) {
        Column(content = content)
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Card — Light", showBackground = true)
@Composable
private fun CardLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            AleAppCard(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Информация", style = MaterialTheme.typography.titleMedium)
                    Text("Имя: Александр", style = MaterialTheme.typography.bodyMedium)
                    Text("@alexander", style = MaterialTheme.typography.bodySmall,
                        color = AleAppTheme.colors.mutedForeground)
                }
            }
        }
    }
}

@Preview(name = "Card — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CardDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            AleAppCard(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Информация", style = MaterialTheme.typography.titleMedium)
                    Text("Имя: Александр", style = MaterialTheme.typography.bodyMedium)
                    Text("@alexander", style = MaterialTheme.typography.bodySmall,
                        color = AleAppTheme.colors.mutedForeground)
                }
            }
        }
    }
}

@Preview(name = "Card с кнопками", showBackground = true)
@Composable
private fun CardWithButtonsPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            AleAppCard(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Управление сервером", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Вы уверены, что хотите удалить сервер?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AleAppTheme.colors.mutedForeground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AleAppButton(
                            onClick = {},
                            variant = AleAppButtonVariant.Outline,
                            modifier = Modifier.weight(1f),
                        ) { Text("Отмена") }
                        AleAppButton(
                            onClick = {},
                            variant = AleAppButtonVariant.Destructive,
                            modifier = Modifier.weight(1f),
                        ) { Text("Удалить") }
                    }
                }
            }
        }
    }
}
