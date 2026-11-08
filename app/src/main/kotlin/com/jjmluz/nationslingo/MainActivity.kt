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
                if (event.repeatCount == 0 && viewModel.onKey(keyCode)) usedKeys += keyCode
                if (keyCode in usedKeys) return true
            }
            KeyEvent.ACTION_UP -> if (usedKeys.remove(keyCode)) return true
        }
        return super.dispatchKeyEvent(event)
    }
}
