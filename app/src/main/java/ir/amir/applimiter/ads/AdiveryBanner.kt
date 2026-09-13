package ir.amir.applimiter.ads

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adivery.sdk.AdiveryAdListener
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.BannerSize

private const val TAG = "AdiveryBanner"
private const val MAX_ATTEMPTS = 4
private const val RETRY_DELAY_MS = 6_000L

/**
 * بنر استاندارد ادیوری.
 * اگر اولین درخواست شکست بخورد، چند بار با فاصله دوباره تلاش می‌کند تا جای بنر همیشه خالی نماند.
 */
@Composable
fun AdiveryBanner(modifier: Modifier = Modifier) {
    if (!AdsManager.bannerEnabled) return

    val handler = remember { Handler(Looper.getMainLooper()) }
    var attempt by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier.fillMaxWidth().height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                val bannerView = AdiveryBannerAdView(context)
                bannerView.setPlacementId(AdsConfig.BANNER_PLACEMENT)
                bannerView.setBannerSize(BannerSize.BANNER)
                bannerView.setBannerAdListener(object : AdiveryAdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "banner loaded")
                    }

                    override fun onError(reason: String) {
                        Log.d(TAG, "banner failed: $reason")
                        if (attempt < MAX_ATTEMPTS) {
                            attempt += 1
                            handler.postDelayed({ runCatching { bannerView.loadAd() } }, RETRY_DELAY_MS)
                        }
                    }

                    override fun onAdClicked() = Unit
                })
                bannerView.loadAd()
                bannerView
            }
        )
    }
}
