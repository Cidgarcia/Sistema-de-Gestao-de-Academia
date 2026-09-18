package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Plano;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela Planos (UC 02).
 *
 * <p>Fornece operações CRUD para o gerenciamento dos planos da academia.</p>
 */
public class PlanoDAO {

    /**
     * Insere um novo plano no banco de dados.
     *
     * @param plano Objeto {@link Plano} a ser salvo.
     * @return {@code true} se inserido com sucesso.
     */
    public boolean inserir(Plano plano) {
        String sql = """
                INSERT INTO Planos (nome, descricao, condicoes_utilizacao, valor, duracao_dias)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, plano.getNome());
            ps.setString(2, plano.getDescricao());
            ps.setString(3, plano.getCondicoesUtilizacao());
            ps.setDouble(4, plano.getValor());
            ps.setInt(5, plano.getDuracaoDias());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) plano.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] PlanoDAO.inserir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Atualiza um plano existente.
     *
     * @param plano Objeto {@link Plano} com os dados atualizados.
     * @return {@code true} se atualizado com sucesso.
     */
    public boolean atualizar(Plano plano) {
        String sql = """
                UPDATE Planos SET nome = ?, descricao = ?, condicoes_utilizacao = ?, valor = ?, duracao_dias = ?
                WHERE id = ?
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, plano.getNome());
            ps.setString(2, plano.getDescricao());
            ps.setString(3, plano.getCondicoesUtilizacao());
            ps.setDouble(4, plano.getValor());
            ps.setInt(5, plano.getDuracaoDias());
            ps.setInt(6, plano.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] PlanoDAO.atualizar: " + e.getMessage());
        }
        return false;
    }

    /**
     * Exclui um plano pelo ID.
     *
     * @param id ID do plano a ser excluído.
     * @return {@code true} se excluído com sucesso.
     */
    public boolean excluir(int id) {
        String sql = "DELETE FROM Planos WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] PlanoDAO.excluir: " + e.getMessage());
        }
        return false;
    }

    /** Informa se o plano possui matrículas vinculadas. */
    public boolean estaEmUso(int id) {
        String sql = "SELECT 1 FROM Matriculas WHERE plano_id = ? LIMIT 1";
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] PlanoDAO.estaEmUso: " + e.getMessage());
            return true;
        }
    }

    /**
     * Lista todos os planos cadastrados.
     *
     * @return Lista de objetos {@link Plano}.
     */
    public List<Plano> listarTodos() {
        String sql = "SELECT * FROM Planos ORDER BY valor";
        List<Plano> planos = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                planos.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] PlanoDAO.listarTodos: " + e.getMessage());
        }
        return planos;
    }

    /** Mapeia uma linha do ResultSet para um objeto {@link Plano}. */
    private Plano mapearResultSet(ResultSet rs) throws SQLException {
        return new Plano(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getString("condicoes_utilizacao"),
                rs.getDouble("valor"),
                rs.getInt("duracao_dias")
        );
    }
}
