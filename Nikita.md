# Orientações Técnicas e Decisões de Arquitetura - Wialon IPS

Este documento registra as recomendações técnicas da Wialon (Nikita Sologub) e a decisão estratégica da equipe PLMAGRO.

## Histórico de Comunicação

### 1. Recomendações de Nikita Sologub (Wialon Support L2)
Nikita sugeriu duas abordagens para a integração:
- **Wialon IPS v.2.2:** Implementação direta do protocolo via TCP/IP. [Documentação PDF](https://help.wialon.com/app/locale/en/spaces/wialon-hosting/user-guide/hardware/assets/wialon-ips-v-2-2-en.pdf)
- **WiaTagKit:** Biblioteca pronta para criação de apps similares ao WiaTag.

### 2. Decisão Técnica PLMAGRO (Soniel Guedes)
**Opção Escolhida:** Implementação direta via protocolo **Wialon IPS 2.2**.

#### Justificativa da Decisão
A arquitetura foi projetada para operações agrícolas industriais que exigem comportamento *offline-first* e alta resiliência. A implementação nativa permite controle total sobre:
- Ciclo de vida do socket TCP.
- Geração manual de pacotes (`#L#`, `#D#`).
- Sincronização em lote (*batching*) e replay offline cronológico.
- Entrega idempotente de eventos e lógica anti-duplicidade (UUID global).
- Serviços de telemetria em foreground e persistência de jornada ativa.
- Recuperação automática pós-reboot ou encerramento do processo.

## Status da Implementação Atual ✅
A implementação foi **concluída e validada em conformidade 100% com o protocolo IPS 2.2**.
- **Login:** Handshake `#L#` -> `#AL#1` totalmente funcional com tratamento de erros.
- **Pacotes de Dados:** Estrutura `#D#` de 16 campos implementada (incluindo HDOP, I/O e ADC).
- **Socket TCP:** Conexão persistente com timeouts industriais e gestão de streams.
- **ACKs:** Validação rigorosa de `#AD#1` (Sucesso), `#AD#0` (Rejeição) e `#AD#-1` (Erro de Estrutura).
- **Offline:** Replay cronológico garantido via OutboxManager.

---
**Contatos:**
- **Wialon:** Nikita Sologub (Technical Care Engineer L2)
- **PLMAGRO:** Soniel Guedes
