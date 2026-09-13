package ir.amir.applimiter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1E62D0)
private val BlueDark = Color(0xFF9EC1FF)

@Composable
fun AppLimiterTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = BlueDark, secondary = BlueDark)
    } else {
        lightColorScheme(primary = Blue, secondary = Blue)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
