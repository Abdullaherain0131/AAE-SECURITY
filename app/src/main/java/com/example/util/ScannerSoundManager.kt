package com.example.util

import android.content.Context
import android.media.MediaPlayer
import com.example.R

object ScannerSoundManager {
    private var scanPlayer: MediaPlayer? = null
    private var threatPlayer: MediaPlayer? = null
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    fun startScanningSound() {
        if (scanPlayer?.isPlaying == true) return
        
        try {
            if (scanPlayer == null && context != null) {
                scanPlayer = MediaPlayer.create(context, R.raw.scanner)
                scanPlayer?.isLooping = true
                scanPlayer?.setVolume(0.3f, 0.3f)
            }
            scanPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopScanningSound() {
        try {
            if (scanPlayer?.isPlaying == true) {
                scanPlayer?.pause()
                scanPlayer?.seekTo(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun playThreatSound() {
        try {
            if (threatPlayer == null && context != null) {
                threatPlayer = MediaPlayer.create(context, R.raw.virus)
                threatPlayer?.setVolume(0.4f, 0.4f)
            }
            if (threatPlayer?.isPlaying == false) {
                threatPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        scanPlayer?.release()
        scanPlayer = null
        threatPlayer?.release()
        threatPlayer = null
        context = null
    }
}
