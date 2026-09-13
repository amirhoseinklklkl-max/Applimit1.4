package ir.amir.applimiter.usage

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

/** فقط برای بررسی دسترسی «آمار استفاده». محاسبه‌ی مصرف در UsageLedger انجام می‌شود. */
object UsageTracker {

    fun hasUsageAccess(context: Context): Boolean {
        val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val op = "android:get_usage_stats"
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aom.unsafeCheckOpNoThrow(op, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            aom.checkOpNoThrow(op, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

fun Long.asHumanDuration(): String {
    val totalSeconds = this / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 && m > 0 -> "$h ساعت و $m دقیقه"
        h > 0 -> "$h ساعت"
        m > 0 -> "$m دقیقه"
        else -> "$s ثانیه"
    }
}

fun Int.minutesAsHuman(): String {
    val h = this / 60
    val m = this % 60
    return when {
        this == 0 -> "مسدود"
        h > 0 && m > 0 -> "$h ساعت و $m دقیقه"
        h > 0 -> "$h ساعت"
        else -> "$m دقیقه"
    }
}
