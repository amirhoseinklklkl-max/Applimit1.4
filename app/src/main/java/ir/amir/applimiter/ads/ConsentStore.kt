package ir.amir.applimiter.ads

import android.content.Context

/** رضایت کاربر برای تبلیغات شخصی‌سازی‌شده (GDPR) را نگه می‌دارد تا هر بار پرسیده نشود. */
object ConsentStore {

    private const val PREFS = "ads_consent"
    private const val KEY_ASKED = "asked"
    private const val KEY_GRANTED = "granted"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasBeenAsked(context: Context): Boolean = prefs(context).getBoolean(KEY_ASKED, false)

    fun isGranted(context: Context): Boolean = prefs(context).getBoolean(KEY_GRANTED, false)

    fun save(context: Context, granted: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_ASKED, true)
            .putBoolean(KEY_GRANTED, granted)
            .apply()
    }
}
