package ir.amir.applimiter.service

import android.content.Context
import android.content.Intent
import ir.amir.applimiter.data.LimitStore
import ir.amir.applimiter.ui.BlockedActivity
import ir.amir.applimiter.ui.ConfirmLimitActivity
import ir.amir.applimiter.usage.UsageLedger

/** مغز برنامه: تصمیم می‌گیرد برنامه‌ی جلو باید بسته شود یا نه. */
object Enforcer {

    private const val THROTTLE_MS = 2500L

    @Volatile private var lastPkg: String? = null
    @Volatile private var lastActionAt = 0L

    private val systemWhitelist = setOf(
        "com.android.systemui",
        "com.android.settings",
        "android"
    )

    fun evaluate(context: Context, pkg: String) {
        if (pkg == context.packageName || pkg in systemWhitelist) return
        if (isLauncher(context, pkg)) return

        val config = LimitStore.get(context, pkg) ?: return
        val minutes = LimitStore.effectiveMinutes(config)

        // هنوز برای امروز تأییدی ثبت نشده -> پاپ‌آپ تأیید روزانه
        if (minutes == null) {
            if (throttled(pkg)) return
            goHome(context)
            context.startActivity(
                Intent(context, ConfirmLimitActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(ConfirmLimitActivity.EXTRA_PACKAGE, pkg)
                }
            )
            return
        }

        val used = UsageLedger.get(context, pkg)
        val limitMs = minutes * 60_000L
        if (minutes <= 0 || used >= limitMs) {
            if (throttled(pkg)) return
            UsageLedger.flush(context)
            goHome(context)
            context.startActivity(
                Intent(context, BlockedActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(BlockedActivity.EXTRA_PACKAGE, pkg)
                    putExtra(BlockedActivity.EXTRA_USED, used)
                    putExtra(BlockedActivity.EXTRA_LIMIT, minutes)
                }
            )
        }
    }

    /** زمان باقی‌مانده‌ی امروز برای یک برنامه به میلی‌ثانیه؛ null یعنی محدودیتی ندارد. */
    fun remainingMillis(context: Context, pkg: String): Long? {
        val config = LimitStore.get(context, pkg) ?: return null
        val minutes = LimitStore.effectiveMinutes(config) ?: return 0L
        return (minutes * 60_000L - UsageLedger.get(context, pkg)).coerceAtLeast(0L)
    }

    private fun throttled(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        if (lastPkg == pkg && now - lastActionAt < THROTTLE_MS) return true
        lastPkg = pkg
        lastActionAt = now
        return false
    }

    fun resetThrottle() {
        lastPkg = null
        lastActionAt = 0L
    }

    private fun goHome(context: Context) {
        if (!AppLockAccessibilityService.goHome()) {
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun isLauncher(context: Context, pkg: String): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(home, 0)
        return res?.activityInfo?.packageName == pkg
    }
}
