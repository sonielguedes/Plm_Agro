# 🚀 MASTER PROMPT — INICIALIZAÇÃO DE PROJETO INDUSTRIAL

Copie e cole o texto abaixo em uma nova conversa com a IA para iniciar um projeto do zero com a mesma arquitetura resiliente do PLMAGRO.

---

## COPIAR ABAIXO DESTA LINHA
---

**Objetivo:** Iniciar um novo projeto Android do zero focado em **Operação Industrial Crítica**, seguindo a arquitetura **Offline-First** e **Resiliente**.

**Diretrizes de Arquitetura (Imutáveis):**
1. **Stack Técnica:** Kotlin, Jetpack Compose, Dagger-Hilt, Room (WAL Mode), Coroutines/Flow, Retrofit e Socket TCP Nativo para telemetria.
2. **Lei da Persistência Local:** Toda ação deve ser salva em uma @Transaction do Room local ANTES de tentar qualquer envio para a rede. A UI deve ser 100% otimista.
3. **Motor de Outbox:** Implementar um `OutboxManager` que gerencie a fila de sincronização com Backoff Exponencial e Dead Letter Queue (DLQ).
4. **Foreground Service:** O coração do app deve ser um `Service` de alta prioridade que sobreviva ao background e ao encerramento do processo.
5. **Máquina de Estados (FSM):** O fluxo do app deve ser controlado por uma FSM persistida para garantir que o estado (ex: Operando, Parado) sobreviva a reboots.
6. **Idempotência:** Todo evento deve ser gerado com um UUID v4 na origem.
7. **Boot Resilience:** Incluir `BootReceiver` para auto-inicialização pós-reboot.

**Instruções Iniciais:**
- Configure o `build.gradle.kts` com as dependências necessárias (Hilt, Room, Compose).
- Crie a estrutura de pastas: `api`, `core`, `di`, `model`, `service`, `ui`.
- Implemente o `IndustrialLogger` para rastreabilidade JSON.
- Comece pelo setup da Camada de Dados (Room + Repositories) antes de criar as telas.

**Mantra:** "O operador trabalha. O sistema sobrevive."

---
## FIM DO PROMPT
---

### Como usar:
1. Abra um novo chat com a IA.
2. Cole o texto acima.
3. No final do texto, adicione uma descrição do seu novo aplicativo (ex: "O aplicativo será para controle de estoque em minas subterrâneas sem internet").
