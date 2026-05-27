# Documentação de Telas - PLMAGRO

Este documento descreve as telas do aplicativo PLMAGRO, suas funções e o fluxo de operação para o usuário/operador.

---

## 1. Fluxo de Inicialização

### **Splash Screen (`SplashScreen.kt`)**
*   **O que é:** A primeira tela exibida ao abrir o app (Logo).
*   **Para que serve:** Realiza a inicialização do banco de dados local e verifica se há uma jornada ativa. Se houver, leva o usuário direto para a Dashboard. Caso contrário, segue para o Login ou Introdução.

### **Introdução de Permissões (`LocationPermissionIntroScreen.kt`)**
*   **O que é:** Tela explicativa sobre o uso do GPS.
*   **Para que serve:** Garante que o operador entenda por que o app precisa de localização em tempo real (telemetria industrial) e solicita as autorizações necessárias do Android.

---

## 2. Acesso e Configuração

### **Tela de Login (`LoginScreen.kt`)**
*   **O que é:** Portal de entrada para o operador.
*   **Para que serve:** Permite selecionar o nome do motorista (sincronizado do Wialon) e inserir a senha (último dígito da matrícula). Também dá acesso rápido às configurações pelo ícone de engrenagem.

### **Configurações Operacionais (`SettingsScreen.kt`)**
*   **O que é:** Painel administrativo e técnico.
*   **Para que serve:** 
    *   Definir código da frota, placa e tipo de equipamento.
    *   Vincular o tablet a uma unidade do Wialon.
    *   Configurar o Gateway IPS (IP e Porta).
    *   Realizar sincronização manual de motoristas e cercas geográficas.
    *   *Nota:* Algumas funções exigem senha administrativa.

---

## 3. Operação de Campo

### **Início de Jornada (`StartJourneyScreen.kt`)**
*   **O que é:** Formulário de abertura de turno.
*   **Para que serve:** Captura o KM inicial (pré-preenchido se houver vínculo), o código da operação e o centro de custo. Só após preencher estes dados a telemetria é ativada.

### **Dashboard (`DashboardScreen.kt`)**
*   **O que é:** A tela principal de trabalho.
*   **Para que serve:** 
    *   Exibir velocidade, KM atual, KM rodado e sinal de satélite.
    *   Mostrar o status industrial atual (OPERANDO, PARADO, MANUTENÇÃO, etc.).
    *   Fornecer botões de ação rápida para trocar de operação ou registrar uma parada.
    *   Indicar o status da conexão com a API e o IPS.

### **Apontamento de Operação (`InformOperationScreen.kt`)**
*   **O que é:** Seleção de atividade produtiva.
*   **Para que serve:** Permite ao operador mudar o código da operação atual (ex: Plantio, Colheita, Deslocamento) sem precisar encerrar a jornada.

### **Apontamento de Parada (`InformStopScreen.kt`)**
*   **O que é:** Registro de tempo improdutivo.
*   **Para que serve:** Lista motivos de parada (Mecânica, Refeição, Clima, etc.). 
    *   Se for **Abastecimento**, leva para a tela de litros.
    *   Se for **Troca de Turno**, gera um relatório automático e envia via WhatsApp.

### **Registro de Abastecimento (`RefuelingScreen.kt`)**
*   **O que é:** Tela de entrada de dados de combustível.
*   **Para que serve:** Captura a quantidade de litros e o KM do momento para envio aos relatórios de consumo no Wialon.

---

## 4. Consulta e Diagnóstico

### **Diário de Bordo / Histórico (`LogbookScreen.kt` / `HistoryScreen.kt`)**
*   **O que é:** Consulta de jornadas passadas.
*   **Para que serve:** Permite ao operador revisar os horários de início/fim e o resumo de produtividade das jornadas anteriores.

### **Diagnóstico Wialon (`WialonDiagnosticScreen.kt`)**
*   **O que é:** Ferramenta avançada para suporte técnico.
*   **Para que serve:** Mostra a fila de eventos pendentes de sincronização, erros de rede detalhados e permite forçar o reenvio de dados que ficaram presos na memória.

### **Configuração IPS Admin (`WialonIpsAdminScreen.kt`)**
*   **O que é:** Teste de telemetria bruta.
*   **Para que serve:** Valida se o "handshake" com o servidor de telemetria está funcionando e exibe o último pacote de dados enviado no formato industrial.

---

## 5. Encerramento

### **Finalizar Jornada (`EndJourneyScreen.kt`)**
*   **O que é:** Resumo final do turno.
*   **Para que serve:** Exibe a duração total, distância percorrida e áreas visitadas. Solicita o KM final e oferece a opção de compartilhar o boletim de serviço via WhatsApp antes de deslogar.
