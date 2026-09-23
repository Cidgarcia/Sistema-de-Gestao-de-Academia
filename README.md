# 🏋 Sistema de Gestão de Academia — Sprint 1

> Aplicação desktop em **Java 17 + JavaFX + SQLite** com tema moderno AtlantaFX.  
> Funciona 100% offline — sem internet, sem nuvem.

---

## 📋 Pré-requisitos

Antes de rodar o projeto, instale e configure:

### 1. Java 17 (JDK)

1. Acesse: https://adoptium.net/
2. Baixe o **JDK 17** (Temurin) para Windows x64 (instalador `.msi`)
3. Execute o instalador — marque a opção **"Set JAVA_HOME"** e **"Add to PATH"**
4. Verifique no terminal:
   ```powershell
   java -version
   # Deve exibir: openjdk version "17.x.x"
   ```

---

### 2. Apache Maven 3.9+

1. Acesse: https://maven.apache.org/download.cgi
2. Baixe o **apache-maven-3.9.x-bin.zip** (Binary zip archive)
3. Extraia em um local fixo, por exemplo: `C:\tools\maven\`
4. Adicione ao PATH do sistema:
   - Pesquise **"Variáveis de Ambiente"** no Windows
   - Em **Variáveis do Sistema → Path**, clique em **Editar → Novo**
   - Adicione: `C:\tools\maven\bin`
5. Verifique no terminal (abra um **novo** PowerShell):
   ```powershell
   mvn -version
   # Deve exibir: Apache Maven 3.9.x
   ```

> **⚠️ Atenção:** O terminal precisa ser reaberto após alterar o PATH para reconhecer o `mvn`.

---

## 🚀 Como Executar o Projeto

### Passo 1 — Abrir o projeto no VS Code

1. Abra o VS Code
2. Vá em **File → Open Folder**
3. Selecione a pasta: `C:\Users\<seu-usuario>\OneDrive\Documentos\Academia`

---

### Passo 2 — Instalar as extensões recomendadas no VS Code

Instale o **Extension Pack for Java** (da Microsoft):
- Acesse a aba de extensões (`Ctrl+Shift+X`)
- Pesquise: `Extension Pack for Java`
- Clique em **Install**

---

### Passo 3 — Compilar e rodar

Abra o terminal integrado do VS Code (`Ctrl+` `` ` ``) e execute:

```powershell
mvn javafx:run
```

Na primeira execução, o Maven vai baixar as dependências automaticamente (JavaFX, AtlantaFX, SQLite). Pode demorar alguns minutos dependendo da sua internet.

---

## 🔑 Login Padrão

Ao abrir o sistema, use as credenciais abaixo:

| Campo            | Valor         |
|------------------|---------------|
| **Login**        | `admin`       |
| **Senha**        | `admin123`    |
| **Perfil**       | `FUNCIONARIO` |

| Campo            | Valor         |
|------------------|---------------|
| **Login**        | `instrutor`   |
| **Senha**        | `admin123`    |
| **Perfil**       | `INSTRUTOR`   |

> A aba **Avaliação Física** (UC 05) só aparece habilitada para o perfil **INSTRUTOR**.

As contas iniciais são para demonstração. Em **Alterar senha**, cada usuário pode trocar a própria senha (mínimo de 8 caracteres). A troca é opcional: enquanto uma conta mantiver `admin123`, ela continuará usando uma senha pública e conhecida. Os hashes novos usam PBKDF2-HMAC-SHA256 com salt individual; contas antigas com SHA-256 são atualizadas automaticamente após o primeiro login correto.

---

## 🗄️ Banco de Dados

- O banco SQLite é criado **automaticamente** no primeiro acesso, sem nenhuma configuração.
- Localização do arquivo: `C:\Users\<seu-usuario>\academia_db.sqlite`
- As tabelas são criadas via `src/main/resources/db/schema.sql`

---

## ❗ Problemas Comuns e Soluções

### ❌ `mvn` não é reconhecido

**Causa:** Maven não está no PATH do sistema.

**Solução:** Siga o **Passo 2** da seção de pré-requisitos acima. Lembre de abrir um **novo** terminal após configurar.

---

### ❌ Erro `java.lang.UnsupportedClassVersionError`

**Causa:** Você está usando uma versão de Java inferior ao 17.

**Solução:** Instale o JDK 17 e configure a variável `JAVA_HOME`.

---

### ❌ "Credenciais inválidas" no login

**Causas possíveis:**
1. **Perfil errado:** Certifique-se de selecionar `FUNCIONARIO`, não `INSTRUTOR`
2. **Senha alterada:** depois da troca, a senha inicial deixa de funcionar.

Se o problema persistir, confira o perfil e a senha com o responsável pelo banco. Não apague o arquivo `academia_db.sqlite`: ele contém os dados cadastrados.

---

### ❌ ComboBoxes vazios em Matrículas ou Pagamentos

**Causa:** Os dados são recarregados ao trocar de aba. Cadastre alunos e planos primeiro, depois acesse Matrículas.

**Solução:** Clique em outra aba e volte — os combos serão atualizados automaticamente.

---

### ❌ `BUILD FAILURE` ao rodar `mvn javafx:run`

**Solução:** Execute `mvn clean` antes e tente novamente:

```powershell
mvn clean
mvn javafx:run
```

---

## 📁 Estrutura do Projeto

```
Academia/
├── pom.xml                          ← Configuração Maven (dependências)
├── README.md                        ← Este arquivo
└── src/main/
    ├── java/
    │   ├── module-info.java          ← Módulo Java 17
    │   └── com/academia/
    │       ├── App.java              ← Ponto de entrada
    │       ├── observer/             ← Padrão Observer (interfaces)
    │       ├── database/             ← Conexão SQLite (Singleton)
    │       ├── model/                ← Entidades (POJOs)
    │       ├── dao/                  ← Acesso ao banco (JDBC)
    │       └── controller/           ← Lógica e controle das telas
    └── resources/
        ├── db/schema.sql             ← Script de criação das tabelas
        └── com/academia/view/        ← Telas (.fxml)
```

---

## 📌 Funcionalidades (Sprint 1)

| Aba                  | Caso de Uso | Perfil       |
|----------------------|-------------|--------------|
| 👤 Alunos            | UC 01       | Todos        |
| 📋 Planos            | UC 02       | Todos        |
| 📝 Matrículas        | UC 03       | Todos        |
| 💳 Pagamentos        | UC 04       | Todos        |
| 📊 Avaliação Física  | UC 05       | Instrutor    |

---

## 🔄 Padrão Observer (requisito do projeto)

O sistema notifica automaticamente o cabeçalho sempre que um pagamento é registrado:

```
PagamentoController  →  notificarObservers()  →  PainelPrincipalController
     (Subject)                                          (Observer)
                                                           ↓
                                               Atualiza "Total Recebido Hoje"
```

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia    | Versão  | Finalidade                  |
|---------------|---------|-----------------------------|
| Java          | 17      | Linguagem principal         |
| JavaFX        | 21.0.2  | Interface gráfica           |
| AtlantaFX     | 2.0.1   | Tema visual moderno         |
| SQLite (JDBC) | 3.45    | Banco de dados local        |
| Maven         | 3.9+    | Gerenciador de dependências |
