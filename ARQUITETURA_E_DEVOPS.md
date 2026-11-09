# 📘 Arquitetura de Distribuição e Auto-Update OTA (Over-The-Air)
### SAP ALCAST Mobile • Documentação Técnica de Infraestrutura e DevOps

---

## 1. 🎯 Visão Geral do Problema e da Solução

### O Desafio Anterior
Anteriormente, cada nova versão ou correção do aplicativo exigia um processo manual e demorado de atualização:
- Conectar cada coletor fisicamente via cabo USB ao computador;
- Copiar o arquivo `.apk` manualmente para a memória interna de cada aparelho;
- Deslocar a equipe de TI até a fábrica e estoque, paralisando a operação dos coletores.

### A Nova Solução
Implementou-se uma esteira moderna de **DevOps** e distribuição **OTA (Over-The-Air)**, onde o próprio aplicativo Android verifica em segundo plano se há novas versões disponíveis na nuvem. Havendo uma versão superior, o coletor baixa e inicia a instalação automaticamente.

---

## 2. 🛠️ Ferramentas Utilizadas e Seus Papéis

| Ferramenta | Papel na Solução | Por que foi escolhida? |
| :--- | :--- | :--- |
| **GitHub** | Versionamento de Código e Hospedagem de Binários (`Releases`) | Padrão da indústria para controle de versão (Git). O recurso de **Releases** permite armazenar os pacotes de instalação (`.apk`) com versionamento semântico (`v3.0`, `v3.1`) e alta taxa de transferência para download. |
| **Vercel** | Hospedagem Serverless / Edge CDN do Manifesto de Versões | Plataforma em nuvem com alta disponibilidade e latência mínima. Hospeda o arquivo `version.json` (que os coletores consultam) e o **Portal Web de Instalação**, com certificados SSL/HTTPS automáticos e cache invalidado a cada deploy. |
| **GitHub Actions** | Esteira de CI/CD (Integração e Entrega Contínua) | Automatiza a compilação de código diretamente em contêineres na nuvem, garantindo reprodutibilidade do build sem depender de configurações manuais locais. |
| **Android FileProvider** | Segurança e Permissões do Sistema Operacional | Mecanismo nativo do Android que permite compartilhar o `.apk` baixado na pasta privada do aplicativo com o instalador oficial de pacotes do Android de forma estritamente segura. |

---

## 3. 🔄 Fluxo de Funcionamento (Arquitetura)

```
┌────────────────────────────────────────────────────────┐
│               1. AMBIENTE DE DESENVOLVIMENTO           │
│                                                        │
│   Desenvolvedor altera o código no Android Studio      │
│   • Incrementa versionCode no build.gradle.kts         │
│   • Atualiza notas da versão no public/version.json    │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│             2. CONTROLE E DISTRIBUIÇÃO (NUVEM)         │
│                                                        │
│   GitHub: Armazena o código e o APK na Release (v3.x)  │
│   Vercel: Publica o manifesto de versão em tempo real  │
│           (https://sapalcast2026.vercel.app)           │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│             3. DISPOSITIVO FINAL (COLETOR DE DADOS)    │
│                                                        │
│   Ao iniciar o app (ou pelo menu do operador):        │
│   • Consulta silenciosa ao /version.json na Vercel     │
│   • Detecta versão mais nova disponível no GitHub      │
│   • Notifica o operador com as novidades da versão     │
│   • Baixa o APK com barra de progresso                 │
│   • Dispara a instalação nativa do Android             │
└────────────────────────────────────────────────────────┘
```

---

## 4. 📂 Estrutura dos Arquivos Criados

### No Aplicativo Android
- `AppUpdateManager.kt` ([caminho](app/src/main/java/com/example/appar/update/AppUpdateManager.kt)):
  Classe responsável pela consulta HTTP assíncrona ao `version.json`, download em streaming com cálculo de porcentagem e acionamento do `Intent.ACTION_VIEW` com o instalador de pacotes do Android.
- `file_paths.xml` ([caminho](app/src/main/res/xml/file_paths.xml)):
  Configuração do `FileProvider` mapeando o diretório seguro de armazenamento do APK baixado.
- `AndroidManifest.xml` ([caminho](app/src/main/AndroidManifest.xml)):
  Declaração da permissão `REQUEST_INSTALL_PACKAGES` e do provedor de conteúdo.

### Na Camada Web (Vercel)
- `public/version.json` ([caminho](public/version.json)):
  Manifesto JSON contendo o código da versão mais recente, link direto do APK no GitHub e lista de novidades.
- `public/index.html` ([caminho](public/index.html)):
  Portal oficial com interface moderna e responsiva para download do APK na primeira configuração de coletores novos.
- `vercel.json` ([caminho](vercel.json)):
  Diretivas de roteamento e desativação de cache HTTP (`no-cache, no-store`) para garantir que os coletores leiam a versão em tempo real.

---

## 5. 📋 Passo a Passo para Lançar Novas Versões

Quando uma nova versão precisar ser lançada para a fábrica:

1. **Alterar a Versão no App**:
   No arquivo `app/build.gradle.kts`, incremente os valores:
   ```kotlin
   versionCode = 5          // Sempre um número a mais que o anterior
   versionName = "3.2"      // Nome amigável da versão
   ```

2. **Gerar o Pacote**:
   No menu do Android Studio:
   `Build > Generate App Bundles or APKs > Generate APKs`.

3. **Publicar a Release no GitHub**:
   - Acesse: `https://github.com/eerickwrld/SAP_ALCAST_2026_02/releases/new`
   - Crie a tag correspondente (ex: `v3.2`).
   - Anexe o arquivo `app-debug.apk` gerado.
   - Publique a Release.

4. **Atualizar o `public/version.json`**:
   Atualize o arquivo com o novo `versionCode`, URL da Release e as notas da versão:
   ```json
   {
     "versionCode": 5,
     "versionName": "3.2",
     "apkUrl": "https://github.com/eerickwrld/SAP_ALCAST_2026_02/releases/download/v3.2/app-debug.apk",
     "releaseNotes": "Descreva aqui as melhorias implementadas.",
     "forceUpdate": false
   }
   ```

5. **Commit e Push**:
   Envie as alterações para o GitHub. A Vercel atualizará o endpoint em segundos e todos os coletores estarão prontos para atualizar sozinhos.

---

## 6. 🏆 Benefícios Alcançados

- ⏱️ **Redução de Tempo de TI:** De horas de coleta manual aparelho por aparelho para **zero tempo de intervenção física**.
- 🛡️ **Zero Impacto na Operação:** O operador é notificado na tela e atualiza em menos de 1 minuto diretamente do posto de trabalho.
- 📊 **Custo Zero de Infraestrutura:** Implementado utilizando planos gratuitos da Vercel e do GitHub com segurança e alto desempenho.
- 📈 **Conformidade DevOps:** Código centralizado, versionado com histórico de commits e rastreabilidade total de versões.
