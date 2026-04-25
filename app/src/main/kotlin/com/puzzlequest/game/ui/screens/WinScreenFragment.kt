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
import com.puzzlequest.game.data.LevelConfig
import com.puzzlequest.game.utils.PreferencesManager

class WinScreenFragment : Fragment() {
    private lateinit var preferencesManager: PreferencesManager
    private var currentLevel = 1
    private var timerSeconds = 0

    companion object {
        fun newInstance(level: Int, timer: Int): WinScreenFragment {
            return WinScreenFragment().apply {
                arguments = Bundle().apply {
                    putInt("level", level)
                    putInt("timer", timer)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentLevel = arguments?.getInt("level") ?: 1
        timerSeconds = arguments?.getInt("timer") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_win_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = (activity as MainActivity).preferencesManager

        val tvCongrats = view.findViewById<TextView>(R.id.tv_congrats)
        val tvTime = view.findViewById<TextView>(R.id.tv_time)
        val btnNextLevel = view.findViewById<Button>(R.id.btn_next_level)
        val btnRestart = view.findViewById<Button>(R.id.btn_restart)
        val btnHome = view.findViewById<Button>(R.id.btn_home)

        val minutes = timerSeconds / 60
        val seconds = timerSeconds % 60
        tvTime.text = "Time: ${String.format("%02d:%02d", minutes, seconds)}"

        preferencesManager.setLevelCompleted(currentLevel)

        btnNextLevel.setOnClickListener {
            if (currentLevel < LevelConfig.getTotalLevels()) {
                navigateToGameplay(currentLevel + 1)
            } else {
                // Game completed
                goHome()
            }
        }

        btnRestart.setOnClickListener {
            navigateToGameplay(currentLevel)
        }

        btnHome.setOnClickListener {
            goHome()
        }
    }

    private fun navigateToGameplay(level: Int) {
        preferencesManager.setCurrentLevel(level)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GameplayFragment.newInstance(level))
            .commit()
    }

    private fun goHome() {
        parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}
