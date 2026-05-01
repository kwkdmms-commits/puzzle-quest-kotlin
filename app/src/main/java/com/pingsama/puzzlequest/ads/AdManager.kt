package com.pingsama.puzzlequest.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

/**
 * Manages AdMob banner ads for the game with fail-safe error handling.
 * Uses Google test ad IDs for development/testing.
 * 
 * CRITICAL: All ad operations must be fail-safe.
 * If ads fail, the game continues running normally.
 */
object AdManager {
    private const val TAG = "AdManager"
    
    // Google test banner ad unit ID (use this for development)
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    
    // Track if ads are available
    private var adsAvailable = false
    
    /**
     * Initialize Mobile Ads SDK.
     * Call this once during app startup.
     * FAIL-SAFE: Never throws, always logs errors.
     */
    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context)
            adsAvailable = true
            Log.d(TAG, "Mobile Ads SDK initialized successfully")
        } catch (e: Throwable) {
            // Catch all exceptions including runtime errors
            adsAvailable = false
            Log.e(TAG, "Failed to initialize Mobile Ads SDK: ${e.message}", e)
            // App continues running even if ads fail
        }
    }
    
    /**
     * Load a banner ad into the provided AdView.
     * FAIL-SAFE: Never throws, always logs errors.
     * Safe to call even if ads are disabled or network is unavailable.
     */
    fun loadBannerAd(adView: AdView) {
        try {
            if (!adsAvailable) {
                Log.d(TAG, "Ads not available, skipping banner load")
                return
            }
            
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            Log.d(TAG, "Banner ad load requested")
        } catch (e: Throwable) {
            // Catch all exceptions including runtime errors
            Log.e(TAG, "Failed to load banner ad: ${e.message}", e)
            // App continues running even if ad load fails
        }
    }
    
    /**
     * Get the test banner ad unit ID.
     * Replace with your real ad unit ID in production.
     */
    fun getBannerAdUnitId(): String = TEST_BANNER_AD_UNIT_ID
    
    /**
     * Get the standard banner ad size.
     */
    fun getBannerAdSize(): AdSize = AdSize.BANNER
    
    /**
     * Check if ads are available.
     */
    fun areAdsAvailable(): Boolean = adsAvailable
}
