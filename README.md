# PLMAGRO - Terminal de Telemetria Industrial Agrícola

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/Version-3.5.0-blue?style=for-the-badge)

O **PLMAGRO** é uma solução robusta de telemetria e gestão operacional para frotas agrícolas. Desenvolvido para operar em ambientes de conectividade instável, o aplicativo atua como um terminal inteligente que integra dados de campo diretamente ao ecossistema Wialon.

## 🚀 Principais Funcionalidades

*   **Telemetria Industrial (Wialon IPS):** Rastreamento em tempo real utilizando protocolo Socket TCP bruto para máxima confiabilidade e baixo consumo de dados.
*   **Arquitetura Offline-First:** Persistência total via banco de dados **Room**. O operador nunca para de trabalhar, mesmo sem sinal 4G/WiFi.
*   **Inteligência de Campo:** 
    *   Automação de operação por **Cercas Geográficas (Geofences)**.
    *   Detecção automática de paradas não apontadas (Timer de inatividade configurável).
*   **Gestão de Insumos:** Registro de abastecimentos com validação de KM.
*   **Relatórios Automáticos:** Geração de boletins de troca de turno enviados via WhatsApp.
*   **Monitor de Saúde do Hardware:** Acompanhamento térmico e de energia do tablet para prevenir danos físicos no ambiente severo do trator.

## 🛠 Tecnologias Utilizadas

*   **Dagger-Hilt:** Injeção de Dependência para um código modular e testável.
*   **Jetpack Compose:** Interface reativa, moderna e de alta performance.
*   **Coroutines & Flow:** Processamento assíncrono de dados em tempo real.
*   **DataStore:** Armazenamento seguro de configurações e estados de sincronização.
*   **Retrofit & OkHttp:** Comunicação resiliente com APIs REST.

## 📂 Estrutura de Documentação

Para entender detalhes específicos do projeto, consulte os arquivos abaixo na raiz:

*   [`DOCUMENTACAO_TELAS.md`](./DOCUMENTACAO_TELAS.md): Guia visual e funcional de cada tela.
*   [`MELHORIAS_EFETUADAS.md`](./MELHORIAS_EFETUADAS.md): Log histórico de todas as versões e evoluções técnicas.
*   [`AGENTE_10_10.md`](./AGENTE_10_10.md): Roadmap estratégico para atingir a excelência industrial.
*   [`GUIA_NOVO_PROJETO.md`](./GUIA_NOVO_PROJETO.md): Manual para replicar estas funcionalidades em novos aplicativos do zero.

## ⚙️ Configuração do Ambiente

1. Clone o repositório: `git clone https://github.com/sonielguedes/Plm_Agro.git`
2. Abra no Android Studio (Versão Ladybug ou superior).
3. Certifique-se de que o plugin **KSP** está configurado.
4. Execute o **Gradle Sync**.

---
*Desenvolvido com foco em alta performance e resiliência no campo.*
