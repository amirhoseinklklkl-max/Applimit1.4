package ir.amir.applimiter.ads

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/** دیالوگ رضایت تبلیغات؛ برای انتشار در گوگل‌پلی لازم است (GDPR). */
@Composable
fun AdsConsentDialog(
    onResult: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("تبلیغات و حمایت از برنامه", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "این برنامه رایگان است و با نمایش تبلیغات نگه‌داری می‌شود. " +
                    "اجازه می‌دهی تبلیغ‌های مرتبط‌تری بر اساس شناسه‌ی تبلیغاتی دستگاهت ببینی؟ " +
                    "با رد کردن هم برنامه کامل کار می‌کند، فقط تبلیغ‌ها عمومی‌تر می‌شوند."
            )
        },
        confirmButton = {
            TextButton(onClick = { onResult(true) }) { Text("موافقم") }
        },
        dismissButton = {
            TextButton(onClick = { onResult(false) }) { Text("نه، ممنون") }
        }
    )
}
