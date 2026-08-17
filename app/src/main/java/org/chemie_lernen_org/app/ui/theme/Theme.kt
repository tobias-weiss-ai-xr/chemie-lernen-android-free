package org.chemie_lernen_org.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandGreen = Color(0xFF2E7D32)
private val BrandGreenDark = Color(0xFF4CAF50)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    secondary = BrandGreen,
    background = Color(0xFFF0F2F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8ECF0),
    onSurfaceVariant = Color(0xFF44474E),
    outlineVariant = Color(0xFFC4C9D0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFA5D6A7),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB8BCC9),
    outlineVariant = Color(0xFF3A4260),
)

@Composable
fun ChemieLernenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
