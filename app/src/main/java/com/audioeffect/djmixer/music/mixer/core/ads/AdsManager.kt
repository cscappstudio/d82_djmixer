package com.audioeffect.djmixer.music.mixer.core.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.cscmobi.libraryads.ads.banner_ads.CSCBanner
import com.cscmobi.libraryads.ads.inter_ads.CSCInter
import com.cscmobi.libraryads.ads.native_ads.CSCNativeManager
import com.cscmobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.cscmobi.libraryads.ads.open_ads.CSCOpenAds
import com.audioeffect.djmixer.music.mixer.R
import com.audioeffect.djmixer.music.mixer.presentation.dialogs.AdLoadingDialog

object AdsManager {
    private const val TAG = "AdsManager"

    private var openAdsInstance: CSCOpenAds? = null

    // 15-second debounce between interstitials requirement
    private var lastInterShowTime: Long = 0L
    private const val INTER_INTERVAL_MS = 15_000L

    fun registerOpenAds(openAds: CSCOpenAds?) {
        this.openAdsInstance = openAds
    }

    fun disableAOA() {
        try {
            openAdsInstance?.disableShowAOA()
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling AOA", e)
        }
    }

    fun enableAOA() {
        try {
            openAdsInstance?.enableShowAOA()
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling AOA", e)
        }
    }

    /**
     * Show Banner in MainActivity or any screen.
     * If isCollapsible is false, requests standard BANNER_ADAPTIVE.
     * If isCollapsible is true, requests BANNER_COLLAPSIBLE_BOTTOM.
     */
    fun showBanner(
        activity: Activity,
        container: ViewGroup?,
        adUnitId: String,
        isEnabled: Boolean = true,
        isCollapsible: Boolean = false,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (container == null) return
        if (!isEnabled || adUnitId.isBlank()) {
            container.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        try {
            Log.d(TAG, "showBanner: unitId=$adUnitId, isEnabled=$isEnabled, isCollapsible=$isCollapsible")
            val frameLayout = if (container is FrameLayout) {
                container
            } else {
                val existing = container.findViewById<FrameLayout>(R.id.flBannerContainer)
                if (existing != null) {
                    existing
                } else {
                    val fl = FrameLayout(activity).apply { id = R.id.flBannerContainer }
                    container.removeAllViews()
                    container.addView(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    fl
                }
            }
            container.visibility = View.VISIBLE
            val bannerType = if (isCollapsible) {
                CSCBanner.TypeAds.BANNER_COLLAPSIBLE_BOTTOM
            } else {
                CSCBanner.TypeAds.BANNER_ADAPTIVE
            }
            CSCBanner.requestBanner(
                activity = activity,
                id = adUnitId,
                typeAds = bannerType,
                adFrame = frameLayout,
                canShowAd = isEnabled,
                onResult = { adView ->
                    activity.runOnUiThread {
                        if (adView != null) {
                            Log.d(TAG, "Banner loaded successfully")
                            container.visibility = View.VISIBLE
                            onLoaded?.invoke()
                        } else {
                            Log.e(TAG, "Banner failed to load (adView is null)")
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }
                },
                onShown = {
                    Log.d(TAG, "Banner impression recorded")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting banner", e)
            container.visibility = View.GONE
            onFailed?.invoke()
        }
    }

    /**
     * Show Collapsible Bottom Banner in MainActivity or any screen
     */
    fun showCollapsibleBanner(
        activity: Activity,
        container: ViewGroup?,
        adUnitId: String,
        isEnabled: Boolean = true,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        showBanner(
            activity = activity,
            container = container,
            adUnitId = adUnitId,
            isEnabled = isEnabled,
            isCollapsible = true,
            onLoaded = onLoaded,
            onFailed = onFailed
        )
    }

    fun destroyBanner(activity: Activity) {
        try {
            CSCBanner.destroyBannerAds(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying banner ad", e)
        }
    }

    /**
     * Load & Show Native Ad inside a container (FrameLayout or ViewGroup)
     */
    fun loadAndShowNative(
        activity: Activity,
        container: ViewGroup?,
        placementKey: String,
        adUnitId: String,
        layoutRes: Int = R.layout.layout_native_media,
        isEnabled: Boolean = true,
        isShowLoading: Boolean = true,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (container == null) return
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            container.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        try {
            val frameLayout: FrameLayout = if (container is FrameLayout) {
                container
            } else {
                val existing = container.findViewById<FrameLayout>(R.id.flNativeContainer)
                if (existing != null) {
                    existing
                } else {
                    val fl = FrameLayout(activity).apply { id = R.id.flNativeContainer }
                    container.removeAllViews()
                    container.addView(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    fl
                }
            }

            val effectiveKey = if (placementKey.isNotBlank()) {
                if (placementKey.contains(activity.javaClass.simpleName)) placementKey else "${placementKey}_${activity.javaClass.simpleName}"
            } else {
                "native_${activity.javaClass.simpleName}"
            }

            Log.d(TAG, "loadAndShowNative: key=$effectiveKey, unitId=$adUnitId, isEnabled=$isEnabled")
            container.visibility = View.VISIBLE
            CSCNativeManager.loadAndShowNative(
                adFrame = frameLayout,
                adName = effectiveKey,
                adId = adUnitId,
                adLayout = layoutRes,
                isPreload = false,
                canShowAd = isEnabled,
                callback = object : INativeAdCallback {
                    override fun onAdLoaded(adName: String) {
                        Log.d(TAG, "Native onAdLoaded: $adName")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.VISIBLE
                            onLoaded?.invoke()
                        }
                    }

                    override fun onAdFailedToLoad(adName: String, errorMessage: String) {
                        Log.e(TAG, "Native onAdFailedToLoad: $adName, error=$errorMessage")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdShowFailed(adName: String, reason: String) {
                        Log.e(TAG, "Native onAdShowFailed: $adName, reason=$reason")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdDismissed(adName: String) {
                        Log.d(TAG, "Native onAdDismissed: $adName")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading native ad: $placementKey", e)
            container.visibility = View.GONE
            onFailed?.invoke()
        }
    }

    /**
     * Show Interstitial Ad with safety fallback so navigation never hangs
     */
    fun showInter(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean = true,
        onNext: () -> Unit
    ) {
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            onNext()
            return
        }

        var executed = false
        val safeNext: () -> Unit = {
            if (!executed) {
                executed = true
                try {
                    com.cscmobi.libraryads.ads.utils.StatusShowAd.isInterstitialShown = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting inter status", e)
                }
                activity.runOnUiThread { onNext() }
            }
        }

        try {
            CSCInter.showInter(
                activity = activity,
                adName = adUnitId,
                nextAction = { _: Boolean -> safeNext() },
                onShown = null,
                preload = true,
                canShowAd = isEnabled
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing inter ad", e)
            safeNext()
        }
    }

    /**
     * Load & Show Interstitial Ad with timeout
     */
    fun loadAndShowInter(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean = true,
        timeout: Long = 6000L,
        onNext: () -> Unit
    ) {
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            onNext()
            return
        }

        var executed = false
        val safeNext: () -> Unit = {
            if (!executed) {
                executed = true
                AdLoadingDialog.dismiss()
                try {
                    com.cscmobi.libraryads.ads.utils.StatusShowAd.isInterstitialShown = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting inter status", e)
                }
                activity.runOnUiThread { onNext() }
            }
        }

        try {
            AdLoadingDialog.show(activity)

            CSCInter.loadAndShowInter(
                activity = activity,
                adId = adUnitId,
                timeDelay = 0L,
                timeOut = timeout,
                nextAction = { _: Boolean ->
                    AdLoadingDialog.dismiss()
                    safeNext()
                },
                onShown = {
                    AdLoadingDialog.dismiss()
                    lastInterShowTime = android.os.SystemClock.elapsedRealtime()
                },
                canShowId = isEnabled,
                onLoadFailed = {
                    AdLoadingDialog.dismiss()
                    safeNext()
                },
                showLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loadAndShowInter", e)
            AdLoadingDialog.dismiss()
            safeNext()
        }
    }

    fun canShowInter(): Boolean {
        if (!RemoteConfigs.inter_all) return false
        val now = android.os.SystemClock.elapsedRealtime()
        return (lastInterShowTime == 0L || (now - lastInterShowTime) >= INTER_INTERVAL_MS)
    }

    /**
     * Show inter_all with strict 15-second debounce.
     * Used for side tools (EQ, Bass, Loop, Cues, Sample), selecting music, and record.
     */
    fun showInterAll(activity: Activity, onNext: () -> Unit) {
        if (!canShowInter()) {
            val now = android.os.SystemClock.elapsedRealtime()
            val diffSec = (now - lastInterShowTime) / 1000
            Log.d(TAG, "showInterAll debounced (only ${diffSec}s since last inter, minimum 15s required)")
            onNext()
            return
        }

        loadAndShowInter(
            activity = activity,
            adUnitId = activity.getString(R.string.inter_all),
            isEnabled = RemoteConfigs.inter_all,
            timeout = 5000L
        ) {
            lastInterShowTime = android.os.SystemClock.elapsedRealtime()
            onNext()
        }
    }

    /**
     * Notify AdsManager that a rewarded ad was completed, resetting the interstitial cooldown.
     */
    fun setRewardWatched() {
        lastInterShowTime = android.os.SystemClock.elapsedRealtime()
    }

    /**
     * Show Rewarded Ad with safety fallback
     */
    fun showReward(
        activity: Activity,
        onEarnedReward: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        val adUnitId = activity.getString(R.string.reward_all)
        if (!RemoteConfigs.reward_all || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            setRewardWatched()
            onEarnedReward()
            return
        }

        try {
            var isAdShowing = false
            var earnedReward = false
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!isAdShowing) {
                    AdLoadingDialog.dismiss()
                    activity.runOnUiThread { onFailed?.invoke() }
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 8000L)

            AdLoadingDialog.show(activity)
            com.cscmobi.libraryads.ads.reward_ads.CSCReward.loadAndShowRewardAd(
                activity = activity,
                rewardId = adUnitId,
                timeOut = 0L,
                onStartLoading = null,
                onFinishLoading = null,
                onShowSuccess = {
                    isAdShowing = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    AdLoadingDialog.dismiss()
                },
                onUserEarnedReward = {
                    earnedReward = true
                },
                onLoadFailed = {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    AdLoadingDialog.dismiss()
                    activity.runOnUiThread { onFailed?.invoke() }
                },
                nextAction = {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    AdLoadingDialog.dismiss()
                    if (earnedReward) {
                        setRewardWatched()
                        activity.runOnUiThread {
                            onEarnedReward()
                        }
                    } else {
                        activity.runOnUiThread {
                            onFailed?.invoke()
                        }
                    }
                },
                reload = true,
                showLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in showReward", e)
            AdLoadingDialog.dismiss()
            activity.runOnUiThread { onFailed?.invoke() }
        }
    }

    /**
     * Show Native Banner at the top of landscape screens (DJ mixer & recording).
     * Attempts Native Banner first, falling back to Adaptive Banner seamlessly.
     */
    fun showNativeBanner(
        activity: Activity,
        container: ViewGroup?,
        placementKey: String = "native_banner",
        adUnitId: String = activity.getString(R.string.native_banner),
        isEnabled: Boolean = RemoteConfigs.native_banner,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (container == null) return
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            container.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        loadAndShowNative(
            activity = activity,
            container = container,
            placementKey = placementKey,
            adUnitId = adUnitId,
            layoutRes = R.layout.layout_native_banner,
            isEnabled = isEnabled,
            isShowLoading = true,
            onLoaded = onLoaded,
            onFailed = {
                // In case adUnitId is configured as a banner ad unit, fallback gracefully to adaptive banner
                showBanner(
                    activity = activity,
                    container = container,
                    adUnitId = adUnitId,
                    isEnabled = isEnabled,
                    isCollapsible = false,
                    onLoaded = onLoaded,
                    onFailed = onFailed
                )
            }
        )
    }

    /**
     * Open Google Mobile Ads Inspector (Next-Gen GMA SDK).
     * Controlled dynamically via RemoteConfigs.enable_ad_inspector.
     */
    fun openAdInspector(activity: Activity, onResult: ((String?) -> Unit)? = null) {
        if (!RemoteConfigs.enable_ad_inspector) {
            val msg = "Ad Inspector is disabled via Remote Config (enable_ad_inspector = false)"
            Log.d(TAG, msg)
            activity.runOnUiThread {
                android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_SHORT).show()
                onResult?.invoke(msg)
            }
            return
        }

        try {
            Log.d(TAG, "Opening Ad Inspector via Next-Gen GMA SDK...")
            activity.runOnUiThread {
                android.widget.Toast.makeText(activity, "Opening Ad Inspector...", android.widget.Toast.LENGTH_SHORT).show()
            }
            com.google.android.libraries.ads.mobile.sdk.MobileAds.openAdInspector { error ->
                if (error != null) {
                    val msg = "AdInspector error: ${error.message} (code: ${error.code})"
                    Log.e(TAG, msg)
                    activity.runOnUiThread {
                        android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_LONG).show()
                        onResult?.invoke(msg)
                    }
                } else {
                    Log.d(TAG, "AdInspector closed successfully")
                    activity.runOnUiThread {
                        onResult?.invoke(null)
                    }
                }
            }
        } catch (e: Exception) {
            val msg = "AdInspector exception: ${e.message}"
            Log.e(TAG, msg, e)
            activity.runOnUiThread {
                android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_LONG).show()
                onResult?.invoke(msg)
            }
        }
    }
}
