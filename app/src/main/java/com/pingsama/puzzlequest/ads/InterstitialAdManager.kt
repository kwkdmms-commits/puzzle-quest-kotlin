package com.pingsama.puzzlequest.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages interstitial ad loading and display with strict show rules.
 *
 * Show rules:
 * - Show ONLY if totalGameplayTime >= 2 minutes OR screenChangeCount >= 3
 * - Never block gameplay if ad fails to load
 * - Reset counters after ad is shown
 */
object InterstitialAdManager {
    private const val TAG = "INTERSTITIAL"
    private const val TEST_AD_UNIT_ID = "ca-app-pub-4699326641068010/9947153726"
    private const val TIME_THRESHOLD_MS = 2 * 60 * 1000 // 2 minutes
    private const val SCREEN_CHANGE_THRESHOLD = 3

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    // Tracking variables
    var totalGameplayTime: Long = 0 // in milliseconds
    var screenChangeCount: Int = 0

    /**
     * Preload interstitial ad when entering a level
     */
    fun preloadInterstitial(activity: Activity) {
        if (isLoading || interstitialAd != null) return

        isLoading = true
        Log.d(TAG, "INTERSTITIAL_LOADING")

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            TEST_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    super.onAdLoaded(ad)
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "INTERSTITIAL_LOADED")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    interstitialAd = null
                    isLoading = false
                    Log.e(TAG, "INTERSTITIAL_FAILED: ${adError.message}")
                }
            }
        )
    }

    /**
     * Check if ad should be shown based on gameplay conditions
     */
    fun shouldShowAd(): Boolean {
        val timeCondition = totalGameplayTime >= TIME_THRESHOLD_MS
        val screenChangeCondition = screenChangeCount >= SCREEN_CHANGE_THRESHOLD

        return if (timeCondition || screenChangeCondition) {
            Log.d(TAG, "AD_CONDITION_MET: time=$totalGameplayTime, changes=$screenChangeCount")
            true
        } else {
            Log.d(TAG, "AD_CONDITION_SKIPPED: time=$totalGameplayTime, changes=$screenChangeCount")
            false
        }
    }

    /**
     * Show interstitial if loaded and conditions are met
     * Callback is invoked when ad is dismissed or failed to show
     */
    fun showInterstitialIfReady(
        activity: Activity,
        onAdDismissed: () -> Unit,
    ) {
        if (interstitialAd == null) {
            Log.d(TAG, "Ad not loaded, continuing immediately")
            onAdDismissed()
            return
        }

        Log.d(TAG, "INTERSTITIAL_SHOWING")

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "INTERSTITIAL_DISMISSED")
                interstitialAd = null
                resetCounters()
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "INTERSTITIAL_FAILED_TO_SHOW: ${adError.message}")
                interstitialAd = null
                resetCounters()
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "INTERSTITIAL_SHOWED_FULL_SCREEN")
            }
        }

        interstitialAd?.show(activity)
    }

    /**
     * Increment screen change counter (called on level complete, restart, next level)
     */
    fun recordScreenChange() {
        screenChangeCount++
        Log.d(TAG, "Screen change recorded: $screenChangeCount")
    }

    /**
     * Update total gameplay time (call from GameScreen timer)
     */
    fun updateGameplayTime(elapsedMs: Long) {
        totalGameplayTime += elapsedMs
    }

    /**
     * Reset counters after ad is shown
     */
    private fun resetCounters() {
        totalGameplayTime = 0
        screenChangeCount = 0
        Log.d(TAG, "Counters reset")
    }

    /**
     * Reset all state (call on app exit or level restart)
     */
    fun reset() {
        interstitialAd = null
        isLoading = false
        totalGameplayTime = 0
        screenChangeCount = 0
        Log.d(TAG, "InterstitialAdManager reset")
    }
}
