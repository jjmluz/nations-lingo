package com.jjmluz.nationslingo.wear

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jjmluz.nationslingo.GameViewModel
import com.jjmluz.nationslingo.Mode
import com.jjmluz.nationslingo.Screen
import kotlin.math.abs
import kotlin.math.sign

/** How far the crown scrolls, in dp, to move the highlight by one answer. */
private const val CROWN_STEP_DP = 36

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    private val crown = Crown()
    private val usedKeys = mutableSetOf<Int>()
    private var crownTravel = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp(viewModel, crown, onSelect = { select() }) }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }

    // The crown (or a rotating bezel) moves a highlight through the answers, like the TV's D-pad. It
    // arrives as scroll events, handled here before Compose so nothing needs focus.
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_SCROLL || !event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            return super.dispatchGenericMotionEvent(event)
        }
        // Turning clockwise scrolls down, which is a negative scroll value.
        val travel = -event.getAxisValue(MotionEvent.AXIS_SCROLL) *
            ViewConfiguration.get(this).scaledVerticalScrollFactor
        if (travel.sign != crownTravel.sign) crownTravel = 0f
        crownTravel += travel
        val step = CROWN_STEP_DP * resources.displayMetrics.density
        // One move per event at most, so a rotating bezel's click is always exactly one answer.
        if (abs(crownTravel) >= step) {
            val forward = crownTravel > 0
            crownTravel = 0f
            if (turnCrown(forward)) {
                window.decorView.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                    else HapticFeedbackConstants.CLOCK_TICK,
                )
            }
        }
        return true
    }

    private fun turnCrown(forward: Boolean): Boolean = when (viewModel.screen) {
        Screen.START -> {
            val before = viewModel.mode
            viewModel.chooseMode(if (forward) Mode.GUESS_COUNTRY else Mode.GUESS_FLAG)
            viewModel.mode != before
        }
        Screen.PLAYING -> {
            val round = viewModel.round
            round != null && viewModel.rightAnswer == null && crown.move(round, forward)
        }
        Screen.PAUSED, Screen.OVER -> false
    }

    // A side button, where the watch lets apps have one, picks the highlighted answer, as does the
    // one-handed gesture (see WearApp). Pixel Watches keep both their buttons for the system. Holding a button only counts as one press, as on TV.
    // Lint wrongly flags this override as restricted: androidx marks its own override of this public
    // Activity method as library-internal.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0 && keyCode in SELECT_KEYS && select()) usedKeys += keyCode
                if (keyCode in usedKeys) return true
            }
            KeyEvent.ACTION_UP -> if (usedKeys.remove(keyCode)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun select(): Boolean = when (viewModel.screen) {
        Screen.PLAYING -> {
            val round = viewModel.round
            val direction = round?.let(crown::highlighted)
            direction != null && viewModel.answer(direction)
        }
        else -> viewModel.confirm()
    }

    private companion object {
        val SELECT_KEYS = setOf(
            KeyEvent.KEYCODE_STEM_PRIMARY,
            KeyEvent.KEYCODE_STEM_1,
            KeyEvent.KEYCODE_STEM_2,
            KeyEvent.KEYCODE_STEM_3,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
        )
    }
}
