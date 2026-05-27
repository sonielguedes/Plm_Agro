# 🕵️ SUPERVISOR.md — BLUEPRINT ARQUITETURAL INDUSTRIAL

Este documento define as leis imutáveis e os padrões de engenharia para a criação de um novo sistema do zero, baseado na filosofia de **Resiliência Industrial** e **Offline-First**. 

Se você está iniciando um novo projeto, ignore os objetivos de negócio do PLMAGRO, mas herde rigorosamente esta estrutura técnica.

---

## 🏛️ 1. STACK TECNOLÓGICA OBRIGATÓRIA
Um sistema industrial não aceita instabilidade. A stack deve ser:
*   **Linguagem:** Kotlin (Coroutines + Flow para asincronia).
*   **UI:** Jetpack Compose (Estado reativo e previsível).
*   **DI:** Dagger-Hilt (Injeção de dependência estrita).
*   **DB:** Room com **WAL Mode** (Write-Ahead Logging) habilitado para concorrência de leitura/escrita.
*   **Network:** 
    *   API: Retrofit + OkHttp.
    *   Telemetria: Socket TCP Nativo (Sem overhead de HTTP).
*   **Async:** WorkManager para tarefas de background garantidas.

---

## 🛡️ 2. AS 5 LEIS DO DESENVOLVIMENTO RESILIENTE

### I. A Lei da Persistência Local Imediata
> "Nada toca a rede antes de tocar o disco."
Toda ação do usuário (cliques, registros, eventos) deve ser escrita em uma **Transação Atômica** no Banco de Dados local ANTES de qualquer tentativa de envio. O sucesso local é o único sucesso que importa para a UI.

### II. A Lei do Motor de Outbox
> "A rede é uma exceção, o Outbox é a regra."
Implemente um `OutboxManager` desacoplado que observa a tabela de pendências. Use **Backoff Exponencial** e **Dead Letter Queue (DLQ)** para gerenciar falhas sem bloquear o fluxo do usuário.

### III. A Lei da Sobrevivência (Foreground Service)
> "O sistema não morre com a tela."
Processos críticos (GPS, Telemetria, Sync) devem rodar em um `ForegroundService` com notificação persistente de alta prioridade. O app deve ser imune ao "OOM Killer" do Android.

### IV. A Lei da Idempotência Global
> "Duplicidade é erro de projeto."
Todo evento gerado deve possuir um **UUID (v4)** na origem. O servidor deve usar esse ID para descartar pacotes duplicados em caso de retentativas.

### V. A Lei do Estado Único (FSM)
> "O sistema sempre sabe onde está."
Utilize uma **Máquina de Estados Finita (FSM)** para gerenciar o estado da aplicação. Transições de estado devem ser validadas e persistidas instantaneamente.

---

## 🛰️ 3. PADRÕES DE COMUNICAÇÃO

### Telemetria Real-Time
*   Utilize pacotes binários ou strings leves via **Socket TCP**.
*   Implemente o protocolo de **ACK (Acknowledgment)**: cada pacote enviado deve aguardar uma confirmação do servidor para ser marcado como "Enviado".

### Sincronização Delta
*   Evite baixar dados completos. Compare Timestamps ou Hashes para baixar apenas o que mudou.
*   Cache persistente para dados de configuração (Motoristas, Áreas, Tabelas de Preço).

---

## 🔌 4. RECOVERY E AUTO-CURA (SELF-HEALING)
*   **BootReceiver:** O sistema deve iniciar seus serviços automaticamente após o reboot do dispositivo.
*   **Watchdogs:** Crie monitores que verificam se os sensores (GPS, Acelerômetro) estão "congelados" e reinicie-os se necessário.
*   **Integrity Hash:** Verifique a integridade dos pacotes no Outbox antes do envio.

---

## 🎨 5. UI/UX INDUSTRIAL
*   **Feedback Visual de Sincronização:** O usuário deve saber o que está pendente, sem ser bloqueado por isso.
*   **Zero Loading Spinners em Ações:** Como tudo é persistido localmente de forma instantânea, a UI deve responder imediatamente (Optimistic UI).
*   **Controle Térmico:** A interface deve monitorar a saúde do hardware (Bateria/Temperatura) para alertar o operador.

---

## 🛠️ 6. CHECKLIST DE BOOTSTRAP (NOVO PROJETO)
1. [ ] Setup **Hilt** Application e Entry Points.
2. [ ] Configurar **Room** com `@Transaction` e `WAL`.
3. [ ] Criar **OutboxManager** com loop de sincronização.
4. [ ] Implementar **ForegroundService** base.
5. [ ] Configurar **Timber/IndustrialLogger** para logs estruturados.
6. [ ] Criar **FSM** de estado operacional.
7. [ ] Implementar **BootReceiver** no Manifest.
8. [ ] Definir **NetworkSecurityConfig** (HTTPS obrigatório).

---
**Assinado:**
_Supervisor de Arquitetura Industrial_
