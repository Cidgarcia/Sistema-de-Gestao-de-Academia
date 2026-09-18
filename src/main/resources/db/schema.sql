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
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    nome               TEXT    NOT NULL,
    cpf                TEXT    UNIQUE,
    email              TEXT,
    telefone           TEXT,
    endereco           TEXT,               
    data_nascimento    TEXT,               -- formato ISO: YYYY-MM-DD
    observacoes        TEXT,
    data_cadastro      TEXT    NOT NULL DEFAULT (date('now'))
);

-- -------------------------------------------------------------
--  Tabela: Planos
--  Opções de planos disponíveis na academia.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Planos (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nome        TEXT    NOT NULL,
    descricao   TEXT,
    condicoes_utilizacao TEXT,
    valor       REAL    NOT NULL,
    duracao_dias INTEGER NOT NULL       -- ex.: 30, 90, 180, 365
);

-- Planos padrão da academia — cada INSERT separado para evitar
-- quebra do parser que divide por ponto-e-vírgula
INSERT OR IGNORE INTO Planos (id, nome, descricao, valor, duracao_dias)
VALUES (2, 'Básico', 'Musculação Livre|Aeróbico', 120.00, 30);
INSERT OR IGNORE INTO Planos (id, nome, descricao, valor, duracao_dias)
VALUES (3, 'Trimestral', 'Musculação Livre|Aeróbico|1 Aula Coletiva/sem', 315.00, 90);
INSERT OR IGNORE INTO Planos (id, nome, descricao, valor, duracao_dias)
VALUES (4, 'Premium', 'Acesso Total 24/7|Aulas Coletivas Livres|Avaliação Física Inclusa', 1068.00, 365);

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

-- -------------------------------------------------------------
--  Tabela: FichasTreino
--  Representa as abas/divisões de treino de um aluno (ex: "Treino A").
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS FichasTreino (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    aluno_id        INTEGER NOT NULL REFERENCES Alunos(id) ON DELETE CASCADE,
    nome_divisao    TEXT    NOT NULL,
    data_atualizacao TEXT   NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- -------------------------------------------------------------
--  Tabela: ExerciciosTreino
--  Representa os exercícios de uma determinada ficha/divisão.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ExerciciosTreino (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    ficha_id        INTEGER NOT NULL REFERENCES FichasTreino(id) ON DELETE CASCADE,
    ordem           INTEGER NOT NULL,
    grupo_muscular  TEXT    NOT NULL,
    nome            TEXT    NOT NULL,
    series          INTEGER NOT NULL,
    repeticoes      TEXT    NOT NULL,
    carga           TEXT,
    descanso        INTEGER,
    observacoes     TEXT
);

-- -------------------------------------------------------------
--  Tabela: Frequencias
--  Registra os acessos/entradas dos alunos na academia.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Frequencias (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    aluno_id          INTEGER NOT NULL REFERENCES Alunos(id) ON DELETE CASCADE,
    data_hora_entrada TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);
