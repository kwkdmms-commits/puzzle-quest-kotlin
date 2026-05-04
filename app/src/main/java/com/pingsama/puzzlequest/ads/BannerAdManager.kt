package com.pingsama.puzzlequest.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdListener

/**
 * Manages AdMob banner ads with delayed load and retry logic.
 * 
 * Features:
 * - Waits 2 seconds before initial load (allows network to stabilize)
 * - Retries up to 3 times on network failure
 * - 30-second delay between retries
 * - Never shows error messages to players
 * - Keeps banner area empty if all retries fail
 * - Fail-safe: never crashes
 */
object BannerAdManager {
    private const val TAG = "BannerAdManager"
    
    // Production banner ad unit ID
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-4699326641068010/2797397808"
    
    // Timing constants (in milliseconds)
    private const val INITIAL_DELAY_MS = 2000L      // 2 seconds before first load
    private const val RETRY_DELAY_MS = 30000L       // 30 seconds between retries
    private const val MAX_RETRIES = 3
    
    // State tracking per AdView
    private val adViewStates = mutableMapOf<AdView, AdViewState>()
    
    private data class AdViewState(
        val context: Context,
        var retryCount: Int = 0,
        var isLoading: Boolean = false,
        var loadScheduled: Boolean = false,
        var retryScheduled: Boolean = false,
    )
    
    /**
     * Start loading banner ad with delayed load and retry logic.
     * Call this when BannerAd composable is created.
     * 
     * FAIL-SAFE: Never throws, always logs errors.
     */
    fun startLoadingBanner(adView: AdView) {
        try {
            val context = adView.context
            
            // Initialize state for this AdView
            if (!adViewStates.containsKey(adView)) {
                adViewStates[adView] = AdViewState(context)
            }
            
            val state = adViewStates[adView] ?: return
            
            // Set up the ad listener
            adView.adListener = createAdListener(adView)
            
            // Set ad unit ID
            adView.adUnitId = TEST_BANNER_AD_UNIT_ID
            
            // Schedule initial load after 2 seconds
            if (!state.loadScheduled) {
                state.loadScheduled = true
                Log.d(TAG, "BANNER_LOAD_SCHEDULED: Initial load in ${INITIAL_DELAY_MS}ms")
                
                adView.postDelayed({
                    loadBannerAd(adView)
                }, INITIAL_DELAY_MS)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error in startLoadingBanner: ${e.message}", e)
        }
    }
    
    /**
     * Load banner ad (called after delay or retry).
     */
    private fun loadBannerAd(adView: AdView) {
        try {
            val state = adViewStates[adView] ?: return
            
            if (state.isLoading) {
                Log.d(TAG, "Ad already loading, skipping")
                return
            }
            
            state.isLoading = true
            Log.d(TAG, "BANNER_LOAD_START: Attempt ${state.retryCount + 1}/$MAX_RETRIES")
            
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Throwable) {
            Log.e(TAG, "Error in loadBannerAd: ${e.message}", e)
            val state = adViewStates[adView]
            if (state != null) {
                state.isLoading = false
            }
        }
    }
    
    /**
     * Create ad listener that handles load success/failure and retry logic.
     */
    private fun createAdListener(adView: AdView): AdListener {
        return object : AdListener() {
            override fun onAdLoaded() {
                try {
                    val state = adViewStates[adView] ?: return
                    state.isLoading = false
                    Log.d(TAG, "BANNER_LOADED: Successfully loaded after ${state.retryCount} retries")
                } catch (e: Throwable) {
                    Log.e(TAG, "Error in onAdLoaded: ${e.message}", e)
                }
            }
            
            override fun onAdFailedToLoad(adError: LoadAdError) {
                try {
                    val state = adViewStates[adView] ?: return
                    state.isLoading = false
                    
                    // Log error details for debugging
                    Log.e(TAG, "BANNER_LOAD_FAILED: Attempt ${state.retryCount + 1}/$MAX_RETRIES")
                    Log.e(TAG, "  Error Code: ${adError.code}")
                    Log.e(TAG, "  Error Domain: ${adError.domain}")
                    Log.e(TAG, "  Error Message: ${adError.message}")
                    
                    // Increment retry count
                    state.retryCount++
                    
                    // If we haven't exceeded max retries, schedule a retry
                    if (state.retryCount < MAX_RETRIES) {
                        Log.d(TAG, "BANNER_RETRY: Scheduling retry in ${RETRY_DELAY_MS}ms")
                        
                        if (!state.retryScheduled) {
                            state.retryScheduled = true
                            adView.postDelayed({
                                state.retryScheduled = false
                                loadBannerAd(adView)
                            }, RETRY_DELAY_MS)
                        }
                    } else {
                        Log.e(TAG, "BANNER_FAILED_PERMANENTLY: Max retries ($MAX_RETRIES) exceeded")
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error in onAdFailedToLoad: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Clean up state when AdView is destroyed.
     */
    fun cleanup(adView: AdView) {
        try {
            adViewStates.remove(adView)
            Log.d(TAG, "Cleaned up AdView state")
        } catch (e: Throwable) {
            Log.e(TAG, "Error in cleanup: ${e.message}", e)
        }
    }
    
    /**
     * Get the banner ad unit ID.
     */
    fun getBannerAdUnitId(): String = BANNER_AD_UNIT_ID
}
