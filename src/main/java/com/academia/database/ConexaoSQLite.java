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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco de dados SQLite: " + e.getMessage(), e);
        }
        return instancia;
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
