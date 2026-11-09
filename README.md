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

- 📡 **Atualizações Automáticas OTA (Over-The-Air)**:
  - Verificação silenciosa e automática de novas versões ao abrir o aplicativo (ou sob demanda via menu).
  - Download em segundo plano com barra de progresso em tempo real e instalação nativa via `FileProvider`.
  - Integração contínua entre **GitHub Releases** (armazenamento de APKs) e **Vercel** (distribuição e manifesto de versão).

- 🛡️ **Suporte a Certificado SSL Corporativo**:
  - Tratamento nativo de conexão para ambientes de intranet corporativa com certificados SSL internos.

- 🔍 **Ajuste de Zoom e Escala de Texto**:
  - Opções de zoom configuráveis (85%, 100%, 115%, 145%) persistidas nas preferências do aplicativo para facilitar a visualização em coletores com telas de tamanhos variados.

- ⚡ **Teclas de Ação Rápida**:
  - Botão de confirmação de campo focado (Enter físico), recarregamento da página e menu rápido de configurações.

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
 │    └── PreferencesRepository.kt   # Leitura e escrita da escala de texto em SharedPreferences (85%, 100%, 115%, 145%)
 ├── ui/
 │    └── AppDialogs.kt              # Construtor de caixas de diálogo e menu principal
 ├── update/
 │    └── AppUpdateManager.kt        # Consulta OTA ao version.json, download de APK e acionamento do instalador nativo
 ├── util/
 │    ├── NetworkUtils.kt            # Verificação do status de rede e conectividade
 │    └── UrlValidator.kt            # Sanitização e validação segura da URL do SAP
 ├── web/
 │    ├── SapSessionManager.kt       # Configurações do WebView e procedimentos de logoff
 │    ├── SapWebViewClient.kt        # Gerenciamento de eventos de página e liberação de SSL
 │    └── TabCookieManager.kt        # Isolamento de cookies e sessão por aba
 └── MainActivity.kt                 # Controladora principal com ViewBinding, Abas Duplas e ciclo do Auto-Update
```

Para detalhes aprofundados sobre a infraestrutura em nuvem e o fluxo DevOps, consulte o documento [ARQUITETURA_E_DEVOPS.md](ARQUITETURA_E_DEVOPS.md).

---

## 📋 Requisitos de Desenvolvimento e Execução

- **Linguagem**: Kotlin
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **JVM Compatibility**: Java 11
- **Recursos Android**: AndroidX, Material Components, View Binding, FileProvider

---

## 👨‍💻 Créditos e Direitos Autorais

- **Desenvolvedores**: Erick Jaboinski e Marcos Gustavo de Lima
- **Copyright**: © 2026 Erick Jaboinski. Todos os direitos reservados.
