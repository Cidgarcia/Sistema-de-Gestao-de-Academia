-- =============================================================
--  Script DDL — GymFlow
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
    senha    TEXT    NOT NULL,          -- PBKDF2 versionado (SHA-256 legado migra após login)
    perfil   TEXT    NOT NULL CHECK (perfil IN ('FUNCIONARIO', 'INSTRUTOR'))
);

-- Usuário administrador padrão (senha: admin123)
INSERT OR IGNORE INTO Usuarios (nome, login, senha, perfil)
VALUES ('Administrador', 'admin',
        'pbkdf2-sha256$600000$YVjaVN7bp4gLr/KXzbKmJg==$p4N8dZavB/tPCy25LmesMrjq0NE4ZRxfYSq8Z/Xq4Qg=',
        'FUNCIONARIO');

-- Usuário instrutor padrão (senha: admin123)
INSERT OR IGNORE INTO Usuarios (nome, login, senha, perfil)
VALUES ('Instrutor Padrão', 'instrutor',
        'pbkdf2-sha256$600000$TB5ifntl1/OXhFldtIZclQ==$mtpjIMYsMjUGTigVhrJMVEEQviDDd93y0xyJ/0el/TE=',
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
    valor       REAL    NOT NULL CHECK (valor > 0),
    duracao_dias INTEGER NOT NULL CHECK (duracao_dias > 0)
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
    ativa         INTEGER NOT NULL DEFAULT 1 CHECK (ativa IN (0, 1))
);

-- -------------------------------------------------------------
--  Tabela: Pagamentos
--  Registros de pagamentos recebidos no balcão.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Pagamentos (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    matricula_id  INTEGER NOT NULL REFERENCES Matriculas(id) ON DELETE CASCADE,
    valor_pago    REAL    NOT NULL CHECK (valor_pago > 0),
    data_pagamento TEXT   NOT NULL DEFAULT (date('now')),
    tipo_pagamento TEXT   NOT NULL DEFAULT 'OUTRO'
                              CHECK (tipo_pagamento IN ('MENSALIDADE', 'OUTRO', 'LEGADO')),
    forma_pagamento TEXT  NOT NULL
                              CHECK (forma_pagamento IN ('DINHEIRO', 'CARTAO', 'PIX')),
    observacoes   TEXT
);

-- Obrigações financeiras geradas para cada mensalidade da matrícula
CREATE TABLE IF NOT EXISTS PendenciasFinanceiras (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    matricula_id     INTEGER NOT NULL REFERENCES Matriculas(id) ON DELETE CASCADE,
    tipo             TEXT    NOT NULL DEFAULT 'MENSALIDADE',
    descricao        TEXT    NOT NULL,
    valor            REAL    NOT NULL CHECK (valor > 0),
    data_vencimento  TEXT    NOT NULL,
    situacao         TEXT    NOT NULL DEFAULT 'PENDENTE'
                              CHECK (situacao IN ('PENDENTE', 'PAGA', 'VENCIDA')),
    pagamento_id     INTEGER UNIQUE REFERENCES Pagamentos(id),
    UNIQUE (matricula_id, tipo, data_vencimento)
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
    peso_kg         REAL CHECK (peso_kg IS NULL OR peso_kg > 0),
    altura_cm       REAL CHECK (altura_cm IS NULL OR altura_cm > 0),
    imc             REAL CHECK (imc IS NULL OR imc > 0),
    gordura_perc    REAL CHECK (gordura_perc IS NULL OR gordura_perc >= 0),
    massa_muscular  REAL CHECK (massa_muscular IS NULL OR massa_muscular >= 0),
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
    ordem           INTEGER NOT NULL CHECK (ordem > 0),
    grupo_muscular  TEXT    NOT NULL,
    nome            TEXT    NOT NULL,
    series          INTEGER NOT NULL CHECK (series > 0),
    repeticoes      TEXT    NOT NULL,
    carga           TEXT,
    descanso        INTEGER CHECK (descanso IS NULL OR descanso >= 0),
    observacoes     TEXT
);

-- -------------------------------------------------------------
--  Tabela: Frequencias
--  Registra os acessos/entradas dos alunos na academia.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Frequencias (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    aluno_id          INTEGER NOT NULL REFERENCES Alunos(id) ON DELETE CASCADE,
    matricula_id      INTEGER REFERENCES Matriculas(id) ON DELETE SET NULL,
    data_hora_entrada TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Índices das consultas mais frequentes e garantia de matrícula ativa única
CREATE INDEX IF NOT EXISTS idx_alunos_nome ON Alunos(nome);
CREATE INDEX IF NOT EXISTS idx_matriculas_aluno ON Matriculas(aluno_id);
CREATE INDEX IF NOT EXISTS idx_pagamentos_matricula_data ON Pagamentos(matricula_id, data_pagamento);
CREATE INDEX IF NOT EXISTS idx_pendencias_matricula_situacao ON PendenciasFinanceiras(matricula_id, situacao);
CREATE INDEX IF NOT EXISTS idx_avaliacoes_aluno_data ON AvaliacoesFisicas(aluno_id, data_avaliacao);
CREATE INDEX IF NOT EXISTS idx_fichas_aluno ON FichasTreino(aluno_id);
CREATE INDEX IF NOT EXISTS idx_frequencias_aluno_data ON Frequencias(aluno_id, data_hora_entrada);
CREATE UNIQUE INDEX IF NOT EXISTS idx_matricula_ativa_unica
    ON Matriculas(aluno_id, plano_id) WHERE ativa = 1;
