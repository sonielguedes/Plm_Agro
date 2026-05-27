package com.soniel.plmagro.model

/**
 * Estados da Máquina/Operação para FSM Industrial
 */
enum class OperationalState {
    SEM_JORNADA,
    OPERANDO,           // Em trabalho/produção
    PARADO,             // Parada detectada (vazio)
    PARADA_APONTADA,    // Parada com motivo informado
    MANUTENCAO,         // Oficina/Manutenção
    AGUARDANDO,         // Aguardando logística/transbordo
    
    // Estados Legados/Auxiliares (Mantidos para compatibilidade)
    JORNADA_ATIVA,
    EM_MOVIMENTO,
    AGUARDANDO_PARADA,
    ABASTECENDO,
    OFFLINE_OPERACIONAL,
    ERRO_GPS,
    FINALIZANDO,
    FINALIZADA
}
