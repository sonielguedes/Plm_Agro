# ADR 001: Implementação Nativa do Protocolo Wialon IPS 2.2

## Data: 2026-05-27
## Status: Aceito

---

## Contexto
O aplicativo PLMAGRO necessita enviar telemetria em tempo real para o servidor Wialon em ambientes agrícolas com baixa conectividade. As opções eram:
1. Usar a biblioteca WiaTagKit.
2. Usar Remote API (HTTP) para tudo.
3. Implementar nativamente o protocolo Wialon IPS 2.2 via Socket TCP.

## Decisão
Decidimos pela **Implementação Nativa do Protocolo IPS 2.2 via Socket TCP**.

## Justificativa
- **Economia de Dados:** O protocolo IPS via TCP é significativamente mais leve que o overhead de cabeçalhos HTTP.
- **Controle Total:** Permite gerenciar o ciclo de vida do socket, tempos de timeout industriais e lógica de reconexão específica para áreas de sombra rural.
- **Offline-First:** Facilita a implementação de Replay cronológico de pacotes binários/texto simples sem a complexidade de gerenciar estados HTTP de lote.
- **Recomendação Técnica:** Alinhado com as orientações de suporte nível 2 da Wialon (Nikita Sologub).

## Consequências
- **Esforço de Desenvolvimento:** Maior complexidade inicial para garantir conformidade com o formato de data, coordenadas e parâmetros do protocolo.
- **Resiliência:** Necessidade de um `OutboxManager` robusto para lidar com o buffer de dados offline.
- **Manutenibilidade:** Exige documentação clara do protocolo para futuros desenvolvedores (criado `IPS_PROTOCOL.md`).
