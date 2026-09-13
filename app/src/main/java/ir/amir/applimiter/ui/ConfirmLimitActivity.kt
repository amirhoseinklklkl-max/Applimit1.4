package ir.amir.applimiter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.applimiter.data.InstalledAppsRepository
import ir.amir.applimiter.data.LimitStore
import ir.amir.applimiter.service.Enforcer
import ir.amir.applimiter.usage.minutesAsHuman

/** پاپ‌آپ تأیید روزانه: «امروز چند ساعت اجازه داری؟» */
class ConfirmLimitActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "pkg"
        private val OPTIONS = listOf(15, 30, 60, 120, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        val label = InstalledAppsRepository.labelOf(this, pkg)

        setContent {
            AppLimiterTheme {
                var minutes by remember { mutableIntStateOf(60) }
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text("سهمیه‌ی امروز «$label»", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("امروز چند وقت می‌خواهی از این برنامه استفاده کنی؟ بعد از تأیید، تا بامداد فردا قابل تغییر نیست.")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OPTIONS.take(3).forEach { option ->
                                    FilterChip(
                                        selected = minutes == option,
                                        onClick = { minutes = option },
                                        label = { Text(option.minutesAsHuman()) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OPTIONS.drop(3).forEach { option ->
                                    FilterChip(
                                        selected = minutes == option,
                                        onClick = { minutes = option },
                                        label = { Text(option.minutesAsHuman()) }
                                    )
                                }
                            }
                            Text(
                                "انتخاب فعلی: ${minutes.minutesAsHuman()}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            LimitStore.confirmForToday(this@ConfirmLimitActivity, pkg, minutes)
                            Enforcer.resetThrottle()
                            InstalledAppsRepository.launch(this@ConfirmLimitActivity, pkg)
                            finish()
                        }) { Text("تأیید و ورود") }
                    },
                    dismissButton = {
                        TextButton(onClick = { finish() }) { Text("بی‌خیال") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
