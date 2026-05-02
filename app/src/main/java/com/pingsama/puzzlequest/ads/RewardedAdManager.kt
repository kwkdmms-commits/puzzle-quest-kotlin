package com.pingsama.puzzlequest.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages rewarded ad loading and display for Hint and More Time power-ups.
 *
 * Rules:
 * - Show rewarded ad first when Hint or More Time is pressed
 * - Only grant reward if user watches ad completely
 * - Do NOT grant reward if user closes ad early
 * - Preload when GameScreen opens
 * - Preload next ad after one is shown
 * - Never crash if ad fails
 */
object RewardedAdManager {
    private const val TAG = "REWARDED"
    private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var rewardEarned = false

    /**
     * Preload rewarded ad
     */
    fun preloadRewardedAd(activity: Activity) {
        if (isLoading || rewardedAd != null) return

        isLoading = true
        Log.d(TAG, "REWARDED_LOADING")

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            TEST_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    super.onAdLoaded(ad)
                    rewardedAd = ad
                    isLoading = false
                    rewardEarned = false
                    Log.d(TAG, "REWARDED_LOADED")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    rewardedAd = null
                    isLoading = false
                    Log.e(TAG, "REWARDED_FAILED: ${adError.message}")
                }
            }
        )
    }

    /**
     * Show rewarded ad and call callback only if reward is earned
     */
    fun showRewardedAdIfReady(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdNotReady: () -> Unit,
    ) {
        if (rewardedAd == null) {
            Log.d(TAG, "Ad not loaded")
            onAdNotReady()
            return
        }

        Log.d(TAG, "REWARDED_SHOWING")
        rewardEarned = false

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "REWARDED_DISMISSED")
                rewardedAd = null
                // Only call onRewardEarned if reward was actually earned
                if (rewardEarned) {
                    onRewardEarned()
                }
                // Preload next ad
                preloadRewardedAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "REWARDED_FAILED_TO_SHOW: ${adError.message}")
                rewardedAd = null
                onAdNotReady()
                // Preload next ad
                preloadRewardedAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "REWARDED_SHOWED_FULL_SCREEN")
            }
        }

        rewardedAd?.show(activity) { reward ->
            Log.d(TAG, "REWARDED_EARNED: ${reward.amount} ${reward.type}")
            rewardEarned = true
        }
    }

    /**
     * Check if rewarded ad is loaded
     */
    fun isAdLoaded(): Boolean = rewardedAd != null

    /**
     * Reset state
     */
    fun reset() {
        rewardedAd = null
        isLoading = false
        rewardEarned = false
        Log.d(TAG, "RewardedAdManager reset")
    }
}
