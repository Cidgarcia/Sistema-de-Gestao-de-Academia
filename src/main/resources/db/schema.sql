-- =============================================================
--  Script DDL — Sistema de Gestão de Academia
--  Sprint 1 | Criação das tabelas no SQLite
-- =============================================================

-- Habilita suporte a chaves estrangeiras no SQLite
PRAGMA foreign_keys = ON;

-- -------------------------------------------------------------
--  Tabela: Usuarios
--  Armazena os usuários do sistema (Funcionário ou Instrutor).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Usuarios (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    nome     TEXT    NOT NULL,
    login    TEXT    NOT NULL UNIQUE,
    senha    TEXT    NOT NULL,          -- senha armazenada com hash simples (SHA-256)
    perfil   TEXT    NOT NULL           -- 'FUNCIONARIO' ou 'INSTRUTOR'
);

-- Usuário administrador padrão (senha: admin123)
INSERT OR IGNORE INTO Usuarios (nome, login, senha, perfil)
VALUES ('Administrador', 'admin',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'FUNCIONARIO');

-- Usuário instrutor padrão (senha: admin123)
INSERT OR IGNORE INTO Usuarios (nome, login, senha, perfil)
VALUES ('Instrutor Padrão', 'instrutor',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'INSTRUTOR');

-- -------------------------------------------------------------
--  Tabela: Alunos
--  Dados cadastrais dos alunos da academia.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Alunos (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    nome             TEXT    NOT NULL,
    cpf              TEXT    UNIQUE,
    email            TEXT,
    telefone         TEXT,
    data_nascimento  TEXT,              -- formato ISO: YYYY-MM-DD
    observacoes      TEXT,
    data_cadastro    TEXT    NOT NULL DEFAULT (date('now'))
);

-- -------------------------------------------------------------
--  Tabela: Planos
--  Opções de planos disponíveis na academia.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Planos (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nome        TEXT    NOT NULL,
    descricao   TEXT,
    valor       REAL    NOT NULL,
    duracao_dias INTEGER NOT NULL       -- ex.: 30, 90, 180, 365
);

-- -------------------------------------------------------------
--  Tabela: Matriculas
--  Vincula um aluno a um plano, com datas de início e fim.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Matriculas (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    aluno_id      INTEGER NOT NULL REFERENCES Alunos(id)  ON DELETE CASCADE,
    plano_id      INTEGER NOT NULL REFERENCES Planos(id)  ON DELETE RESTRICT,
    data_inicio   TEXT    NOT NULL,    -- YYYY-MM-DD
    data_fim      TEXT    NOT NULL,    -- calculada: data_inicio + duracao_dias
    ativa         INTEGER NOT NULL DEFAULT 1  -- 1 = ativa, 0 = cancelada
);

-- -------------------------------------------------------------
--  Tabela: Pagamentos
--  Registros de pagamentos recebidos no balcão.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Pagamentos (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    matricula_id  INTEGER NOT NULL REFERENCES Matriculas(id) ON DELETE CASCADE,
    valor_pago    REAL    NOT NULL,
    data_pagamento TEXT   NOT NULL DEFAULT (date('now')),
    forma_pagamento TEXT  NOT NULL,    -- 'DINHEIRO', 'CARTAO', 'PIX'
    observacoes   TEXT
);

-- -------------------------------------------------------------
--  Tabela: AvaliacoesFisicas
--  Medidas e progresso dos alunos (acesso restrito a Instrutores).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS AvaliacoesFisicas (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    aluno_id        INTEGER NOT NULL REFERENCES Alunos(id) ON DELETE CASCADE,
    instrutor_id    INTEGER NOT NULL REFERENCES Usuarios(id),
    data_avaliacao  TEXT    NOT NULL DEFAULT (date('now')),
    peso_kg         REAL,
    altura_cm       REAL,
    imc             REAL,              -- calculado automaticamente pela aplicação
    gordura_perc    REAL,
    massa_muscular  REAL,
    observacoes     TEXT
);

