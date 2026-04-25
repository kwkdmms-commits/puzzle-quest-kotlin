package com.puzzlequest.game

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puzzlequest.game.ui.screens.MainMenuFragment
import com.puzzlequest.game.utils.PreferencesManager

class MainActivity : AppCompatActivity() {
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferencesManager = PreferencesManager(this)

        if (savedInstanceState == null) {
            // Load main menu on first launch
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainMenuFragment())
                .commit()
        }
    }
}
