# SONIEL.md — IDENTIDADE OFICIAL DO PROJETO PLMAGRO

# RESPONSÁVEL PRINCIPAL

Nome:
Soniel Guedes

Cargo no Projeto:
Project Owner & Lead Architect

Especialidade:
- Android Industrial
- Telemetria Offline-First
- Wialon IPS 2.2
- Sistemas Operacionais Agrícolas
- Arquitetura Resiliente
- FSM Operacional
- Replay Offline Industrial

---

# VISÃO DO PROJETO

O PLMAGRO NÃO é um aplicativo comum.

O sistema é uma plataforma operacional agrícola industrial com:
- telemetria realtime;
- replay offline;
- rastreamento resiliente;
- FSM operacional;
- recuperação automática;
- sobrevivência pós-reboot;
- integração Wialon IPS 2.2.

Objetivo:
Operar em:
- colhedoras;
- caminhões;
- transbordos;
- veículos agrícolas;
- ambientes sem internet;
- operação contínua 24/7.

---

# FILOSOFIA DO PROJETO

A operação NÃO pode depender:
- da internet;
- do operador;
- do Android;
- do sinal;
- do servidor.

O sistema deve sobreviver sozinho.

---

# PADRÃO DE QUALIDADE

Toda alteração deve:
- preservar arquitetura;
- preservar replay;
- preservar offline-first;
- preservar UUID global;
- preservar cronologia operacional;
- preservar integridade Room;
- preservar telemetria IPS;
- preservar FSM.

---

# REGRAS ABSOLUTAS

NUNCA:
- quebrar Wialon IPS;
- remover Outbox;
- remover replay;
- remover WAL;
- criar mock em produção;
- misturar HTTP com IPS;
- criar duplicidade operacional;
- quebrar jornada ativa;
- ignorar ACK IPS;
- remover ForegroundService.

---

# ARQUITETURA OFICIAL

Stack:
- Kotlin
- Jetpack Compose
- MVVM
- Room
- Coroutines
- ForegroundService
- BootReceiver
- Wialon IPS 2.2
- Remote API Wialon

---

# OBJETIVO FINAL

Transformar o PLMAGRO em:

“Terminal Telemétrico Operacional Agrícola Industrial Offline-First”

Capaz de:
- sobreviver reboot;
- sobreviver reconnect;
- sobreviver process death;
- sobreviver áreas sem sinal;
- operar continuamente em campo.

---

# DIRETRIZ PARA IA E DESENVOLVEDORES

Antes de qualquer alteração:
1. Ler TODOS os arquivos .md da pasta /docs.
2. Validar impacto arquitetural.
3. NÃO criar regressão.
4. Priorizar estabilidade sobre feature nova.
5. Priorizar resiliência sobre estética.
6. Priorizar operação real sobre mock/demo.

---

# MANTRA DO PROJETO

“O operador trabalha.
O sistema sobrevive.”
