package com.example.android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.ui.theme.AleAppTheme

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  FormField — reusable labeled text input                                   */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun FormField(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    helperText: String? = null,
    isPassword: Boolean = false,
    minHeight: Int = 0,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AleAppTheme.colors

    Column(modifier = modifier) {
        // Label
        Row {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.foreground,
            )
            if (required) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "*",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.destructive,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Input
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.inputBackground,
            border = BorderStroke(1.dp, colors.border),
            modifier = if (minHeight > 0) {
                Modifier.height(minHeight.dp)
            } else {
                Modifier
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (prefix != null) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.mutedForeground,
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.mutedForeground,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.foreground,
                        ),
                        singleLine = singleLine,
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = keyboardOptions,
                        visualTransformation = if (isPassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Helper text
        if (helperText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════ */
/*  Previews                                                                  */
/* ═══════════════════════════════════════════════════════════════════════════ */

@Preview(name = "FormField — filled", showBackground = true)
@Composable
private fun FormFieldFilledPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            FormField(
                label = "Название сервера",
                required = true,
                value = "Tech Community",
                onValueChange = {},
                placeholder = "Введите название",
                singleLine = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "FormField — empty with helper", showBackground = true)
@Composable
private fun FormFieldEmptyHelperPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            FormField(
                label = "Username",
                required = true,
                value = "",
                onValueChange = {},
                placeholder = "username",
                singleLine = true,
                prefix = "@ ",
                helperText = "Только буквы, цифры и подчёркивание",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "FormField — password", showBackground = true)
@Composable
private fun FormFieldPasswordPreview() {
    AleAppTheme(darkTheme = false) {
        Surface(color = AleAppTheme.colors.card) {
            FormField(
                label = "API ключ",
                required = false,
                value = "secret123",
                onValueChange = {},
                placeholder = "Введите API ключ",
                singleLine = true,
                isPassword = true,
                helperText = "API ключ даёт права администратора сервера",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "FormField — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FormFieldDarkPreview() {
    AleAppTheme(darkTheme = true) {
        Surface(color = AleAppTheme.colors.card) {
            FormField(
                label = "Название сервера",
                required = true,
                value = "Tech Community",
                onValueChange = {},
                placeholder = "Введите название",
                singleLine = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
