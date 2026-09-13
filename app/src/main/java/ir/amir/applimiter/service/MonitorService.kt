package ir.amir.applimiter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ir.amir.applimiter.MainActivity
import ir.amir.applimiter.R
import ir.amir.applimiter.data.InstalledAppsRepository
import ir.amir.applimiter.usage.ForegroundWatcher
import ir.amir.applimiter.usage.UsageLedger
import ir.amir.applimiter.usage.UsageTracker
import ir.amir.applimiter.usage.asHumanDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * سرویس همیشه‌روشن. هر ثانیه:
 *  ۱) برنامه‌ی جلو را پیدا می‌کند
 *  ۲) به مصرف امروزش زمان واقعی سپری‌شده را اضافه می‌کند
 *  ۳) بررسی می‌کند سهمیه تمام شده یا نه
 */
class MonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "monitor_channel"
        private const val NOTIF_ID = 1001

        /** فاصله‌ی تیک؛ یک ثانیه تا شمارش دقیق باشد. */
        private const val TICK_MS = 1000L

        /** سقف زمانی که در یک تیک می‌شود اضافه کرد. جلوی پرش بعد از کشته شدن پروسه را می‌گیرد. */
        private const val MAX_DELTA_MS = 3000L

        /** هر چند تیک روی دیسک ذخیره کن. */
        private const val FLUSH_EVERY_TICKS = 10

        @Volatile var lastForegroundPackage: String? = null

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val watcher by lazy { ForegroundWatcher(this) }

    private var lastTickAt = 0L
    private var ticks = 0
    private var lastNotificationText = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification("در حال مراقبت از زمان استفاده"))
        lastTickAt = System.currentTimeMillis()

        scope.launch {
            while (isActive) {
                try {
                    tick()
                } catch (t: Throwable) {
                    // ادامه بده تا سرویس نخوابد
                }
                delay(TICK_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun tick() {
        val now = System.currentTimeMillis()
        val rawDelta = now - lastTickAt
        lastTickAt = now

        if (!UsageTracker.hasUsageAccess(this)) return

        // صفحه خاموش یا قفل؟ زمان نباید شمرده شود
        if (!watcher.isUserPresent()) {
            updateNotification("در حال مراقبت از زمان استفاده")
            return
        }

        val pkg = watcher.current(lastForegroundPackage) ?: return

        // زمان واقعی سپری‌شده را اضافه کن، با سقف امن
        val delta = rawDelta.coerceIn(0L, MAX_DELTA_MS)
        if (isCountable(pkg)) {
            UsageLedger.add(this, pkg, delta)
        }

        if (++ticks >= FLUSH_EVERY_TICKS) {
            ticks = 0
            UsageLedger.flush(this)
        }

        Enforcer.evaluate(this, pkg)

        val remaining = Enforcer.remainingMillis(this, pkg)
        val text = if (remaining == null) {
            "در حال مراقبت از زمان استفاده"
        } else {
            val name = InstalledAppsRepository.labelOf(this, pkg)
            "$name: ${remaining.asHumanDuration()} باقی‌مانده"
        }
        updateNotification(text)
    }

    /** برنامه‌ی خودمان، لانچر و صفحات سیستمی نباید در آمار بیایند. */
    private fun isCountable(pkg: String): Boolean {
        if (pkg == packageName) return false
        if (pkg == "android" || pkg == "com.android.systemui") return false
        return true
    }

    private fun updateNotification(text: String) {
        if (text == lastNotificationText) return
        lastNotificationText = text
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "مراقبت از زمان",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        UsageLedger.flush(this)
        scope.cancel()
        super.onDestroy()
    }
}
