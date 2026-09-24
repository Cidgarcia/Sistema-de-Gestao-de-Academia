# GymCore — Sistema de Gestão de Academia

> Aplicação desktop em **Java 17 + JavaFX + SQLite** com tema moderno AtlantaFX.  
> Focada no controle de frequência, treinos, gestão financeira e emissão de relatórios.  
> Funciona 100% offline — sem internet, sem nuvem.

---

## 📋 Pré-requisitos

O código do aplicativo é o mesmo para Windows e Linux. Antes de rodar o projeto, instale e configure:

### 1. Java 17 (JDK)

**Windows:**
1. Acesse: https://adoptium.net/
2. Baixe o **JDK 17** (Temurin) para Windows x64 (instalador `.msi`)
3. Execute o instalador — marque a opção **"Set JAVA_HOME"** e **"Add to PATH"**

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

Verifique no terminal:
```bash
java -version
# Deve exibir: openjdk version "17.x.x"
```

---

### 2. Apache Maven 3.9+

**Windows:**
1. Acesse: https://maven.apache.org/download.cgi
2. Baixe o **apache-maven-3.9.x-bin.zip** (Binary zip archive)
3. Extraia em um local fixo, por exemplo: `C:\tools\maven\`
4. Adicione ao PATH do sistema:
   - Pesquise **"Variáveis de Ambiente"** no Windows
   - Em **Variáveis do Sistema → Path**, clique em **Editar → Novo**
   - Adicione: `C:\tools\maven\bin`

**Linux (Ubuntu/Debian):**
```bash
sudo apt install maven
```

Verifique no terminal (abra um **novo** terminal):
```bash
mvn -version
# Deve exibir: Apache Maven 3.9.x
```

> **⚠️ Atenção:** O terminal precisa ser reaberto após alterar o PATH para reconhecer o `mvn` no Windows.

---

## 🚀 Como Executar o Projeto

### Passo 1 — Abrir a pasta do projeto

Abra a pasta em que o repositório foi clonado no seu explorador de arquivos ou editor de preferência.

*(Opcional)* Se usar o **VS Code**:
1. Vá em **File → Open Folder** e selecione a pasta do repositório.
2. É recomendável instalar o **Extension Pack for Java** (da Microsoft) para obter melhor suporte.

---

### Passo 2 — Compilar, testar e rodar

Abra o terminal (ou terminal integrado da sua IDE) na pasta do projeto e execute os comandos abaixo (válidos para Windows e Linux):

Para rodar os testes:
```bash
mvn test
```

Para executar a aplicação:
```bash
mvn javafx:run
```

Na primeira execução, o Maven vai baixar as dependências automaticamente (JavaFX, AtlantaFX, SQLite, etc). Pode demorar alguns minutos dependendo da sua internet.

---

## 🔑 Login e Segurança

Ao abrir o sistema, use as credenciais de demonstração abaixo:

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

As contas iniciais são para demonstração. Em **Alterar senha** (disponível na interface principal), cada usuário pode trocar a própria senha (mínimo de 8 caracteres). As senhas iniciais só valem enquanto não forem trocadas. 

A troca é opcional, mas os hashes novos usam PBKDF2-HMAC-SHA256 com salt individual para maior segurança. Contas antigas que utilizam SHA-256 são atualizadas automaticamente após o primeiro login correto.

---

## 🗄️ Banco de Dados

- O banco SQLite é criado **automaticamente** no primeiro acesso, sem nenhuma configuração.
- O arquivo `academia_db.sqlite` fica localizado na pasta pessoal do usuário do sistema operacional. Por exemplo:
  - **Windows:** `C:\Users\<seu-usuario>\academia_db.sqlite`
  - **Linux:** `/home/<seu-usuario>/academia_db.sqlite`
- As tabelas são criadas via `src/main/resources/db/schema.sql`

> **⚠️ Aviso:** Não apague o banco de dados se ele contiver dados cadastrados, para não perder as informações da academia.

---

## 📌 Funcionalidades e Permissões por Perfil

O sistema possui controle de permissões baseado no perfil do usuário logado:

| Área                  | Acesso: Funcionário | Acesso: Instrutor |
|-----------------------|---------------------|-------------------|
| 👤 Alunos — consulta  | ✅                  | ✅                |
| 👤 Alunos — cadastro, edição e exclusão | ✅ | ❌          |
| 🚪 Recepção/Frequência| ✅                  | ❌                |
| 📋 Planos             | ✅                  | ❌                |
| 📝 Matrículas         | ✅                  | ❌                |
| 💳 Pagamentos/Caixa   | ✅                  | ❌                |
| 📊 Avaliação Física   | ❌                  | ✅                |
| 🏋️ Treinos            | ❌                  | ✅                |
| 📈 Relatórios         | ✅                  | ✅                |

O Instrutor pode consultar alunos, mas somente o Funcionário pode cadastrar, editar ou excluir alunos e cancelar matrículas pela tela Alunos.

---

## 📈 Relatórios

O sistema conta com uma área dedicada a **Relatórios** gerenciais e operacionais. Os relatórios disponíveis podem ser visualizados na interface e possuem a funcionalidade de exportação direta em formato **PDF**, facilitando o compartilhamento e impressão (ex: desempenho da academia, fichas de alunos, etc).

---

## ❗ Problemas Comuns e Soluções

### ❌ `mvn` não é reconhecido

**Causa:** Maven não está no PATH do sistema.

**Solução:** Siga os passos de instalação e configuração do Maven na seção de pré-requisitos acima. Lembre de abrir um **novo** terminal após configurar.

---

### ❌ Erro `java.lang.UnsupportedClassVersionError`

**Causa:** Você está usando uma versão de Java inferior ao 17.

**Solução:** Instale o JDK 17 e configure a variável `JAVA_HOME`.

---

### ❌ "Credenciais inválidas" no login

**Causas possíveis:**
1. **Perfil errado:** Certifique-se de selecionar o perfil correto (`FUNCIONARIO` ou `INSTRUTOR`).
2. **Senha alterada:** Depois da troca, a senha inicial de demonstração deixa de funcionar.

Se o problema persistir, confira o perfil e a senha.

---

### ❌ ComboBoxes vazios em Matrículas ou Pagamentos

**Causa:** Os dados são recarregados automaticamente conforme a navegação atual.

**Solução:** Certifique-se de que você já cadastrou alunos e planos. Caso ainda não tenha feito, vá nas respectivas telas e efetue os cadastros, então retorne para as Matrículas/Pagamentos.

---

### ❌ `BUILD FAILURE` ao rodar `mvn javafx:run`

**Causa:** "Build Failure" é um aviso genérico de erro. A compilação falhou por algum motivo (erro de sintaxe, teste que falhou, dependência não encontrada, etc).

**Solução:** Role o terminal para cima e identifique o erro exato na saída do Maven antes de prosseguir. Caso tenha certeza que é apenas um problema de cache corrompido ou de limpezas de compilações anteriores, você pode tentar rodar `mvn clean` antes de executar novamente:

```bash
mvn clean
mvn javafx:run
```

---

## 📁 Estrutura do Projeto

```text
Sistema-de-Gestao-de-Academia/
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
    │       ├── controller/           ← Lógica e controle das telas
    │       ├── validation/           ← Validações de regras de negócio
    │       └── util/                 ← Classes utilitárias e exportação PDF
    └── resources/
        ├── db/schema.sql             ← Script de criação das tabelas
        └── com/academia/view/        ← Telas (.fxml)
```

---

## 🔄 Padrão Observer (requisito do projeto)

O sistema notifica automaticamente o cabeçalho sempre que um pagamento é registrado:

```text
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
| JUnit         | 5       | Testes automatizados        |
| OpenPDF       | 1.3.36  | Exportação de relatórios PDF|
