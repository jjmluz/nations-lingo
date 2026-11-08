package com.jjmluz.nationslingo

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes

enum class Sound(@param:RawRes val resource: Int) {
    MOVE(R.raw.move),
    START(R.raw.start),
    CORRECT(R.raw.correct),
    WRONG(R.raw.wrong),
    TICK(R.raw.tick),
    GAME_OVER(R.raw.game_over),
    VICTORY(R.raw.victory),
}

class Sounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = Sound.entries.associateWith { pool.load(context, it.resource, 1) }

    fun play(sound: Sound) {
        pool.play(ids.getValue(sound), 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
