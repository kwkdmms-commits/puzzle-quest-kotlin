package com.pingsama.puzzlequest

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.pingsama.puzzlequest.audio.AudioManager
import com.pingsama.puzzlequest.game.LeaderboardManager

/**
 * Process-wide owner of the audio subsystem and the SharedPreferences-backed
 * leaderboard. We deliberately keep this minimal — both objects are cheap to
 * hold for the app's lifetime and survive configuration changes.
 */
class PuzzleQuestApp : Application() {
    val audio: AudioManager by lazy { AudioManager() }
    val leaderboard: LeaderboardManager by lazy { LeaderboardManager(this) }

    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob once. Done on a background thread so it doesn't delay
        // the splash screen. The banner won't load until this completes, which is fine.
        MobileAds.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        audio.release()
    }
}
