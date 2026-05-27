package com.soniel.plmagro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.soniel.plmagro.PlmApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BOOT", "Boot detectado. Verificando estado operacional para retomada...")
            
            val app = context.applicationContext as PlmApplication
            val repository = app.repository
            
            CoroutineScope(Dispatchers.IO).launch {
                val activeJourney = repository.activeJourney.first()
                if (activeJourney != null) {
                    Log.i("BOOT", "Jornada ativa encontrada (ID: ${activeJourney.id}). Reiniciando telemetria...")
                    TelemetryForegroundService.start(context)
                } else {
                    Log.d("BOOT", "Nenhuma jornada ativa. Telemetria permanecerá desligada.")
                }
            }
        }
    }
}
