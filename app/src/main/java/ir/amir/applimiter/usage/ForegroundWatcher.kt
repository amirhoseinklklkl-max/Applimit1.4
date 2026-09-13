package ir.amir.applimiter.usage

import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager

/**
 * تشخیص اینکه همین حالا کدام برنامه جلو است.
 *
 * فقط برای «شناسایی» از رویدادهای سیستم استفاده می‌کنیم (که قابل اتکاست)؛
 * «اندازه‌گیری مدت» را خودمان با ساعت خودمان انجام می‌دهیم.
 * یک نشانگر (cursor) نگه می‌داریم تا هر ثانیه کل روز را دوباره نخوانیم.
 */
class ForegroundWatcher(private val context: Context) {

    private val usm by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    private val power by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    private val keyguard by lazy {
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    private var cursor = 0L
    private var current: String? = null

    /** صفحه روشن و قفل باز است؟ اگر نه، زمان نباید شمرده شود. */
    fun isUserPresent(): Boolean =
        power.isInteractive && !keyguard.isKeyguardLocked

    /**
     * برنامه‌ی جلو را برمی‌گرداند.
     * @param hint اگر سرویس دسترس‌پذیری روشن باشد، آخرین بسته‌ای که دیده (سریع و دقیق).
     */
    fun current(hint: String? = null): String? {
        val now = System.currentTimeMillis()
        if (cursor == 0L) cursor = now - 30_000

        try {
            val events = usm.queryEvents(cursor, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
                if (event.eventType == 1) {
                    event.packageName?.let { current = it }
                }
            }
            cursor = now
        } catch (t: Throwable) {
            // دسترسی آمار قطع شده؟ به hint تکیه کن
        }

        // رویدادهای سیستم فیلترشده و مطمئن‌ترند؛ hint فقط جانشین است
        return current ?: hint
    }
}
