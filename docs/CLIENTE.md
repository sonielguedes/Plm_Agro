# 🚜 GUIA DE CONFIGURAÇÃO DO CLIENTE - PLMAGRO

Este guia descreve o passo a passo para configurar o terminal telemétrico **PLMAGRO** do zero até a operação 100% funcional no campo.

---

## 🏁 PASSO 1: Permissões Iniciais
Ao abrir o aplicativo pela primeira vez, ele solicitará permissões vitais. Sem elas, o rastreamento industrial não funcionará.
1.  **Localização (Sempre):** Permita o acesso à localização "O tempo todo". Isso garante que o rastro continue mesmo com a tela desligada.
2.  **Notificações:** Permita notificações para que o serviço de telemetria não seja encerrado pelo sistema Android.
3.  **Bateria:** Se o Android perguntar, selecione "Não otimizar" para o PLMAGRO.

---

## 🛰️ PASSO 2: Vínculo com a Unidade Wialon
O tablet precisa saber qual máquina ele está representando no servidor.
1.  Acesse o **Menu (Três Traços)** -> **Configurações**.
2.  Vá em **Vincular Frota**.
3.  O sistema carregará a lista de máquinas do Wialon. Selecione a máquina correspondente.
4.  Confirme o Código da Frota, Placa e Tipo de Veículo.
5.  Clique em **SALVAR VÍNCULO**. O ícone de API no topo deverá ficar verde (ONLINE).

---

## 🔌 PASSO 3: Configuração de Telemetria (IPS)
Para que a posição apareça no mapa em tempo real:
1.  Acesse **Configurações** -> **Admin IPS**.
2.  Verifique se o **Host** (ex: `193.193.165.165`) e a **Porta** (ex: `20332`) estão corretos.
3.  O **Unique ID** deve ser o mesmo cadastrado no Wialon (geralmente o IMEI ou ID único do tablet).
4.  Clique em **SALVAR CONFIGURAÇÃO**. O ícone de IPS no topo deverá mudar para ONLINE ou SYNCING.

---

## 🔄 PASSO 4: Sincronização de Banco de Dados
Para trabalhar offline, o app precisa baixar os dados do servidor.
1.  Acesse o **Menu** -> **Diagnóstico**.
2.  Clique no botão **CONEXÃO** para validar o acesso.
3.  Clique em **CERCAS** para baixar as áreas de trabalho.
4.  Clique em **MOTORISTAS** para baixar a lista de operadores autorizados.
5.  Dica: Use o botão **CloudSync** (nuvem no topo) para fazer tudo de uma vez.

---

## ⚙️ PASSO 5: Parâmetros Operacionais (Nível 10/10)
Configure a inteligência do sistema para ajudar o operador:
1.  **Manutenção Preventiva:** Em Configurações, defina o horímetro da próxima revisão e com quantas horas de antecedência o app deve avisar.
2.  **Auto-Parada:** Defina o tempo de inatividade (ex: 5 minutos). Se a máquina parar e o operador não apontar o motivo, o app falará: *"Por favor, informe o motivo da parada"*.
3.  **Modo Satelital:** Se estiver usando antena satelital cara, ative o **Modo Satelital** para economizar 90% dos dados.

---

## 🚜 PASSO 6: Início da Operação (Dia a Dia)
Com tudo configurado, o fluxo diário é simples:
1.  **Login:** O operador seleciona sua matrícula.
2.  **KM Inicial:** Informa o KM que está no painel da máquina.
3.  **Operação:** Seleciona o que vai fazer (Plantio, Colheita, etc).
4.  **Trabalho:** O sistema cuida do resto automaticamente.
5.  **Fim do Turno:** Clica em "Finalizar", confere o resumo e gera o **PDF de Troca de Turno**.

---

## 🛠️ RESOLUÇÃO DE PROBLEMAS
*   **API Vermelha:** Verifique a internet ou o Token no Wialon.
*   **IPS Vermelho:** Verifique o Unique ID e se a porta está aberta no servidor.
*   **GPS Sem Sinal:** Verifique se o tablet tem visão do céu ou se o GPS está ligado no Android.

---
**Suporte:** Entre em contato com a equipe técnica da PLMAGRO via Diário de Bordo.
