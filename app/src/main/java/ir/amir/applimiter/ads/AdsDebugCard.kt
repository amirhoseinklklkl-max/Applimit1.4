package ir.amir.applimiter.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * کارت وضعیت تبلیغات؛ فقط در بیلد دیباگ نمایش داده می‌شود.
 * برای اشکال‌یابی: می‌گوید تبلیغ در چه مرحله‌ای است و اجازه‌ی نمایش دستی می‌دهد.
 */
@Composable
fun AdsDebugCard() {
    val activity = LocalContext.current as? Activity ?: return

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("وضعیت تبلیغات (فقط دیباگ)", fontWeight = FontWeight.Bold)
            Text(AdsManager.status, style = MaterialTheme.typography.bodySmall)
            Text(
                "بازگشت به برنامه: ${if (AdsManager.appOpenEnabled) "فعال" else "زون ندارد"} • " +
                    "بنر: ${if (AdsManager.bannerEnabled) "فعال" else "زون ندارد"} • " +
                    "آماده: ${if (AdsManager.isAdReady) "بله" else "خیر"}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { AdsManager.preloadAppOpen(force = true) }) {
                    Text("دریافت مجدد")
                }
                OutlinedButton(onClick = { AdsManager.requestShowOnEntry(bypassInterval = true) }) {
                    Text("نمایش تبلیغ")
                }
            }
        }
    }
}
