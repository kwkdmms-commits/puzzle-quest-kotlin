package com.puzzlequest.game.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.puzzlequest.game.MainActivity
import com.puzzlequest.game.R
import com.puzzlequest.game.utils.PreferencesManager

class MainMenuFragment : Fragment() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = (activity as MainActivity).preferencesManager

        val currentLevel = preferencesManager.getCurrentLevel()

        val playButton = view.findViewById<Button>(R.id.btn_play)
        val removeAdsButton = view.findViewById<Button>(R.id.btn_remove_ads)
        val quitButton = view.findViewById<Button>(R.id.btn_quit)

        playButton.text = getString(R.string.play, currentLevel)

        playButton.setOnClickListener {
            navigateToGameplay(currentLevel)
        }

        removeAdsButton.setOnClickListener {
            // Placeholder for remove ads
        }

        quitButton.setOnClickListener {
            activity?.finish()
        }
    }

    private fun navigateToGameplay(level: Int) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GameplayFragment.newInstance(level))
            .addToBackStack(null)
            .commit()
    }
}
