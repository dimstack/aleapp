package com.example.android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.ui.theme.AleAppTheme

enum class AleAppButtonVariant { Primary, Secondary, Outline, Destructive, Ghost }
enum class AleAppButtonSize { Small, Default, Large }

/**
 * Кнопка CallApp.
 *
 * Варианты (из button.tsx):
 *  - Primary:     bg-primary  text-primary-foreground
 *  - Secondary:   bg-secondary text-secondary-foreground
 *  - Outline:     border bg-background text-foreground
 *  - Destructive: bg-destructive text-destructive-foreground
 *  - Ghost:       прозрачный фон, text-foreground
 *
 * Размеры: Small h-8 / Default h-9 / Large h-10
 */
@Composable
fun AleAppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AleAppButtonVariant = AleAppButtonVariant.Primary,
    size: AleAppButtonSize = AleAppButtonSize.Default,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AleAppTheme.colors
    val shape = RoundedCornerShape(10.dp) // --radius-md ≈ 10px

    val height = when (size) {
        AleAppButtonSize.Small -> 32.dp    // h-8
        AleAppButtonSize.Default -> 36.dp  // h-9
        AleAppButtonSize.Large -> 40.dp    // h-10
    }

    val contentPadding = when (size) {
        AleAppButtonSize.Small -> PaddingValues(horizontal = 12.dp)    // px-3
        AleAppButtonSize.Default -> PaddingValues(horizontal = 16.dp)  // px-4
        AleAppButtonSize.Large -> PaddingValues(horizontal = 24.dp)    // px-6
    }

    @Suppress("NAME_SHADOWING")
    val modifier = modifier.defaultMinSize(minWidth = 1.dp, minHeight = height)

    when (variant) {
        AleAppButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.primaryForeground,
                disabledContainerColor = colors.primary.copy(alpha = 0.5f),
                disabledContentColor = colors.primaryForeground.copy(alpha = 0.5f),
            ),
            elevation = null,
            contentPadding = contentPadding,
            content = content,
        )

        AleAppButtonVariant.Secondary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.secondary,
                contentColor = colors.secondaryForeground,
                disabledContainerColor = colors.secondary.copy(alpha = 0.5f),
                disabledContentColor = colors.secondaryForeground.copy(alpha = 0.5f),
            ),
            elevation = null,
            contentPadding = contentPadding,
            content = content,
        )

        AleAppButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colors.background,
                contentColor = colors.foreground,
                disabledContentColor = colors.foreground.copy(alpha = 0.5f),
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) colors.border else colors.border.copy(alpha = 0.5f),
            ),
            contentPadding = contentPadding,
            content = content,
        )

        AleAppButtonVariant.Destructive -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.destructive,
                contentColor = colors.destructiveForeground,
                disabledContainerColor = colors.destructive.copy(alpha = 0.5f),
                disabledContentColor = colors.destructiveForeground.copy(alpha = 0.5f),
            ),
            elevation = null,
            contentPadding = contentPadding,
            content = content,
        )

        AleAppButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.foreground,
                disabledContentColor = colors.foreground.copy(alpha = 0.5f),
            ),
            contentPadding = contentPadding,
            content = content,
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Button Variants — Light", showBackground = true)
@Composable
private fun ButtonVariantsLightPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AleAppButton(onClick = {}) { Text("Primary") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Secondary) { Text("Secondary") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Outline) { Text("Outline") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Destructive) { Text("Destructive") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Ghost) { Text("Ghost") }
            }
        }
    }
}

@Preview(name = "Button Variants — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ButtonVariantsDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AleAppButton(onClick = {}) { Text("Primary") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Secondary) { Text("Secondary") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Outline) { Text("Outline") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Destructive) { Text("Destructive") }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Ghost) { Text("Ghost") }
            }
        }
    }
}

@Preview(name = "Button Sizes", showBackground = true)
@Composable
private fun ButtonSizesPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AleAppButton(onClick = {}, size = AleAppButtonSize.Small) { Text("Small") }
                AleAppButton(onClick = {}, size = AleAppButtonSize.Default) { Text("Default") }
                AleAppButton(onClick = {}, size = AleAppButtonSize.Large) { Text("Large") }
            }
        }
    }
}

@Preview(name = "Button with Icon", showBackground = true)
@Composable
private fun ButtonWithIconPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AleAppButton(onClick = {}) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Редактировать")
                }
                AleAppButton(onClick = {}, variant = AleAppButtonVariant.Outline) {
                    Text("Отмена")
                }
                AleAppButton(onClick = {}, modifier = Modifier.fillMaxWidth(), size = AleAppButtonSize.Large) {
                    Text("Подключиться")
                }
            }
        }
    }
}

@Preview(name = "Disabled Buttons", showBackground = true)
@Composable
private fun ButtonDisabledPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AleAppButton(onClick = {}, enabled = false) { Text("Primary") }
                AleAppButton(onClick = {}, enabled = false, variant = AleAppButtonVariant.Outline) { Text("Outline") }
            }
        }
    }
}