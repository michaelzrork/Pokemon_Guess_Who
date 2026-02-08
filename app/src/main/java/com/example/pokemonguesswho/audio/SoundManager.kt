package com.example.pokemonguesswho.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

class SoundManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Note: Add sound files to res/raw/ directory
    // card_flip.mp3, eliminate.mp3, win.mp3, game_over.mp3
    
    fun playCardFlip() {
        // Uncomment when sound files are added
        // playSound(R.raw.card_flip)
    }
    
    fun playCardEliminate() {
        // Uncomment when sound files are added
        // playSound(R.raw.eliminate)
    }
    
    fun playGameWin() {
        // Uncomment when sound files are added
        // playSound(R.raw.win)
    }
    
    fun playGameOver() {
        // Uncomment when sound files are added
        // playSound(R.raw.game_over)
    }
    
    private fun playSound(soundResId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, soundResId).apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}
