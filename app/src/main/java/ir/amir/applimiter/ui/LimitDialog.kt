package ir.amir.applimiter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.applimiter.data.AppEntry
import ir.amir.applimiter.data.LimitConfig
import ir.amir.applimiter.usage.asHumanDuration
import ir.amir.applimiter.usage.minutesAsHuman

private val PRESETS = listOf(15, 30, 60, 120, 180, 300)

/**
 * پاپ‌آپی که با زدن روی هر برنامه باز می‌شود:
 * انتخاب سهمیه‌ی هر ۲۴ ساعت، پرسش روزانه، مسدودسازی کامل و حذف محدودیت.
 */
@Composable
fun LimitDialog(
    app: AppEntry,
    current: LimitConfig?,
    usedTodayMillis: Long,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, askDaily: Boolean) -> Unit,
    onRemove: () -> Unit,
    onResetUsage: () -> Unit
) {
    var minutes by remember { mutableIntStateOf(current?.limitMinutes ?: 60) }
    var askDaily by remember { mutableStateOf(current?.askDaily ?: false) }
    var sliderValue by remember { mutableFloatStateOf((current?.limitMinutes ?: 60).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(app.label, fontWeight = FontWeight.Bold)
                Text(
                    "امروز ${usedTodayMillis.asHumanDuration()} استفاده شده",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("در هر ۲۴ ساعت چقدر مجاز باشد؟", fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRESETS.take(3).forEach { preset ->
                        FilterChip(
                            selected = minutes == preset,
                            onClick = { minutes = preset; sliderValue = preset.toFloat() },
                            label = { Text(preset.minutesAsHuman()) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRESETS.drop(3).forEach { preset ->
                        FilterChip(
                            selected = minutes == preset,
                            onClick = { minutes = preset; sliderValue = preset.toFloat() },
                            label = { Text(preset.minutesAsHuman()) }
                        )
                    }
                }

                Text("دلخواه: ${minutes.minutesAsHuman()}")
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        minutes = (it / 5).toInt() * 5
                    },
                    valueRange = 0f..480f
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("هر ۲۴ ساعت از من بپرس", fontWeight = FontWeight.SemiBold)
                        Text(
                            "بار اول در هر روز، قبل از باز شدن برنامه سهمیه‌ی همان روز را تأیید می‌کنی",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = askDaily, onCheckedChange = { askDaily = it })
                }

                HorizontalDivider()

                TextButton(onClick = { onSave(0, false) }) {
                    Text("مسدودسازی کامل (بدون اجازه‌ی استفاده)")
                }

                TextButton(onClick = onResetUsage) {
                    Text("صفر کردن مصرف امروز")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(minutes, askDaily) }) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = onRemove) { Text("حذف محدودیت") }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}
