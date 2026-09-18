package com.academia.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Classe responsável por gerenciar a conexão com o banco de dados SQLite.
 *
 * <p>Utiliza o padrão Singleton para garantir que apenas uma instância de
 * conexão exista durante toda a execução da aplicação.</p>
 *
 * <p>O banco de dados é criado automaticamente no diretório do usuário,
 * garantindo que funcione em qualquer máquina sem configuração adicional.</p>
 */
public class ConexaoSQLite {

    /** Caminho do arquivo do banco de dados (criado na pasta do usuário). */
    /** Instância única (Singleton) da conexão. */
    private static Connection instancia = null;

    // Construtor privado — impede instanciação direta
    private ConexaoSQLite() {}

    /**
     * Retorna a conexão ativa com o banco. Se não existir, cria uma nova
     * e executa o script de criação das tabelas (schema.sql).
     *
     * @return Objeto {@link Connection} do JDBC.
     * @throws RuntimeException se não for possível conectar ao banco.
     */
    public static Connection getConexao() {
        try {
            if (instancia == null || instancia.isClosed()) {
                String caminho = System.getProperty(
                        "academia.db.path", System.getProperty("user.home") + "/academia_db.sqlite");
                instancia = DriverManager.getConnection("jdbc:sqlite:" + caminho);
                // Habilita suporte a chaves estrangeiras no SQLite
                instancia.createStatement().execute("PRAGMA foreign_keys = ON;");
                // Executa o script DDL para criar as tabelas, se ainda não existirem
                executarSchema(instancia);
                executarMigracoes(instancia);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco de dados SQLite: " + e.getMessage(), e);
        }
        return instancia;
    }

    private static void executarMigracoes(Connection conexao) throws SQLException {
        boolean possuiEnderecoAluno = false;
        try (Statement stmt = conexao.createStatement();
             var rs = stmt.executeQuery("PRAGMA table_info(Alunos)")) {
            while (rs.next()) {
                if ("endereco".equals(rs.getString("name"))) possuiEnderecoAluno = true;
            }
        }
        if (!possuiEnderecoAluno) {
            try (Statement stmt = conexao.createStatement()) {
                stmt.execute("ALTER TABLE Alunos ADD COLUMN endereco TEXT");
            }
        }

        boolean possuiCondicoes = false;
        try (Statement stmt = conexao.createStatement();
             var rs = stmt.executeQuery("PRAGMA table_info(Planos)")) {
            while (rs.next()) {
                if ("condicoes_utilizacao".equals(rs.getString("name"))) possuiCondicoes = true;
            }
        }
        try (Statement stmt = conexao.createStatement()) {
            if (!possuiCondicoes) {
                stmt.execute("ALTER TABLE Planos ADD COLUMN condicoes_utilizacao TEXT");
            }
            stmt.executeUpdate("""
                    UPDATE Planos
                    SET condicoes_utilizacao = 'Plano pessoal e intransferível'
                    WHERE condicoes_utilizacao IS NULL OR trim(condicoes_utilizacao) = ''
                    """);
        }

        boolean possuiTipoPagamento = false;
        try (Statement stmt = conexao.createStatement();
             var rs = stmt.executeQuery("PRAGMA table_info(Pagamentos)")) {
            while (rs.next()) {
                if ("tipo_pagamento".equals(rs.getString("name"))) possuiTipoPagamento = true;
            }
        }
        if (!possuiTipoPagamento) {
            try (Statement stmt = conexao.createStatement()) {
                stmt.execute("ALTER TABLE Pagamentos ADD COLUMN tipo_pagamento TEXT NOT NULL DEFAULT 'OUTRO'");
                stmt.executeUpdate("UPDATE Pagamentos SET tipo_pagamento = 'LEGADO'");
            }
        }

        try (Statement stmt = conexao.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_alunos_nome ON Alunos(nome)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_matriculas_aluno ON Matriculas(aluno_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagamentos_matricula_data ON Pagamentos(matricula_id, data_pagamento)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pendencias_matricula_situacao ON PendenciasFinanceiras(matricula_id, situacao)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_avaliacoes_aluno_data ON AvaliacoesFisicas(aluno_id, data_avaliacao)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_fichas_aluno ON FichasTreino(aluno_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_frequencias_aluno_data ON Frequencias(aluno_id, data_hora_entrada)");

            // Triggers aplicam as novas regras também em bancos criados por versões anteriores.
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_perfil_usuario_insert
                    BEFORE INSERT ON Usuarios WHEN NEW.perfil NOT IN ('FUNCIONARIO', 'INSTRUTOR')
                    BEGIN SELECT RAISE(ABORT, 'Perfil de usuário inválido'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_perfil_usuario_update
                    BEFORE UPDATE OF perfil ON Usuarios WHEN NEW.perfil NOT IN ('FUNCIONARIO', 'INSTRUTOR')
                    BEGIN SELECT RAISE(ABORT, 'Perfil de usuário inválido'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_plano_positivo_insert
                    BEFORE INSERT ON Planos WHEN NEW.valor <= 0 OR NEW.duracao_dias <= 0
                    BEGIN SELECT RAISE(ABORT, 'Valor e duração do plano devem ser positivos'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_plano_positivo_update
                    BEFORE UPDATE OF valor, duracao_dias ON Planos WHEN NEW.valor <= 0 OR NEW.duracao_dias <= 0
                    BEGIN SELECT RAISE(ABORT, 'Valor e duração do plano devem ser positivos'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_pagamento_positivo_insert
                    BEFORE INSERT ON Pagamentos WHEN NEW.valor_pago <= 0
                    BEGIN SELECT RAISE(ABORT, 'O valor do pagamento deve ser positivo'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_pagamento_positivo_update
                    BEFORE UPDATE OF valor_pago ON Pagamentos WHEN NEW.valor_pago <= 0
                    BEGIN SELECT RAISE(ABORT, 'O valor do pagamento deve ser positivo'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_exercicio_positivo_insert
                    BEFORE INSERT ON ExerciciosTreino
                    WHEN NEW.ordem <= 0 OR NEW.series <= 0 OR NEW.descanso < 0
                    BEGIN SELECT RAISE(ABORT, 'Ordem e séries devem ser positivas'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS validar_exercicio_positivo_update
                    BEFORE UPDATE OF ordem, series, descanso ON ExerciciosTreino
                    WHEN NEW.ordem <= 0 OR NEW.series <= 0 OR NEW.descanso < 0
                    BEGIN SELECT RAISE(ABORT, 'Ordem e séries devem ser positivas'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS impedir_matricula_ativa_duplicada_insert
                    BEFORE INSERT ON Matriculas WHEN NEW.ativa = 1 AND EXISTS (
                        SELECT 1 FROM Matriculas
                        WHERE aluno_id = NEW.aluno_id AND plano_id = NEW.plano_id AND ativa = 1)
                    BEGIN SELECT RAISE(ABORT, 'Matrícula ativa duplicada'); END
                    """);
            stmt.execute("""
                    CREATE TRIGGER IF NOT EXISTS impedir_matricula_ativa_duplicada_update
                    BEFORE UPDATE OF aluno_id, plano_id, ativa ON Matriculas
                    WHEN NEW.ativa = 1 AND EXISTS (
                        SELECT 1 FROM Matriculas
                        WHERE aluno_id = NEW.aluno_id AND plano_id = NEW.plano_id
                          AND ativa = 1 AND id <> OLD.id)
                    BEGIN SELECT RAISE(ABORT, 'Matrícula ativa duplicada'); END
                    """);
        }
    }

    /**
     * Lê e executa o arquivo schema.sql localizado nos resources do projeto.
     * As instruções CREATE TABLE utilizam IF NOT EXISTS, sendo seguro executar
     * múltiplas vezes.
     *
     * @param conexao Conexão ativa com o banco.
     */
    private static void executarSchema(Connection conexao) {
        // Carrega o arquivo SQL a partir do classpath (resources)
        try (InputStream is = ConexaoSQLite.class
                .getResourceAsStream("/db/schema.sql")) {

            if (is == null) {
                System.err.println("[AVISO] schema.sql não encontrado no classpath.");
                return;
            }

            // Lê todo o conteúdo do arquivo como String
            String sql;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            // Executa cada instrução separada por ponto-e-vírgula
            try (Statement stmt = conexao.createStatement()) {
                for (String instrucao : sql.split(";")) {
                    String instrucaoLimpa = instrucao.strip();
                    if (!instrucaoLimpa.isEmpty()) {
                        stmt.execute(instrucaoLimpa);
                    }
                }
            }

            System.out.println("[INFO] Banco de dados inicializado com sucesso.");

        } catch (IOException | SQLException e) {
            System.err.println("[ERRO] Falha ao executar schema.sql: " + e.getMessage());
        }
    }

    /**
     * Fecha a conexão com o banco de dados de forma segura.
     * Deve ser chamado ao encerrar a aplicação.
     */
    public static void fecharConexao() {
        if (instancia != null) {
            try {
                instancia.close();
                instancia = null;
                System.out.println("[INFO] Conexão com o banco encerrada.");
            } catch (SQLException e) {
                System.err.println("[ERRO] Falha ao fechar conexão: " + e.getMessage());
            }
        }
    }
}
