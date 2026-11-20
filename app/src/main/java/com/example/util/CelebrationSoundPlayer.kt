package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.R

/**
 * Plays celebratory chiptune effects and epic victory fanfare using Android MediaPlayer
 * backed by bundled WAV sound resources in res/raw/.
 */
object CelebrationSoundPlayer {

    private const val TAG = "CelebrationSoundPlayer"
    private var activePlayer: MediaPlayer? = null

    fun playLowerWeightChime(context: Context) {
        playRawResource(context, R.raw.sfx_lower_weight)
    }

    fun playMilestoneChime(context: Context) {
        playRawResource(context, R.raw.sfx_milestone_achievement)
    }

    fun playIdealWeightVictoryFanfare(context: Context) {
        playRawResource(context, R.raw.sfx_ideal_weight_epic)
    }

    private fun playRawResource(context: Context, rawResId: Int) {
        runCatching {
            activePlayer?.let { prev ->
                runCatching {
                    if (prev.isPlaying) prev.stop()
                    prev.release()
                }
            }
            val appCtx = context.applicationContext
            val player = MediaPlayer.create(appCtx, rawResId) ?: return
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setOnCompletionListener { mp ->
                runCatching { mp.release() }
                if (activePlayer === mp) {
                    activePlayer = null
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                runCatching { mp.release() }
                if (activePlayer === mp) {
                    activePlayer = null
                }
                true
            }
            activePlayer = player
            player.start()
        }.onFailure { err ->
            Log.w(TAG, "No se pudo reproducir efecto MediaPlayer", err)
        }
    }
}
