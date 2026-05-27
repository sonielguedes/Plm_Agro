# Guia para Criar um Novo App do Zero (Mesmas Funções)

Este roteiro técnico descreve os passos necessários para replicar a arquitetura e as funcionalidades do sistema **PLMAGRO** em um novo projeto Android.

---

## 1. Configuração Inicial e Dependências

Para ter um app robusto, offline-first e com telemetria, você deve configurar as seguintes bibliotecas no seu `build.gradle` (ou `libs.versions.toml`):

*   **Hilt:** Para Injeção de Dependência (Organização do código).
*   **Room:** Banco de dados local (Para funcionar sem internet).
*   **Retrofit + OkHttp:** Para comunicação com a API do Wialon.
*   **DataStore:** Para salvar configurações simples (Token, IPs).
*   **Compose:** Para a interface moderna e reativa.

---

## 2. Passo a Passo Técnico

### Passo 1: Estrutura de Pastas (Arquitetura)
Organize seu projeto seguindo este padrão:
1.  `api/`: Interfaces de rede e modelos de dados do servidor.
2.  `di/`: Módulos do Hilt para fornecer instâncias automáticas.
3.  `model/`: Entidades do Banco de Dados (Room) e Repositórios.
4.  `service/`: Serviços de primeiro plano (Foreground Services) para telemetria.
5.  `ui/`: Suas telas (Screens) e componentes visuais.
6.  `core/`: Utilitários globais como logs industriais e gerenciador de rede.

### Passo 2: Criar o Coração Offline (Banco de Dados)
Crie a classe `PlmDatabase` e o `PlmDao`. 
*   **Regra de Ouro:** Toda ação do usuário deve ser salva primeiro no banco local. Nunca dependa da internet para registrar um evento.

### Passo 3: Implementar o Motor de Sincronização (Outbox)
Crie um `OutboxManager`. 
*   Ele deve observar o banco de dados e, sempre que houver um item novo, tentar enviar para a internet usando **Backoff Exponencial** (tentar de 5 em 5s, depois 10s, etc).

### Passo 4: Telemetria Industrial (Socket TCP)
Para o rastreamento em tempo real (Protocolo IPS):
*   Não use HTTP. Use `java.net.Socket`.
*   Crie um `WialonIpsClient` que formate as mensagens no padrão `#D#...` e envie via socket bruto para o IP do gateway.

### Passo 5: Gerenciador de Estado de Jornada
Crie uma lógica (FSM - Finite State Machine) para controlar o estado do veículo:
*   `SEM_JORNADA` -> `OPERANDO` -> `PARADO` -> `MANUTENÇÃO`.
*   Isso garante que o operador não pule etapas obrigatórias.

---

## 3. Fluxo de Desenvolvimento Sugerido

1.  **Configurar o Hilt e Room:** Garanta que os dados salvos persistam após fechar o app.
2.  **Criar o Serviço de GPS:** Implementar o `ForegroundService` que roda mesmo com o app em segundo plano.
3.  **Implementar Login e Vínculo:** Conectar com a API do Wialon para baixar a lista de unidades.
4.  **Desenvolver a Dashboard:** Criar a tela principal consumindo os dados do Repositório via `Flow`.
5.  **Adicionar Segurança Offline:** Implementar o `NetworkUtils` para bloquear chamadas de rede quando não houver sinal, evitando travamentos.

---

## 4. Diferenciais Essenciais para Replicar

*   **Robustez de Rede:** Sempre use `try-catch` em chamadas de rede para o app não fechar (crash).
*   **Economia de Dados:** Implemente cache de 1 hora para cercas geográficas e motoristas.
*   **Logs JSON:** Use um logger estruturado para facilitar o suporte remoto.
*   **Abertura Rápida:** O `init` das suas ViewModels não deve fazer chamadas de rede pesadas de forma síncrona.

---

Este guia garante que o novo aplicativo tenha o mesmo nível de confiabilidade e desempenho industrial do projeto atual.
