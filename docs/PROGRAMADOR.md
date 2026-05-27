# PROGRAMADOR.md — GUIA OFICIAL DE ENTRADA NO PROJETO PLMAGRO

> **Nota Arquitetural:** Este guia foca em implementação e tokens. Para detalhes de design, consulte [ARQUITETURA.md](./ARQUITETURA.md), [OFFLINE_FIRST.md](./OFFLINE_FIRST.md) e [IPS_PROTOCOL.md](./IPS_PROTOCOL.md).

# ACESSO WIALON

## TOKEN OFICIAL REMOTE API

TOKEN:
a61d89834829125fbc5adc9f6c4b7331130B24B26A4FD4D8E95416A2F08B4386FA57F822

IMPORTANTE:
- NUNCA logar token.
- NUNCA expor token em Toast.
- NUNCA commitar token em repositório público.
- NUNCA colocar token hardcoded em múltiplos arquivos.

O token deve ser utilizado:
- somente via camada segura;
- EncryptedSharedPreferences;
- BuildConfig;
- Configuração Administrativa protegida.

---

# LOGIN REMOTE API

Endpoint oficial:

https://hst-api.wialon.com/wialon/ajax.html

Fluxo correto:

1. token/login
2. receber eid
3. armazenar eid temporário
4. utilizar eid nas chamadas
5. renovar automaticamente se expirar

Exemplo:

svc=token/login
params={"token":"TOKEN_AQUI"}

---

# ARQUITETURA OFICIAL

O PLMAGRO NÃO é um app comum.

O sistema é:
- telemetria industrial;
- offline-first;
- resiliente;
- integrado ao Wialon IPS 2.2.

Objetivo:
- rastreamento;
- jornadas;
- paradas;
- replay offline;
- sincronização industrial.

---

# STACK OFICIAL

- Kotlin
- Jetpack Compose
- MVVM
- StateFlow
- Room
- Coroutines
- ForegroundService
- BootReceiver
- Wialon IPS 2.2
- Remote API Wialon

---

# REGRAS ABSOLUTAS

## OFFLINE-FIRST

Toda operação deve funcionar:
- sem internet;
- sem API;
- sem Wialon.

Fluxo:
persistir local → Outbox → replay posterior.

---

## IPS PURO

Telemetria usa:
- Socket TCP puro;
- java.net.Socket;
- protocolo Wialon IPS 2.2.

NÃO usar:
- Retrofit no IPS;
- HTTP no IPS;
- MQTT para telemetria principal.

---

# MÓDULOS CRÍTICOS

NÃO quebrar:

- WialonIpsClient
- WialonIpsProtocol
- OutboxManager
- TelemetryForegroundService
- MainViewModel recovery
- BootReceiver
- FSM operacional
- Room migrations

---

# FSM OPERACIONAL

Estados oficiais:

- OPERANDO
- PARADO
- PARADA_APONTADA
- AGUARDANDO
- MANUTENCAO

Toda transição:
- persistida;
- sincronizada;
- idempotente.

---

# JORNADA

Regra:
- apenas 1 jornada ativa.

Ao abrir app:
- recuperar jornada automaticamente;
- NÃO criar duplicidade.

---

# OUTBOX

Todo evento:
- UUID global;
- replay cronológico;
- retry seguro;
- idempotência.

---

# LOGS OFICIAIS

Usar:

- IPS_LOGIN_SENT
- IPS_LOGIN_ACK
- IPS_PACKET_SENT
- IPS_PACKET_ACK
- IPS_PACKET_ERROR
- REPLAY_STARTED
- REPLAY_SUCCESS
- HEARTBEAT_SENT
- JORNADA_RECUPERADA

NÃO logar:
- token;
- senha;
- eid.

---

# SEGURANÇA

Obrigatório:

- HTTPS
- allowBackup=false
- EncryptedSharedPreferences
- release minifyEnabled true
- shrinkResources true

---

# BUILD OFICIAL

Gerar somente:

build-oficial-release.bat

APK:
dist/PLMAGRO-OFICIAL-release.apk

---

# O QUE NÃO FAZER

❌ remover Outbox  
❌ remover replay  
❌ remover WAL  
❌ remover UUID  
❌ criar mock  
❌ misturar HTTP com IPS  
❌ ignorar ACK IPS  
❌ duplicar jornada  
❌ salvar sem transaction  

---

# TESTE OBRIGATÓRIO

1. abrir jornada
2. fechar app
3. recuperar jornada
4. desligar internet
5. gerar backlog
6. reboot aparelho
7. religar internet
8. validar replay
9. validar Wialon
10. validar sem duplicidade

---

# FILOSOFIA DO SISTEMA

O operador não deve depender da tecnologia.

A tecnologia deve sobreviver sozinha.
