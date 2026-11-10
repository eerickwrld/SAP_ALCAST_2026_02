# SAP ALCAST MOBILE 2026

Interface simplificada e otimizada para coletores de dados Android com **SAP ITS Mobile** e sistema de **Auto-Update OTA (Over-The-Air)**.

---

## 📌 Sobre o Projeto

O **SAP ALCAST MOBILE** foi desenvolvido para proporcionar uma experiência fluida, estável e eficiente no manuseio de coletores de dados Android em ambiente fabril e logístico. O aplicativo atua como uma camada de interface moderna sobre o SAP ITS Mobile, garantindo a leitura física por leitores de código de barras, navegação por abas paralelas sem perda de dados e atualização remota automática de coletores sem necessidade de intervenção física da equipe de TI.

---

## 🚀 Funcionalidades Principais

- 🔄 **Navegação com Abas Duplas Independentes**:
  - Permite operar duas telas/processos do SAP ITS Mobile em paralelo (**Aba 1** e **Aba 2**).
  - Troca instantânea de abas mantendo a tela anterior congelada/estática, preservando formulários preenchidos, posição de rolagem e histórico.

- 🔑 **Isolamento de Sessão e Cookies**:
  - Mecanismo exclusivo de gerenciamento de cookies por aba (`TabCookieManager`), permitindo que cada aba mantenha sua própria sessão no servidor SAP NetWeaver sem que uma aba desconecte a outra.
  - Blindagem de autenticação SAML 2.0 / SSO contra expiração de tokens e loops de redirecionamento.

- 📡 **Atualizações Automáticas OTA (Over-The-Air)**:
  - Verificação silenciosa e automática de novas versões ao abrir o aplicativo (ou sob demanda via menu).
  - Download em segundo plano com barra de progresso em tempo real e instalação nativa via `FileProvider`.
  - Integração contínua entre **GitHub Releases** (armazenamento de APKs) e **Vercel** (distribuição e manifesto de versão).

- 🔋 **Monitores de Bateria e Wi-Fi em Tempo Real**:
  - Indicador visual do nível exato de bateria e status de carregamento no topo da tela, com alertas visuais para bateria fraca.
  - Monitoramento contínuo de conectividade Wi-Fi (`Wi-Fi OK` / `Sem Wi-Fi`) para prevenir perda de apontamentos em pontos cegos da fábrica.

- 🎯 **Auto-Foco Inteligente no Leitor (Barcode Ready)**:
  - Posicionamento e seleção automática do cursor no primeiro campo editável da tela do SAP assim que a página carrega, eliminando a necessidade de tocar na tela antes de bipar etiquetas.

- 📳 **Feedback Tátil & Proteção de Saída**:
  - Resposta por vibração física (*Haptic Feedback*) ao acionar o botão de confirmação.
  - Bloqueio de saída acidental (*Double back to exit*), prevenindo que o operador feche o aplicativo involuntariamente.

- 🛡️ **Blindagem contra `ERR_CACHE_MISS`**:
  - Tratamento de `onFormResubmission` e política `LOAD_CACHE_ELSE_NETWORK`, permitindo voltar no histórico de telas e formulários POST do SAP com reenvio transparente e sem travar.

- 🔍 **Ajuste de Zoom e Escala de Texto**:
  - Opções de zoom configuráveis (85%, 100%, 115%, 160%) persistidas nas preferências do aplicativo para facilitar a visualização em coletores com telas de tamanhos variados.

- ⚡ **Teclas de Ação Rápida**:
  - Botão de confirmação de campo focado (Enter físico), recarregamento da página e menu rápido de configurações.

---

## 💎 Versão Atual: `v3.7 • Cristal` (Build 10)

As versões do aplicativo são batizadas com codinomes de pedras preciosas:
- **v3.7 • Cristal** (Build 10): Auto-Foco inteligente no leitor de código de barras e blindagem definitiva de cache no voltar.
- **v3.6 • Berilo** (Build 9): Indicadores visuais de nível de Bateria e status do Wi-Fi no topo do app.
- **v3.5 • Peridoto** (Build 8): Vibração tátil no botão confirmar e proteção contra saída acidental do SAP.
- **v3.3 / v3.4**: Esteira completa de Auto-Update OTA via Vercel e GitHub Releases.

---

## 🌐 Portal de Instalação e Endpoints

- **Portal Web Oficial:** [https://sapalcast2026.vercel.app](https://sapalcast2026.vercel.app)
- **Manifesto de Versão (API OTA):** [https://sapalcast2026.vercel.app/version.json](https://sapalcast2026.vercel.app/version.json)
- **Repositório GitHub & Releases:** [https://github.com/eerickwrld/SAP_ALCAST_2026_02](https://github.com/eerickwrld/SAP_ALCAST_2026_02)

---

## 🛠️ Arquitetura e Clean Code

O projeto é estruturado segundo os princípios do **Clean Code** e **Responsabilidade Única (SRP)**:

```
app/src/main/java/com/example/appar/
 ├── data/
 │    └── PreferencesRepository.kt   # Leitura e escrita da escala de texto em SharedPreferences (85%, 100%, 115%, 160%)
 ├── ui/
 │    └── AppDialogs.kt              # Construtor de caixas de diálogo, menu principal e tela "Sobre o Aplicativo"
 ├── update/
 │    └── AppUpdateManager.kt        # Consulta OTA ao version.json, download de APK e acionamento do instalador nativo
 ├── util/
 │    ├── NetworkUtils.kt            # Verificação do status de rede e conectividade
 │    └── UrlValidator.kt            # Sanitização e validação segura da URL do SAP
 ├── web/
 │    ├── SapSessionManager.kt       # Configurações do WebView (LOAD_CACHE_ELSE_NETWORK) e procedimentos de logoff
 │    ├── SapWebViewClient.kt        # Gerenciamento de eventos de página, auto-foco, liberação de SSL e form resubmission
 │    └── TabCookieManager.kt        # Isolamento de cookies e sessão por aba com suporte a SAML2
 └── MainActivity.kt                 # Controladora principal com Abas Duplas, Bateria/Wi-Fi, Haptic Feedback e Auto-Update
```

Para detalhes aprofundados sobre a infraestrutura em nuvem e o fluxo DevOps, consulte o documento [ARQUITETURA_E_DEVOPS.md](ARQUITETURA_E_DEVOPS.md).

---

## 📋 Requisitos de Desenvolvimento e Execução

- **Linguagem**: Kotlin
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **JVM Compatibility**: Java 11
- **Recursos Android**: AndroidX, Material Components, View Binding, FileProvider, Haptic Feedback, Network/Battery APIs

---

## 👨‍💻 Créditos e Direitos Autorais

- **Desenvolvedores**: Erick Jaboinski e Marcos Gustavo de Lima
- **Organização**: Alcast do Brasil S.A.
- **Copyright**: © 2026 Erick Jaboinski. Todos os direitos reservados.
