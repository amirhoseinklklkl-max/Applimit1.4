package ir.amir.applimiter.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener
import java.lang.ref.WeakReference

/**
 * لایه‌ی واسط با ادیوری برای تبلیغ «بازگشت به برنامه» (App Open).
 *
 * روش کار: نمایش «صف» می‌شود. لحظه‌ای که تبلیغ آماده شد، اگر صفحه هنوز باز باشد نشان داده می‌شود.
 * درخواست ناموفق هم چند بار با فاصله دوباره تلاش می‌کند.
 */
object AdsManager {

    private const val TAG = "AdiveryAds"
    private const val MAX_ATTEMPTS = 4
    private const val RETRY_DELAY_MS = 5_000L

    /** تا این مدت بعد از ورود، اگر تبلیغ برسد نشان بده. بعد از آن دیگر مزاحم کاربر نشو. */
    private const val PENDING_WINDOW_MS = 20_000L

    private val main = Handler(Looper.getMainLooper())

    private var activityRef: WeakReference<Activity>? = null

    @Volatile private var appOpenLoaded = false
    @Volatile private var requestInFlight = false
    @Volatile private var attempts = 0
    @Volatile private var lastShownAt = 0L
    @Volatile private var showing = false
    @Volatile private var pendingShowUntil = 0L
    @Volatile private var listenerRegistered = false

    /** برای دیدن وضعیت در حالت دیباگ، تا اشکال‌یابی راحت باشد. */
    var status by mutableStateOf("آماده‌سازی…")
        private set

    val appOpenEnabled: Boolean
        get() = AdsConfig.isConfigured(AdsConfig.APPOPEN_PLACEMENT)

    val bannerEnabled: Boolean
        get() = AdsConfig.isConfigured(AdsConfig.BANNER_PLACEMENT)

    val isAdReady: Boolean
        get() = appOpenLoaded

    // ---------------------------------------------------------------- lifecycle

    fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)

        if (!listenerRegistered) {
            listenerRegistered = true
            registerAppOpenListener()
        }
        preloadAppOpen()
    }

    fun onActivityPaused(activity: Activity) {
        if (activityRef?.get() === activity) activityRef = null
    }

    private fun currentActivity(): Activity? {
        val activity = activityRef?.get() ?: return null
        if (activity.isFinishing || activity.isDestroyed) return null
        return activity
    }

    // ---------------------------------------------------------------- request

    private fun registerAppOpenListener() {
        if (!appOpenEnabled) return
        runCatching {
            Adivery.addPlacementListener(
                AdsConfig.APPOPEN_PLACEMENT,
                object : AdiveryListener() {
                    override fun onAppOpenAdLoaded(placementId: String) {
                        requestInFlight = false
                        attempts = 0
                        appOpenLoaded = true
                        updateStatus("تبلیغ آماده است")
                        // اگر نمایش در صف بود، همین حالا نشان بده
                        main.post { showIfPending() }
                    }

                    override fun onAppOpenAdShown(placementId: String) {
                        updateStatus("تبلیغ نمایش داده شد")
                    }

                    override fun onAppOpenAdClicked(placementId: String) = Unit

                    override fun onAppOpenAdClosed(placementId: String) {
                        showing = false
                        appOpenLoaded = false
                        updateStatus("تبلیغ بسته شد")
                        attempts = 0
                        preloadAppOpen()
                    }
                }
            )
        }.onFailure { updateStatus("خطا در ثبت listener: ${it.message}") }
    }

    /**
     * تبلیغ را از قبل می‌گیرد. اگر شکست خورد، چند بار با فاصله دوباره تلاش می‌کند.
     *
     * توجه: چون امضای دقیق callback خطا (onError) در نسخه‌ی فعلی SDK با مستندات هم‌خوانی نداشت،
     * به‌جای تکیه به آن، بعد از یک بازه‌ی زمانی اگر تبلیغ لود نشده باشد، خودمان آن را
     * شکست‌خورده در نظر می‌گیریم و دوباره تلاش می‌کنیم.
     */
    fun preloadAppOpen(force: Boolean = false) {
        if (!appOpenEnabled) {
            updateStatus("جایگاه بازگشت به برنامه تنظیم نشده")
            return
        }
        if (force) {
            attempts = 0
            appOpenLoaded = false
        }
        if (appOpenLoaded || requestInFlight) return
        if (attempts >= MAX_ATTEMPTS) {
            updateStatus("تبلیغی موجود نبود (${attempts} تلاش)")
            return
        }

        val activity = currentActivity() ?: return

        requestInFlight = true
        attempts++
        val thisAttempt = attempts
        updateStatus("در حال دریافت تبلیغ… (تلاش $attempts)")

        runCatching {
            Adivery.prepareAppOpenAd(activity, AdsConfig.APPOPEN_PLACEMENT)
        }.onFailure {
            requestInFlight = false
            updateStatus("خطای درخواست: ${it.message}")
            Log.w(TAG, "app open request error: ${it.message}")
        }

        // اگر تا این مدت onAppOpenAdLoaded صدا زده نشد، شکست‌خورده در نظرش بگیر و دوباره تلاش کن
        main.postDelayed({
            if (requestInFlight && attempts == thisAttempt && !appOpenLoaded) {
                requestInFlight = false
                updateStatus("دریافت نشد (پاسخی نرسید)")
                if (attempts < MAX_ATTEMPTS) {
                    preloadAppOpen()
                }
            }
        }, RETRY_DELAY_MS + PENDING_WINDOW_MS)
    }

    // ---------------------------------------------------------------- show

    /**
     * در ورود کاربر صدا زده می‌شود.
     * اگر تبلیغ آماده باشد فوراً نشان می‌دهد، وگرنه نمایش را در صف می‌گذارد تا برسد.
     */
    fun requestShowOnEntry(bypassInterval: Boolean = false) {
        if (!appOpenEnabled || showing) return

        val now = System.currentTimeMillis()
        if (!bypassInterval && now - lastShownAt < AdsConfig.APPOPEN_MIN_INTERVAL_MS) {
            updateStatus("فاصله‌ی بین دو تبلیغ رعایت می‌شود")
            return
        }

        pendingShowUntil = now + PENDING_WINDOW_MS
        if (appOpenLoaded) {
            main.post { showIfPending() }
        } else {
            preloadAppOpen()
        }
    }

    private fun showIfPending() {
        if (showing) return
        if (System.currentTimeMillis() > pendingShowUntil) return
        if (!appOpenLoaded || !Adivery.isLoaded(AdsConfig.APPOPEN_PLACEMENT)) return

        val activity = currentActivity() ?: return

        showing = true
        lastShownAt = System.currentTimeMillis()
        pendingShowUntil = 0
        updateStatus("در حال نمایش تبلیغ")

        runCatching {
            Adivery.showAppOpenAd(activity, AdsConfig.APPOPEN_PLACEMENT)
        }.onFailure {
            showing = false
            updateStatus("خطای نمایش: ${it.message}")
            Log.w(TAG, "app open show error: ${it.message}")
        }
    }

    private fun updateStatus(text: String) {
        main.post { status = text }
        Log.d(TAG, text)
    }
}
