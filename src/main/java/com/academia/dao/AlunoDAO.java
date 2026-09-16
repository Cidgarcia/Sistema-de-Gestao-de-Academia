package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela Alunos (UC 01).
 *
 * <p>Fornece operações CRUD completas para o cadastro de alunos.</p>
 */
public class AlunoDAO {

    /**
     * Insere um novo aluno no banco de dados.
     *
     * @param aluno Objeto {@link Aluno} com os dados a serem salvos.
     * @return {@code true} se inserido com sucesso.
     */
    public boolean inserir(Aluno aluno) {
        String sql = """
                INSERT INTO Alunos (nome, cpf, email, telefone, data_nascimento, observacoes)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, aluno.getNome());
            ps.setString(2, aluno.getCpf());
            ps.setString(3, aluno.getEmail());
            ps.setString(4, aluno.getTelefone());
            ps.setString(5, aluno.getDataNascimento());
            ps.setString(6, aluno.getObservacoes());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        aluno.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.inserir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Atualiza os dados de um aluno existente.
     *
     * @param aluno Objeto {@link Aluno} com os dados atualizados.
     * @return {@code true} se atualizado com sucesso.
     */
    public boolean atualizar(Aluno aluno) {
        String sql = """
                UPDATE Alunos SET nome = ?, cpf = ?, email = ?, telefone = ?,
                data_nascimento = ?, observacoes = ?
                WHERE id = ?
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, aluno.getNome());
            ps.setString(2, aluno.getCpf());
            ps.setString(3, aluno.getEmail());
            ps.setString(4, aluno.getTelefone());
            ps.setString(5, aluno.getDataNascimento());
            ps.setString(6, aluno.getObservacoes());
            ps.setInt(7, aluno.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.atualizar: " + e.getMessage());
        }
        return false;
    }

    /**
     * Exclui um aluno pelo seu ID.
     *
     * @param id ID do aluno a ser excluído.
     * @return {@code true} se excluído com sucesso.
     */
    public boolean excluir(int id) {
        String sql = "DELETE FROM Alunos WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.excluir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lista todos os alunos cadastrados.
     *
     * @return Lista de objetos {@link Aluno}.
     */
    public List<Aluno> listarTodos() {
        String sql = "SELECT * FROM Alunos ORDER BY nome";
        List<Aluno> alunos = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                alunos.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.listarTodos: " + e.getMessage());
        }
        return alunos;
    }

    /**
     * Busca alunos pelo nome (pesquisa parcial, case-insensitive).
     *
     * @param nome Parte do nome a ser buscada.
     * @return Lista de alunos correspondentes.
     */
    public List<Aluno> buscarPorNome(String nome) {
        String sql = "SELECT * FROM Alunos WHERE nome LIKE ? ORDER BY nome";
        List<Aluno> alunos = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nome + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alunos.add(mapearResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.buscarPorNome: " + e.getMessage());
        }
        return alunos;
    }

    /** Mapeia uma linha do ResultSet para um objeto {@link Aluno}. */
    private Aluno mapearResultSet(ResultSet rs) throws SQLException {
        return new Aluno(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cpf"),
                rs.getString("email"),
                rs.getString("telefone"),
                rs.getString("data_nascimento"),
                rs.getString("observacoes"),
                rs.getString("data_cadastro")
        );
    }
}

