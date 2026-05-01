package com.pingsama.puzzlequest.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

/**
 * Manages AdMob banner ads for the game.
 * Uses Google test ad IDs for development/testing.
 */
object AdManager {
    private const val TAG = "AdManager"
    
    // Google test banner ad unit ID (use this for development)
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    
    /**
     * Initialize Mobile Ads SDK.
     * Call this once during app startup.
     */
    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context)
            Log.d(TAG, "Mobile Ads SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Mobile Ads SDK", e)
        }
    }
    
    /**
     * Load a banner ad into the provided AdView.
     * Safe to call even if ads are disabled or network is unavailable.
     */
    fun loadBannerAd(adView: AdView) {
        try {
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            Log.d(TAG, "Banner ad load requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load banner ad", e)
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
}
