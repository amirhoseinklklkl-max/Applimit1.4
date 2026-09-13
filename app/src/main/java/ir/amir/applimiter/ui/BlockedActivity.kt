package ir.amir.applimiter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.amir.applimiter.data.InstalledAppsRepository
import ir.amir.applimiter.data.LimitStore
import ir.amir.applimiter.usage.asHumanDuration
import ir.amir.applimiter.usage.minutesAsHuman

/** صفحه‌ای که بعد از پر شدن زمان مجاز نشان داده می‌شود و راه بازگشت به برنامه را می‌بندد. */
class BlockedActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "pkg"
        const val EXTRA_USED = "used"
        const val EXTRA_LIMIT = "limit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val used = intent.getLongExtra(EXTRA_USED, 0L)
        val limit = intent.getIntExtra(EXTRA_LIMIT, 0)
        val label = InstalledAppsRepository.labelOf(this, pkg)

        // بازگشت به برنامه‌ی مسدود ممکن نیست
        onBackPressedDispatcher.addCallback(this) { finish() }

        setContent {
            AppLimiterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlockedContent(
                        label = label,
                        used = used,
                        limit = limit,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedContent(label: String, used: Long, limit: Int, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text("زمان امروزت تمام شد", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            text = if (limit <= 0) {
                "«$label» کاملاً مسدود است."
            } else {
                "سهمیه‌ی «$label» برای امروز ${limit.minutesAsHuman()} بود و ${used.asHumanDuration()} استفاده کردی."
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            "دوباره در ${LimitStore.millisUntilTomorrow().asHumanDuration()} دیگر (بامداد فردا) باز می‌شود.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onClose, modifier = Modifier.padding(top = 22.dp)) {
            Text("باشه، بستم")
        }
    }
}
