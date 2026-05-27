# 🏛 ARQUITETURA OFICIAL - PLMAGRO

## Visão Geral
O PLMAGRO é projetado sob os princípios de **Resiliência Industrial** e **Offline-First**. A arquitetura garante que a telemetria e os apontamentos operacionais nunca sejam perdidos, independentemente da conectividade ou do ciclo de vida do processo Android.

---

## 🛠 Stack Tecnológica
- **Linguagem:** Kotlin
- **Interface:** Jetpack Compose (UI Reativa)
- **Persistência:** Room Database (WAL Mode habilitado)
- **Injeção de Dependência:** Dagger-Hilt
- **Assincronia:** Coroutines & Flow
- **Navegação:** Type-safe Navigation Compose
- **Comunicação:** 
  - **IPS:** Socket TCP Nativo (Protocolo 2.2)
  - **Remote API:** Retrofit + OkHttp

---

## 🛰 Camadas do Sistema

### 1. UI Layer (Compose + ViewModels)
- Gerencia o estado da interface usando `StateFlow`.
- Interage exclusivamente com os `Repositories`.
- Implementa o **Recovery de Jornada** na inicialização.

### 2. Domain/State Layer (FSM & EventBus)
- **FSM (MaquinaEstadoOperacional):** Controla as transições de estado (OPERANDO, PARADO, etc).
- **OperationalEventBus:** Barramento central de eventos em tempo real para comunicação desacoplada.
- **SensorWatchdog:** Monitora integridade de GPS e hardware.

### 3. Service Layer (Foreground Services)
- **TelemetryForegroundService:** Mantém o GPS e o Heartbeat ativos mesmo com o app em background ou tela desligada. Possui prioridade máxima no SO.
- **BootReceiver:** Garante a reinicialização automática dos serviços após reboot do dispositivo.

### 4. Data Layer (Repositories & Outbox)
- **PlmRepository:** Porta de entrada para persistência local (Journeys, Events, Geofences, OperationConfigs).
- **OutboxManager:** O "Coração Industrial". Gerencia a fila de sincronização, garante cronologia e implementa replay resiliente com backoff exponencial.

---

## 🔄 Fluxo de Dados (Data Flow)

### Fluxo de Telemetria (IPS)
`Sensor GPS` → `TelemetryService` → `PlmRepository (Room)` → `OutboxManager` → `WialonIpsClient (Socket TCP)` → `Wialon Server`

### Fluxo de Eventos (Remote API)
`Ação do Usuário` → `ViewModel` → `PlmRepository (Room Transaction)` → `OutboxManager` → `WialonRepository (Retrofit)` → `Wialon Server`

---

## 🛡 Garantias Arquiteturais
- **Idempotência:** Todo evento possui um UUID único gerado na origem.
- **Atomicidade:** Uso de Transações Room para garantir que o evento local e o Outbox sejam criados simultaneamente.
- **Persistência de Estado:** O estado da jornada é recuperado automaticamente em caso de Process Death.
