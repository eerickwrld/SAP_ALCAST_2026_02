# SAP ALCAST MOBILE 2026

Interface simplificada e otimizada para coletores de dados Android com **SAP ITS Mobile**.

---

## 📌 Sobre o Projeto

O **SAP ALCAST MOBILE** foi desenvolvido para proporcionar uma experiência fluida, estável e eficiente no manuseio de coletores de dados Android em ambiente fabril e logístico. O aplicativo atua como uma camada de interface moderna sobre o SAP ITS Mobile, garantindo a leitura física por leitores de código de barras e oferecendo navegação por abas paralelas sem perda de dados.

---

## 🚀 Funcionalidades Principais

- 🔄 **Navegação com Abas Duplas Independentes**:
  - Permite operar duas telas/processos do SAP ITS Mobile em paralelo (**Aba 1** e **Aba 2**).
  - Troca instantânea de abas mantendo a tela anterior congelada/estática, preservando formulários preenchidos, posição de rolagem e histórico.

- 🔑 **Isolamento de Sessão e Cookies**:
  - Mecanismo exclusivo de gerenciamento de cookies por aba (`TabCookieManager`), permitindo que cada aba mantenha sua própria sessão no servidor SAP NetWeaver sem que uma aba desconecte a outra.

- 🛡️ **Suporte a Certificado SSL Corporativo**:
  - Tratamento nativo de conexão para ambientes de intranet corporativa com certificados SSL internos.

- 🔍 **Ajuste de Zoom e Escala de Texto**:
  - Opções de zoom configuráveis (85%, 100%, 115%, 150%) persistidas nas preferências do aplicativo para facilitar a visualização em coletores com telas de tamanhos variados.

- ⚡ **Teclas de Ação Rápida**:
  - Botão de confirmação de campo focado (Enter físico), recarregamento da página e menu rápido de configurações.

---

## 🛠️ Arquitetura e Clean Code

O projeto é estruturado segundo os princípios do **Clean Code** e **Responsabilidade Única (SRP)**:

```
app/src/main/java/com/example/appar/
 ├── data/
 │    └── PreferencesRepository.kt   # Leitura e escrita do zoom persistido em SharedPreferences
 ├── ui/
 │    └── AppDialogs.kt              # Construtor de caixas de diálogo e menus do app
 ├── util/
 │    ├── NetworkUtils.kt            # Verificação do status de rede e conectividade
 │    └── UrlValidator.kt            # Sanitização e validação segura da URL do SAP
 ├── web/
 │    ├── SapSessionManager.kt       # Configurações do WebView e procedimentos de logoff
 │    ├── SapWebViewClient.kt        # Gerenciamento de eventos de página e liberação de SSL
 │    └── TabCookieManager.kt        # Isolamento de cookies e sessão por aba
 └── MainActivity.kt                 # Controladora principal com ViewBinding e Abas Duplas
```

---

## 📋 Requisitos de Desenvolvimento e Execução

- **Linguagem**: Kotlin
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **JVM Compatibility**: Java 11
- **Recursos Android**: AndroidX, Material Components, View Binding

---

## 👨‍💻 Créditos e Direitos Autorais

- **Desenvolvedores**: Erick Jaboinski e Marcos Gustavo de Lima
- **Copyright**: © 2026 Erick Jaboinski. Todos os direitos reservados.
- **Auto-Update Host**: Vercel (https://sapalcast2026.vercel.app)
