package ir.amir.applimiter.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * تنظیمات محدودیت هر برنامه.
 * limitMinutes = دقیقه‌ی مجاز در هر ۲۴ ساعت (۰ یعنی کاملاً مسدود)
 * askDaily     = هر روز اول کار از کاربر بپرس امروز چند ساعت مجاز است
 */
data class LimitConfig(
    val packageName: String,
    val limitMinutes: Int,
    val askDaily: Boolean = false,
    val confirmedDay: String = "",
    val todayLimitMinutes: Int = 0
)

object LimitStore {

    private const val PREFS = "app_limits"
    private const val KEY_PACKAGES = "packages"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

    fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun millisUntilTomorrow(): Long {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis - System.currentTimeMillis()
    }

    private fun packages(context: Context): MutableSet<String> =
        prefs(context).getStringSet(KEY_PACKAGES, emptySet())!!.toMutableSet()

    fun get(context: Context, pkg: String): LimitConfig? {
        val p = prefs(context)
        if (!p.contains("limit_$pkg")) return null
        return LimitConfig(
            packageName = pkg,
            limitMinutes = p.getInt("limit_$pkg", 0),
            askDaily = p.getBoolean("ask_$pkg", false),
            confirmedDay = p.getString("day_$pkg", "") ?: "",
            todayLimitMinutes = p.getInt("todaylimit_$pkg", 0)
        )
    }

    fun save(context: Context, config: LimitConfig) {
        val p = prefs(context)
        val pkgs = packages(context).also { it.add(config.packageName) }
        p.edit()
            .putInt("limit_${config.packageName}", config.limitMinutes)
            .putBoolean("ask_${config.packageName}", config.askDaily)
            .putString("day_${config.packageName}", config.confirmedDay)
            .putInt("todaylimit_${config.packageName}", config.todayLimitMinutes)
            .putStringSet(KEY_PACKAGES, pkgs)
            .apply()
    }

    fun remove(context: Context, pkg: String) {
        val p = prefs(context)
        val pkgs = packages(context).also { it.remove(pkg) }
        p.edit()
            .remove("limit_$pkg")
            .remove("ask_$pkg")
            .remove("day_$pkg")
            .remove("todaylimit_$pkg")
            .putStringSet(KEY_PACKAGES, pkgs)
            .apply()
    }

    fun confirmForToday(context: Context, pkg: String, minutes: Int) {
        val current = get(context, pkg) ?: LimitConfig(pkg, minutes, askDaily = true)
        save(
            context,
            current.copy(
                askDaily = true,
                confirmedDay = todayKey(),
                todayLimitMinutes = minutes,
                limitMinutes = minutes
            )
        )
    }

    fun all(context: Context): List<LimitConfig> =
        packages(context).mapNotNull { get(context, it) }

    /** دقیقه‌ی مجاز امروز؛ null یعنی هنوز باید از کاربر پرسیده شود. */
    fun effectiveMinutes(config: LimitConfig): Int? {
        if (!config.askDaily) return config.limitMinutes
        if (config.confirmedDay != todayKey()) return null
        return config.todayLimitMinutes
    }
}
