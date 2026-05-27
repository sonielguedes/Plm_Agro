# ⚙️ MÁQUINA DE ESTADO OPERACIONAL (FSM)

## Definição
A FSM do PLMAGRO centraliza a lógica de negócios e garante que o veículo/operador siga um fluxo operacional consistente e validado.

---

## 🏁 Estados Oficiais

### 1. SEM_JORNADA
- Estado inicial.
- Telemetria IPS inativa.
- Apenas funções administrativas liberadas.

### 2. OPERANDO
- Veículo em atividade produtiva.
- Velocidade > 5km/h detectada ou operação manual iniciada.
- Telemetria de rastro em alta frequência.

### 3. PARADO
- Inatividade detectada (Velocidade < 1km/h por mais de 5 min).
- O sistema aguarda o apontamento do motivo da parada.
- Alertas de voz solicitam interação do operador.

### 4. PARADA_APONTADA
- O operador informou o motivo (ex: Mecânica, Refeição).
- Telemetria entra em modo de economia (apenas batimento cardíaco).

### 5. MANUTENÇÃO
- Estado de serviço técnico.
- Alertas de produtividade silenciados.

### 6. FINALIZADA
- Jornada encerrada.
- Bloqueio de novos registros até nova jornada.

---

## ⚡ Gatilhos e Transições

| De | Para | Gatilho |
| :--- | :--- | :--- |
| SEM_JORNADA | OPERANDO | `JourneyStarted` |
| OPERANDO | PARADO | `Speed < 1km/h` por 5 min |
| PARADO | OPERANDO | `Speed > 10km/h` (Retomada Automática) |
| PARADO | PARADA_APONTADA | `Motivo Informado` pelo operador |
| PARADA_APONTADA | OPERANDO | `Speed > 10km/h` ou `Botão Iniciar` |
| QUALQUER | ERRO_GPS | `SensorWatchdog` detecta falha total |

---

## 🏛 Implementação Técnica
- **Localização:** `com.soniel.plmagro.core.fsm.MaquinaEstadoOperacional`
- **Reatividade:** O estado é exposto via `StateFlow` e consumido por toda a UI.
- **Persistência:** Toda transição de estado é registrada como um evento na tabela `events` e sincronizada via Outbox.
