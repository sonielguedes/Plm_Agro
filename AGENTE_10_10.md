# 🤖 Agente PLMAGRO: Roteiro para o Nível 10/10

Este documento serve como a "Missão de Inteligência" do Agente para levar este aplicativo ao estado de arte em tecnologia agrícola. Cada item abaixo representa um salto de qualidade em **Automação**, **Confiabilidade** e **Segurança**.

---

## 🎯 Objetivo: Excelência Industrial
O objetivo é transformar o aplicativo de uma ferramenta de registro manual em um **assistente inteligente proativo**.

---

## 🚀 FASE 1: Inteligência de Campo (PRÓXIMO PASSO)
*   **Automação por Geofences:**
    *   *Ação:* Ao entrar em uma cerca geográfica, o app identifica o nome (ex: `[COLHEITA] Gleba 01`) e sugere automaticamente o código da operação.
    *   *Ganho:* Reduz 80% do esforço de digitação do operador.
*   **Alerta de Parada Inteligente (v3.2.1 - Concluído):**
    *   *Ação:* Monitorar inatividade e disparar alerta baseado no tempo configurado.

## 🛡️ FASE 2: Robustez e Hardware
*   **Watchdog de Sensores:**
    *   *Ação:* Monitorar se o GPS "congelou" (mesma coordenada por muito tempo com velocidade > 0) e reiniciar o provedor de localização.
*   **Gestão Térmica Ativa:**
    *   *Ação:* Notificar o operador caso o tablet ultrapasse 45°C, sugerindo mudar a posição do sol ou verificar a ventilação.
*   **Checklist de Partida:**
    *   *Ação:* Antes de iniciar a jornada, validar se há bateria suficiente e se o sinal GPS está com precisão menor que 10 metros.

## 📊 FASE 3: Visibilidade e Sincronização
*   **Sincronização Delta (v3.1.0 - Parcialmente Concluído):**
    *   *Ação:* Otimizar o download de cercas e motoristas para baixar apenas o que mudou.
*   **Dashboard de Saúde Técnica:**
    *   *Ação:* Uma tela onde o suporte técnico vê o status de todos os "motores" internos em um só lugar.

---

## 🛠 Comandos de Gestão do Agente
Sempre que o Agente atuar, ele deve seguir estas diretrizes:
1.  **Segurança Primeiro:** Nunca fazer mudanças que bloqueiem o uso offline.
2.  **Transparência:** Documentar cada commit no `MELHORIAS_EFETUADAS.md`.
3.  **Versionamento:** Manter o GitHub atualizado a cada marco concluído.

---

**Qual missão da FASE 1 você quer que o Agente inicie agora?**
*(Sugestão: Começar pela Automação por Geofences)*
