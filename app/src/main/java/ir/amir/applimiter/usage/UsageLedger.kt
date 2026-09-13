package ir.amir.applimiter.usage

import android.content.Context
import ir.amir.applimiter.data.LimitStore

/**
 * دفتر زمان: مصرف امروز هر برنامه را خودمان با ساعت خودمان می‌شماریم.
 *
 * دلیل وجودش: محاسبه‌ی مصرف از روی جفت‌کردن رویدادهای UsageStats غیرقابل‌اتکا بود
 * (زمان‌های طولانی گم می‌شد و هر باز/بسته کردن یک عدد اشتباه اضافه می‌کرد).
 * حالا سرویس هر ثانیه به برنامه‌ی جلو ثانیه اضافه می‌کند؛ دقیق و بدون پرش.
 *
 * مقادیر در حافظه نگه داشته می‌شوند و هر چند ثانیه روی دیسک ذخیره می‌شوند تا با کشته شدن
 * پروسه از دست نروند. با تغییر روز همه‌چیز صفر می‌شود.
 */
object UsageLedger {

    private const val PREFS = "usage_ledger"
    private const val KEY_DAY = "day"
    private const val PREFIX = "u_"

    private val lock = Any()
    private var loadedDay: String? = null
    private val memory = HashMap<String, Long>()
    private var dirty = false

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** اگر روز عوض شده باشد همه‌چیز را صفر می‌کند. */
    private fun ensureToday(context: Context) {
        val today = LimitStore.todayKey()
        if (loadedDay == today) return

        val p = prefs(context)
        val storedDay = p.getString(KEY_DAY, null)
        memory.clear()

        if (storedDay == today) {
            for ((key, value) in p.all) {
                if (key.startsWith(PREFIX) && value is Long) {
                    memory[key.removePrefix(PREFIX)] = value
                }
            }
        } else {
            // روز جدید: پاک‌سازی کامل
            p.edit().clear().putString(KEY_DAY, today).apply()
        }
        loadedDay = today
        dirty = false
    }

    /** به مصرف امروزِ یک بسته زمان اضافه می‌کند. */
    fun add(context: Context, pkg: String, deltaMillis: Long) {
        if (deltaMillis <= 0) return
        synchronized(lock) {
            ensureToday(context)
            memory[pkg] = (memory[pkg] ?: 0L) + deltaMillis
            dirty = true
        }
    }

    fun get(context: Context, pkg: String): Long = synchronized(lock) {
        ensureToday(context)
        memory[pkg] ?: 0L
    }

    fun all(context: Context): Map<String, Long> = synchronized(lock) {
        ensureToday(context)
        HashMap(memory)
    }

    /** مصرف یک برنامه را صفر می‌کند (برای وقتی کاربر محدودیت را عوض می‌کند). */
    fun reset(context: Context, pkg: String) {
        synchronized(lock) {
            ensureToday(context)
            memory.remove(pkg)
            dirty = true
            flush(context)
        }
    }

    /** ذخیره روی دیسک. هر چند ثانیه یک‌بار از سرویس صدا زده می‌شود. */
    fun flush(context: Context) {
        synchronized(lock) {
            if (!dirty) return
            val today = loadedDay ?: return
            val editor = prefs(context).edit()
            editor.putString(KEY_DAY, today)
            for ((pkg, millis) in memory) {
                editor.putLong(PREFIX + pkg, millis)
            }
            editor.apply()
            dirty = false
        }
    }
}
