# 🔌 ESTRATÉGIA OFFLINE-FIRST E OUTBOX

## Princípio Fundamental
O PLMAGRO opera sob a premissa de que a conectividade é uma exceção, não a regra. O sucesso de uma operação de campo não depende da internet.

---

## 1. Persistência Local Imediata
Qualquer ação (Iniciar Jornada, Registrar Parada, Abastecimento) segue o fluxo:
1. Validação de dados na UI.
2. Início de **Transação Room**.
3. Escrita na tabela de domínio (ex: `TabelaParadas`).
4. Escrita na tabela `sync_outbox`.
5. Confirmação da transação.

**NUNCA** faça uma chamada de rede diretamente de um botão de ação sem antes persistir no Outbox.

---

## 2. O Motor de Outbox (OutboxManager)
O `OutboxManager` é um serviço desacoplado que observa o banco de dados.

### Características:
- **Cronologia Estrita:** Eventos são enviados na ordem em que foram criados (FIFO), preservando a linha do tempo da jornada.
- **Backoff Exponencial:** Em caso de falha, o intervalo de retentativa aumenta (5s, 10s, 20s... até 5 min) para evitar exaustão de bateria e rede.
- **Isolamento de Erro:** Se um evento crítico falha após X tentativas (ex: 10), ele é movido para a **Dead Letter Queue** para análise técnica, sem bloquear a fila principal.

---

## 3. Idempotência e UUID
Para evitar duplicidade em casos de *timeout* (onde o servidor recebe, mas o ACK não chega ao app):
- Cada evento possui um `eventId` (UUID v4).
- O servidor Wialon/Central utiliza este ID para descartar pacotes duplicados.

---

## 4. Replay Offline
Ao recuperar a conexão, o sistema inicia o **Replay Industrial**:
1. Prioriza eventos de mudança de estado (Início/Fim de Jornada).
2. Processa telemetria acumulada em lotes (batching) para eficiência de socket.
3. Notifica o `DiagnosticRepository` sobre o progresso.

---

## 5. Sobrevivência a Process Death e Reboot
- O `OutboxManager` reinicia automaticamente via `BootReceiver`.
- O estado `PENDENTE` no banco garante que nenhum dado seja perdido se o tablet for desligado subitamente.

---

## 6. Rastreamento Satelital Blindado (GPS Sem Internet)
A captura de telemetria não requer pacotes de dados. Ela é sustentada pelas seguintes fundações:
- **Prioridade de Alta Precisão:** O app solicita explicitamente o `Priority.PRIORITY_HIGH_ACCURACY`, forçando o Android a acordar a antena de GPS física do tablet e buscar a triangulação direto com satélites, sem depender do A-GPS de rede.
- **Gravação Silenciosa:** Cada coordenada capturada (mesmo 100% offline) é gravada na tabela de telemetria via `PlmRepository`. Caso a variação de ângulo ou distância seja alta o suficiente, um pacote `#D#` é gerado na tabela `sync_outbox`.
- **Injeção de Pós-Conexão:** Quando a internet for detectada, o *Replay Offline* agrupa todas essas milhares de coordenadas silenciosas e empurra para a plataforma Wialon. O rastro da viagem no mapa aparecerá intacto, com velocidades e acelerações preservadas.
