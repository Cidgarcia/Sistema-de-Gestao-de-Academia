package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Pagamento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela Pagamentos (UC 04).
 *
 * <p>Registra pagamentos e verifica mensalidades atrasadas (vencidas).</p>
 */
public class PagamentoDAO {

    /**
     * Insere um novo pagamento no banco de dados.
     *
     * @param pagamento Objeto {@link Pagamento} a ser salvo.
     * @return {@code true} se inserido com sucesso.
     */
    public boolean inserir(Pagamento pagamento) {
        String sql = """
                INSERT INTO Pagamentos (matricula_id, valor_pago, data_pagamento, forma_pagamento, observacoes)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, pagamento.getMatriculaId());
            ps.setDouble(2, pagamento.getValorPago());
            ps.setString(3, pagamento.getDataPagamento());
            ps.setString(4, pagamento.getFormaPagamento());
            ps.setString(5, pagamento.getObservacoes());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) pagamento.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] PagamentoDAO.inserir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lista todos os pagamentos com o nome do aluno (JOIN).
     *
     * @return Lista de objetos {@link Pagamento}.
     */
    public List<Pagamento> listarTodos() {
        String sql = """
                SELECT p.*, a.nome AS nome_aluno
                FROM Pagamentos p
                JOIN Matriculas m ON p.matricula_id = m.id
                JOIN Alunos a ON m.aluno_id = a.id
                ORDER BY p.data_pagamento DESC
                """;
        List<Pagamento> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) lista.add(mapearResultSet(rs));

        } catch (SQLException e) {
            System.err.println("[ERRO] PagamentoDAO.listarTodos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Calcula o total de pagamentos recebidos na data informada.
     *
     * @param data Data no formato YYYY-MM-DD.
     * @return Soma dos valores pagos na data.
     */
    public double totalRecebidoNoDia(String data) {
        String sql = "SELECT COALESCE(SUM(valor_pago), 0) AS total FROM Pagamentos WHERE data_pagamento = ?";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, data);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] PagamentoDAO.totalRecebidoNoDia: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Lista alunos com matrículas ativas cujo prazo já venceu (mensalidades atrasadas).
     *
     * <p>Retorna matrículas ativas cuja data_fim já passou e que não possuem
     * nenhum pagamento registrado após o vencimento.</p>
     *
     * @return Lista de matrículas vencidas sem pagamento recente.
     */
    public List<Pagamento> listarAtrasados() {
        String sql = """
                SELECT 0 AS id, m.id AS matricula_id, 0.0 AS valor_pago,
                       date('now') AS data_pagamento, '' AS forma_pagamento,
                       'ATRASADO' AS observacoes, a.nome AS nome_aluno
                FROM Matriculas m
                JOIN Alunos a ON m.aluno_id = a.id
                WHERE m.ativa = 1
                  AND m.data_fim < date('now')
                ORDER BY m.data_fim ASC
                """;
        List<Pagamento> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) lista.add(mapearResultSet(rs));

        } catch (SQLException e) {
            System.err.println("[ERRO] PagamentoDAO.listarAtrasados: " + e.getMessage());
        }
        return lista;
    }

    /** Mapeia uma linha do ResultSet para um objeto {@link Pagamento}. */
    private Pagamento mapearResultSet(ResultSet rs) throws SQLException {
        Pagamento p = new Pagamento(
                rs.getInt("id"),
                rs.getInt("matricula_id"),
                rs.getDouble("valor_pago"),
                rs.getString("data_pagamento"),
                rs.getString("forma_pagamento"),
                rs.getString("observacoes")
        );
        p.setNomeAluno(rs.getString("nome_aluno"));
        return p;
    }
}

