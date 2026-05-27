package com.soniel.plmagro.core.config

import com.soniel.plmagro.BuildConfig

/**
 * Configuração de Produção Oficial do PLMAGRO
 * Terminal Telemétrico Operacional Agrícola
 */
object AppConfig {
    
    val centralApiUrl: String = "https://plmview.plmagro.com.br/"

    /**
     * Em produção oficial, logs sensíveis são desativados.
     * Logs industriais de FSM e Outbox permanecem via IndustrialLogger se necessário.
     */
    val isLoggingEnabled: Boolean = BuildConfig.DEBUG
}
