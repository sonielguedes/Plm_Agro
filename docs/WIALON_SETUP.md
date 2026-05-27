# 🛰 CONFIGURAÇÃO OFICIAL UNIDADE WIALON IPS

Este guia descreve o procedimento obrigatório para configurar unidades no Wialon e no aplicativo PLMAGRO para garantir a compatibilidade 100% com o protocolo IPS 2.2.

---

## PASSO 1 — CRIAÇÃO DA UNIDADE NO WIALON

No painel de monitoramento do Wialon:
1. Clique em **Nova Unidade**.
2. **Nome:** Sugerido `PLMAGRO TESTE 01` (ou padrão da frota).
3. **Tipo de dispositivo:** Selecione obrigatoriamente **Wialon IPS**.

> ❌ **NÃO UTILIZAR:** Wialon IPS (flespi), flespi, HTTP, Teltonika ou WiaTag.

---

## PASSO 2 — CONFIGURAÇÃO DO SERVIDOR (HOST)

Endereço oficial para apontamento IPS:
- **IP:** `64.120.108.24`
- **Porta:** `20332`

---

## PASSO 3 — IDENTIFICAÇÃO (ID ÚNICO)

O **ID Único** (IMEI) configurado no Wialon deve ser **idêntico** ao ID configurado no aplicativo.
- **ID Exemplo:** `PLMAGRO001`

### Validação do Login:
O aplicativo enviará o pacote: `#L#PLMAGRO001;NA\r\n`
- Se o ID estiver divergente, o servidor recusará a conexão e a unidade permanecerá offline.

---

## PASSO 4 — CONFIGURAÇÃO NO APP PLMAGRO

No tablet/celular:
1. Vá em **Configurações Operacionais** (Senha administrativa necessária).
2. Na seção **GATEWAY WIALON IPS**:
   - **Host IPS:** `64.120.108.24`
   - **Porta IPS:** `20332`
3. No **Vínculo de Hardware**:
   - Certifique-se de que o **ID Único** seja `PLMAGRO001`.

---

## PASSO 5 — PROTOCOLO DE VALIDAÇÃO TÉCNICA

### 5.1. Handshake de Login
Verifique no Logcat as tags:
- `IPS_LOGIN_SENT`: Envio do ID Único.
- `IPS_LOGIN_ACK`: Resposta `#AL#1` (Sucesso) ou `#AL#0` (Rejeitado).

### 5.2. Telemetria em Tempo Real
Ao movimentar o veículo, verifique:
- `IPS_PACKET_SENT`: Pacote `#D#` enviado.
- `IPS_PACKET_ACK`: Resposta `#AD#1` (Aceito).

### 5.3. Teste Offline-First (Resiliência)
1. Desligue o tráfego de dados.
2. Realize manobras ou aguarde geração de rastro.
3. Religue a internet.
4. Valide se a trilha no Wialon é recomposta cronologicamente sem "pulos" ou duplicidade.

---

## RESULTADO ESPERADO
O PLMAGRO operando como um **Terminal Telemétrico Operacional Agrícola Industrial** de alta fidelidade via Wialon IPS 2.2.
