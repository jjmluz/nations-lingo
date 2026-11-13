package com.jjmluz.nationslingo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    private val usedKeys = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { NationsLingoApp(viewModel) }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }

    // Keys are handled here rather than through Compose focus: nothing on screen is focusable.
    // Holding a key only counts as one press, and a used key's release is swallowed with it.
    // Lint wrongly flags this override as restricted: androidx marks its own override of this public
    // Activity method as library-internal.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0 && onKey(keyCode)) usedKeys += keyCode
                if (keyCode in usedKeys) return true
            }
            KeyEvent.ACTION_UP -> if (usedKeys.remove(keyCode)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Returns true when the key was used, so it doesn't also reach the system. Back is a key on the TV
     * remote, not a gesture, and goes through here like the others so holding it counts as one press.
     */
    @SuppressLint("GestureBackNavigation")
    private fun onKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> viewModel.answer(Direction.UP)
        KeyEvent.KEYCODE_DPAD_DOWN -> viewModel.answer(Direction.DOWN)
        KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.answer(Direction.LEFT)
        KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.answer(Direction.RIGHT)
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A -> viewModel.confirm()
        KeyEvent.KEYCODE_BACK -> viewModel.back()
        else -> false
    }
}
