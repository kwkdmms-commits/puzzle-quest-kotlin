package com.puzzlequest.game.ui.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.puzzlequest.game.MainActivity
import com.puzzlequest.game.R
import com.puzzlequest.game.data.LevelConfig
import com.puzzlequest.game.engine.GameEngine
import com.puzzlequest.game.ui.views.PuzzleBoardView
import com.puzzlequest.game.utils.PreferencesManager

class GameplayFragment : Fragment() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var gameEngine: GameEngine
    private lateinit var puzzleBoard: PuzzleBoardView
    private var currentLevel = 1
    private var timerSeconds = 0
    private var timerRunnable: Runnable? = null
    private val timerHandler = Handler(Looper.getMainLooper())
    private var hintsUsed = 0
    private var moreTimeUsed = 0
    private var isGameActive = true

    companion object {
        fun newInstance(level: Int): GameplayFragment {
            return GameplayFragment().apply {
                arguments = Bundle().apply {
                    putInt("level", level)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentLevel = arguments?.getInt("level") ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gameplay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = (activity as MainActivity).preferencesManager

        val level = LevelConfig.getLevel(currentLevel)
        gameEngine = GameEngine(level)

        puzzleBoard = view.findViewById(R.id.puzzle_board)
        puzzleBoard.setGameEngine(gameEngine)
        puzzleBoard.setOnPieceLocked { checkLevelComplete() }

        val tvLevel = view.findViewById<TextView>(R.id.tv_level)
        val tvTimer = view.findViewById<TextView>(R.id.tv_timer)
        val tvPieces = view.findViewById<TextView>(R.id.tv_pieces)
        val btnHome = view.findViewById<Button>(R.id.btn_home)
        val btnRestart = view.findViewById<Button>(R.id.btn_restart)
        val btnMoreTime = view.findViewById<Button>(R.id.btn_more_time)
        val btnHint = view.findViewById<Button>(R.id.btn_hint)

        tvLevel.text = getString(R.string.level, currentLevel)

        btnHome.setOnClickListener { goHome() }
        btnRestart.setOnClickListener { showRestartConfirmation() }
        btnMoreTime.setOnClickListener { useMoreTime() }
        btnHint.setOnClickListener { showHint() }

        startTimer(tvTimer, tvPieces)
    }

    private fun startTimer(tvTimer: TextView, tvPieces: TextView) {
        // Create a runnable that updates the timer every 1 second
        timerRunnable = object : Runnable {
            override fun run() {
                if (isGameActive) {
                    timerSeconds++
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    tvTimer.text = getString(R.string.timer, minutes, seconds)
                }
                // Schedule the next update in 1 second
                timerHandler.postDelayed(this, 1000)
            }
        }

        // Start the timer
        timerHandler.postDelayed(timerRunnable!!, 1000)

        // Update pieces count
        tvPieces.text = getString(
            R.string.pieces_placed,
            gameEngine.getLockedPiecesCount(),
            gameEngine.getTotalPieces()
        )
    }

    private fun checkLevelComplete() {
        if (gameEngine.isLevelComplete()) {
            isGameActive = false
            stopTimer()
            showWinScreen()
        }
    }

    private fun stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable!!)
        }
    }

    private fun showWinScreen() {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                WinScreenFragment.newInstance(currentLevel, timerSeconds)
            )
            .commit()
    }

    private fun goHome() {
        parentFragmentManager.popBackStack()
    }

    private fun showRestartConfirmation() {
        // Show restart dialog
        val dialog = RestartConfirmationDialog {
            restartLevel()
        }
        dialog.show(parentFragmentManager, "restart_dialog")
    }

    private fun restartLevel() {
        gameEngine.resetLevel()
        timerSeconds = 0
        hintsUsed = 0
        moreTimeUsed = 0
        isGameActive = true
        puzzleBoard.invalidate()
    }

    private fun useMoreTime() {
        if (moreTimeUsed < 1) {
            moreTimeUsed++
            // More time logic (e.g., add 60 seconds)
        }
    }

    private fun showHint() {
        if (hintsUsed < 1) {
            hintsUsed++
            // Show hint dialog
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
    }
}
