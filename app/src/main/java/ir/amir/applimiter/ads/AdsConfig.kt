package ir.amir.applimiter.ads

import ir.amir.applimiter.BuildConfig

/**
 * تنظیمات تبلیغات ادیوری.
 * App ID و جایگاه‌ها (Placement) از gradle.properties خوانده می‌شوند تا داخل کد hardcode نشوند.
 */
object AdsConfig {

    /** کلید اپلیکیشن در پنل ادیوری؛ در Application.onCreate به Adivery.configure داده می‌شود. */
    val APP_ID: String = BuildConfig.ADIVERY_APP_ID.trim()

    /** جایگاه تبلیغ «بازگشت به برنامه» (App Open) که هنگام ورود/بازگشت کاربر نمایش داده می‌شود. */
    val APPOPEN_PLACEMENT: String = BuildConfig.ADIVERY_PLACEMENT_APPOPEN.trim()

    /** جایگاه بنر پایین لیست برنامه‌ها. خالی باشد، بنری نمایش داده نمی‌شود. */
    val BANNER_PLACEMENT: String = BuildConfig.ADIVERY_PLACEMENT_BANNER.trim()

    /** حداقل فاصله‌ی بین دو تبلیغ؛ جلوی اذیت شدن کاربر را می‌گیرد. */
    const val APPOPEN_MIN_INTERVAL_MS = 3 * 60_000L

    private val placeholders = setOf(
        "",
        "YOUR_ADIVERY_APP_ID",
        "YOUR_APPOPEN_PLACEMENT_ID",
        "YOUR_BANNER_PLACEMENT_ID"
    )

    fun isConfigured(value: String): Boolean = value !in placeholders
}
