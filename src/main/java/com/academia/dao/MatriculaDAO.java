package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Matricula;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela Matriculas (UC 03).
 *
 * <p>Vincula alunos a planos e controla o período de vigência das matrículas.</p>
 */
public class MatriculaDAO {

    /**
     * Efetua a matrícula de um aluno em um plano.
     *
     * @param matricula Objeto {@link Matricula} com os dados da matrícula.
     * @return {@code true} se inserido com sucesso.
     */
    public boolean inserir(Matricula matricula) {
        String sql = """
                INSERT INTO Matriculas (aluno_id, plano_id, data_inicio, data_fim, ativa)
                VALUES (?, ?, ?, ?, 1)
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, matricula.getAlunoId());
            ps.setInt(2, matricula.getPlanoId());
            ps.setString(3, matricula.getDataInicio());
            ps.setString(4, matricula.getDataFim());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) matricula.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] MatriculaDAO.inserir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lista todas as matrículas com nome do aluno e plano (JOIN).
     *
     * @return Lista de objetos {@link Matricula}.
     */
    public List<Matricula> listarTodas() {
        String sql = """
                SELECT m.*, a.nome AS nome_aluno, p.nome AS nome_plano
                FROM Matriculas m
                JOIN Alunos a ON m.aluno_id = a.id
                JOIN Planos p ON m.plano_id = p.id
                ORDER BY m.data_inicio DESC
                """;
        List<Matricula> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] MatriculaDAO.listarTodas: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Lista as matrículas de um aluno específico.
     *
     * @param alunoId ID do aluno.
     * @return Lista de matrículas do aluno.
     */
    public List<Matricula> listarPorAluno(int alunoId) {
        String sql = """
                SELECT m.*, a.nome AS nome_aluno, p.nome AS nome_plano
                FROM Matriculas m
                JOIN Alunos a ON m.aluno_id = a.id
                JOIN Planos p ON m.plano_id = p.id
                WHERE m.aluno_id = ?
                ORDER BY m.data_inicio DESC
                """;
        List<Matricula> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] MatriculaDAO.listarPorAluno: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Cancela uma matrícula (soft delete: define ativa = 0).
     *
     * @param id ID da matrícula a ser cancelada.
     * @return {@code true} se cancelada com sucesso.
     */
    public boolean cancelar(int id) {
        String sql = "UPDATE Matriculas SET ativa = 0 WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] MatriculaDAO.cancelar: " + e.getMessage());
        }
        return false;
    }

    /** Mapeia uma linha do ResultSet para um objeto {@link Matricula}. */
    private Matricula mapearResultSet(ResultSet rs) throws SQLException {
        Matricula m = new Matricula(
                rs.getInt("id"),
                rs.getInt("aluno_id"),
                rs.getInt("plano_id"),
                rs.getString("data_inicio"),
                rs.getString("data_fim"),
                rs.getInt("ativa") == 1
        );
        m.setNomeAluno(rs.getString("nome_aluno"));
        m.setNomePlano(rs.getString("nome_plano"));
        return m;
    }
}

