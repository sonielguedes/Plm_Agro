package com.soniel.plmagro.core.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class AlertManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true
            }
        }
    }

    fun speak(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun playSpeedAlert() {
        // Alerta sonoro
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)

        // Vibração: padrão de pulso curto e forte
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    fun playGeofenceEntry() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    /**
     * Alerta Crítico: GPS Travado ou Falha de Hardware.
     * Som longo e vibração intensa.
     */
    fun playCriticalAlert() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    /**
     * Aviso Operacional: Pedir motivo de parada ou sugestão de Geofence.
     * Bipe duplo e vibração curta.
     */
    fun playOperationalNotice() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}
