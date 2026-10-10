package com.waveyy.app.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Outlined text input. Ports `components/ui/input.jsx`. Extended with
 * password visibility toggle, error state, and supporting text.
 *
 * @param value current text.
 * @param onValueChange text change handler.
 * @param modifier layout modifier.
 * @param label optional label.
 * @param placeholder optional placeholder.
 * @param singleLine single-line flag.
 * @param enabled input enabled flag.
 * @param isError error highlighting flag.
 * @param supportingText optional helper or error text below the field.
 * @param isPassword when true, masks input and shows a visibility toggle.
 * @param keyboardOptions keyboard configuration.
 * @param keyboardActions keyboard action handlers.
 */
@Composable
fun WaveyyInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = singleLine,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                      else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}
