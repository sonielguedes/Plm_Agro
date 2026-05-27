# Relatório Técnico de Correções e Melhorias - Plm_Agro

Este documento descreve as intervenções técnicas realizadas para estabilizar a comunicação entre o aplicativo e os serviços Wialon (IPS e Remote API), bem como melhorias na interface de usuário e persistência de dados.

## 1. Comunicação e Segurança (Network)

### 1.1. Remoção de SSL Pinning Restritivo
*   **Problema:** O aplicativo apresentava o erro `Certificate pinning failure` ao tentar conectar ao domínio personalizado `plmview.plmagro.com.br` e `*.wialon.com`. Isso ocorria devido a códigos (pins) de certificados fixos no código que não correspondiam aos certificados reais dos servidores.
*   **Solução:** Removida a configuração de `CertificatePinner` no `NetworkModule.kt`.
*   **Resultado:** O aplicativo agora utiliza a validação SSL padrão do sistema Android, permitindo conexão segura com os servidores Wialon e o gateway IPS sem bloqueios.

## 2. Gateway Wialon IPS (Telemetria)

### 2.1. Correção no Salvamento de Configurações
*   **Problema:** A função `saveIpsConfig` no `MainViewModel.kt` estava com a assinatura incompleta, impedindo a persistência do ID Único e do Nome da Unidade. Além disso, a tela de configuração não oferecia feedback visual de sucesso.
*   **Solução:** 
    *   Atualizada a função `saveIpsConfig` para receber e persistir quatro parâmetros: `host`, `port`, `uniqueId` e `unitName`.
    *   Adicionada notificação via `Snackbar` após o salvamento bem-sucedido.

### 2.2. Validação de ID Único e Correção Automática
*   **Problema:** Divergência entre o ID Único configurado manualmente e o ID vinculado à frota no Wialon causava falha de autenticação (Erro `#AD#-1`).
*   **Solução:** Implementada uma lógica de "Divergência Detectada" na `WialonIpsAdminScreen.kt` que compara o ID digitado com o ID do vínculo ativo, oferecendo um botão de **"CORRIGIR"** automático.

## 3. Diagnóstico e Sincronização (Remote API)

### 3.1. Reestruturação do Layout de Diagnóstico
*   **Problema:** As listas de "Unidades Encontradas" e "Últimos Eventos" estavam sobrepostas (encavaladas) devido ao uso incorreto de pesos (`weight`) dentro de uma `Column` com múltiplas áreas roláveis.
*   **Solução:** Refatorada a `WialonDiagnosticScreen.kt` para utilizar uma **única `LazyColumn`**. Todos os elementos agora seguem um fluxo contínuo de rolagem, eliminando a sobreposição.

### 3.2. Sincronização de Motoristas e Cercas
*   **Problema:** A tela de login não exibia todos os motoristas cadastrados e havia uma trava de 30 minutos que impedia atualizações manuais frequentes.
*   **Solução:**
    *   Aprimorada a função `syncOperators` no `MainViewModel.kt` para garantir a persistência imediata no banco de dados local (`Room`).
    *   **Removido o cache de 30 minutos** em `WialonRepository.kt` para permitir sincronizações manuais imediatas sob demanda do usuário.
    *   Aumentado o limite de exibição na tela de login de 5 para 50 motoristas.
    *   Melhorado o filtro de busca por Nome e Matrícula.

### 3.3. Recuperação de Dados Avançados da Unidade (Vínculo)
*   **Problema:** Ao vincular um equipamento, campos como **Placa**, **Tipo** e **KM** ficavam vazios ou exigiam preenchimento manual redundante.
*   **Solução:** 
    *   Atualizada a função `getUnitData` no `WialonRepository.kt` para solicitar flags de propriedades de perfil e contadores (`0x1 | 0x100 | 0x400`).
    *   Expandida a busca de propriedades para incluir mapeamentos comuns do Wialon: `pht`/`registration_plate` para Placa e `vt`/`vehicle_type` para Tipo de Equipamento.
    *   **Importação Automática:** Implementada lógica na `LinkFleetScreen.kt` que busca e exibe esses dados em uma seção "DADOS DO SERVIDOR" antes de confirmar o vínculo, preenchendo-os automaticamente na configuração do veículo.

## 4. Tratamento de Erros de Protocolo

*   **Implementação:** Adicionada documentação e logs para o erro `#AD#-1` (Erro de Protocolo de Dados), auxiliando o usuário a entender que o erro refere-se à rejeição do pacote de GPS pelo servidor (geralmente por falta de sinal de GPS válido no momento do envio).

---
**Status Final:** O projeto encontra-se compilável, com comunicação restabelecida e interfaces corrigidas.
**Data:** 26/05/2026
**Versão:** v3.1.0-SYNC
