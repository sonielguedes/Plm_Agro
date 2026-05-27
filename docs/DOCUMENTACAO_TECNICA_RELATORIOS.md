# Documentação Técnica e Relatórios de Engenharia - PLMAGRO

Este documento detalha a arquitetura técnica, os motores de sincronização e os sistemas de relatórios implementados para garantir a operação industrial de alta disponibilidade.

## 1. Arquitetura de Sincronização (Sync Engine)

O sistema utiliza uma abordagem de **Outbox Pattern** para garantir que nenhum dado operacional seja perdido em áreas de sombra (sem cobertura de rede).

### 1.1. OutboxManager e WorkManager
*   **Funcionamento:** Cada evento gerado (telemetria, parada, jornada) é primeiro persistido localmente no banco de dados `Room` com status `PENDENTE`.
*   **Proatividade:** Ao inserir um evento, o `OutboxManager` dispara um `OneTimeWorkRequest` via **WorkManager**.
*   **Resiliência:** Caso a rede falhe, o sistema utiliza uma política de **Backoff Exponencial** (30s, 60s, 120s...) para tentar novamente, respeitando as restrições de conectividade do Android.
*   **Sincronização Periódica:** Um worker de fundo (`OutboxSyncWorker`) revisa toda a fila a cada 15 minutos como contingência.

### 1.2. Integridade de Dados
*   **SHA-256:** Todos os payloads de sincronização geram um hash de integridade no momento da criação para evitar corrupção de dados durante o transporte.
*   **Dead Letter Queue (DLQ):** Eventos que falham após 10 tentativas consecutivas são movidos para a `dead_letter_events` para auditoria manual, liberando a fila principal.

## 2. Integração Wialon Remote API

O aplicativo atua como um gateway inteligente entre o hardware e a plataforma Wialon.

### 2.1. Vínculo Inteligente de Frota
*   **Auto-preenchimento:** O sistema consome a API `core/search_item` para extrair:
    *   **Placa:** Mapeada dos campos `pht` ou `registration_plate`.
    *   **Tipo de Equipamento:** Mapeado do campo `vt` ou `vehicle_type`.
    *   **KM Inicial:** Sincronizado em tempo real no início da jornada para garantir paridade com o site.
*   **Sincronização de Motoristas:** Busca recursiva em todos os recursos (`avl_resource`) com remoção de cache para atualizações imediatas em campo.

## 3. Sistema de Relatórios e Telemetria

### 3.1. Relatórios Operacionais (PDF/Texto)
*   **Ficha de Trabalho:** O `ShareUtils` gera relatórios formatados contendo:
    *   Duração real do turno (Horas/Minutos).
    *   Distância acumulada via cálculos de Haversine (GPS).
    *   Contadores de abastecimento e áreas visitadas (Geofences).
*   **Exportação:** Suporte nativo para compartilhamento via WhatsApp (texto plano) e geração de documentos PDF oficiais (A4) para arquivamento.

### 3.2. Histórico de Eventos (Timeline)
*   **Categorização Visual:**
    *   **Produtivo (Verde):** Operação iniciada, movimento detectado, retomada.
    *   **Improdutivo/Alerta (Vermelho):** Paradas, excesso de velocidade, perda de sinal GPS.
    *   **Geográfico (Amarelo):** Entradas e saídas de cercas eletrônicas.

## 4. Gestão de Mensagens e Alertas

### 4.1. Comunicação Bidirecional
*   **Polling de Comandos:** O sistema verifica a fila de comandos Wialon a cada 60 segundos.
*   **Alertas TTS (Text-to-Speech):** Utilização do motor de voz do Android para anunciar mensagens da central e alertas de velocidade, permitindo que o operador mantenha os olhos na operação.
*   **Feedback Tátil:** Padrões de vibração distintos para diferentes severidades de eventos.

---

## 5. Resumo de Métricas de Desenvolvimento

*   **Linguagem:** Kotlin 2.0.21
*   **UI:** Jetpack Compose (Material 3)
*   **DI:** Hilt (Dagger) para Injeção de Dependências
*   **Banco de Dados:** Room (SQLite) com suporte a transações atômicas
*   **Versão Atual:** v4.9.0-RELEASE
*   **Ambiente:** Produção Industrial

---
*Relatório gerado em: 24/05/2026*
*Responsável Técnico: Assistente de Engenharia PLMAGRO*
