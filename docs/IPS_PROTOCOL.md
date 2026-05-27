# 🛰 PROTOCOLO WIALON IPS 2.2 - ESPECIFICAÇÃO INDUSTRIAL

## Visão Geral
A telemetria do PLMAGRO utiliza o protocolo **Wialon IPS v2.2** sobre **Socket TCP puro**. Esta escolha técnica visa a economia de dados e a resiliência em redes instáveis (GPRS/LTE/Satelital).

---

## 1. Handshake de Login (#L#)
Toda conexão deve ser iniciada com o pacote de login.

**Formato:** `#L#unique_id;password\r\n`
- `unique_id`: IMEI ou ID Único vinculado no sistema.
- `password`: Padrão "NA" ou configurado.

**Respostas:**
- `#AL#1`: Sucesso. Conexão mantida.
- `#AL#0`: Acesso Negado. Fechar socket.

---

## 2. Pacote de Dados (#D#)
Enviado periodicamente (Heartbeat) ou por mudança de posição/estado.

**Formato Oficial v2.2:**
`#D#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats;hdop;inputs;outputs;adc;ibutton;params\r\n`

**Campos Obrigatórios no PLMAGRO:**
- `date/time`: UTC (ddMMyy;HHmmss).
- `lat/lon`: Formato Wialon (DDMM.MMMM;N/S e DDDMM.MMMM;E/W).
- `speed/course/height`: Inteiros.
- `sats`: Quantidade de satélites em uso.
- `hdop`: Precisão horizontal.
- `params`: Parâmetros extras (batt, temp, disk, etc) no formato `name:type:value`.
  - Type 1: Int/Long
  - Type 2: Double
  - Type 3: String

---

## 3. Confirmações (ACK)
O servidor Wialon envia ACKs para cada pacote de dados.

- `#AD#1`: Pacote aceito e processado.
- `#AD#0`: Pacote rejeitado pelo servidor.
- `#AD#-1`: Erro de estrutura de dados (Protocol Error).

---

## 4. Gestão de Conexão
- **Timeout de Conexão:** 20 segundos.
- **Timeout de Leitura:** 15 segundos.
- **Keep-Alive:** Mantido via Heartbeat (60s padrão ou 300s em modo satelital).
- **Reconexão:** Em caso de erro, o socket é fechado e o `OutboxManager` retenta com backoff.

---

## 5. Modo Satelital (Otimização)
Quando ativado, o protocolo filtra telemetrias redundantes e envia apenas o estado mais recente no lote para reduzir o consumo de pacotes M2M.
- Redução de batimento cardíaco.
- Redução de precisão de rastro em favor de eventos de estado.
