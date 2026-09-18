package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Pagamento;
import com.academia.model.PendenciaFinanceira;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela Pagamentos (UC 04).
 *
 * <p>Registra pagamentos e verifica mensalidades atrasadas (vencidas).</p>
 */
public class PagamentoDAO {

    public enum ResultadoPagamento {
        SUCESSO, PENDENCIA_INEXISTENTE, PENDENCIA_JA_PAGA, ERRO
    }

    /**
     * Insere um novo pagamento no banco de dados.
     *
     * @param pagamento Objeto {@link Pagamento} a ser salvo.
     * @return {@code true} se inserido com sucesso.
     */
    public boolean inserir(Pagamento pagamento) {
        return registrarPagamento(pagamento, null) == ResultadoPagamento.SUCESSO;
    }

    /** Registra o pagamento e, quando mensalidade, baixa a pendência na mesma transação. */
    public ResultadoPagamento registrarPagamento(Pagamento pagamento, Integer pendenciaId) {
        String sql = """
                INSERT INTO Pagamentos
                    (matricula_id, valor_pago, data_pagamento, tipo_pagamento, forma_pagamento, observacoes)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        if (pagamento.getValorPago() <= 0) return ResultadoPagamento.ERRO;

        Connection conn = ConexaoSQLite.getConexao();
        try {
            conn.setAutoCommit(false);

            if ("MENSALIDADE".equals(pagamento.getTipo())) {
                if (pendenciaId == null) return encerrar(conn, ResultadoPagamento.PENDENCIA_INEXISTENTE);
                String situacao = buscarSituacaoPendencia(conn, pendenciaId, pagamento.getMatriculaId());
                if (situacao == null) return encerrar(conn, ResultadoPagamento.PENDENCIA_INEXISTENTE);
                if ("PAGA".equals(situacao)) return encerrar(conn, ResultadoPagamento.PENDENCIA_JA_PAGA);
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, pagamento.getMatriculaId());
                ps.setDouble(2, pagamento.getValorPago());
                ps.setString(3, pagamento.getDataPagamento());
                ps.setString(4, pagamento.getTipo());
                ps.setString(5, pagamento.getFormaPagamento());
                ps.setString(6, pagamento.getObservacoes());

                if (ps.executeUpdate() == 0) return encerrar(conn, ResultadoPagamento.ERRO);
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) pagamento.setId(keys.getInt(1));
                }
            }

            if (pendenciaId != null && "MENSALIDADE".equals(pagamento.getTipo())) {
                try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE PendenciasFinanceiras
                        SET situacao = 'PAGA', pagamento_id = ?
                        WHERE id = ? AND situacao <> 'PAGA'
                        """)) {
                    ps.setInt(1, pagamento.getId());
                    ps.setInt(2, pendenciaId);
                    if (ps.executeUpdate() == 0) return encerrar(conn, ResultadoPagamento.PENDENCIA_JA_PAGA);
                }
            }

            conn.commit();
            return ResultadoPagamento.SUCESSO;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            System.err.println("[ERRO] PagamentoDAO.registrarPagamento: " + e.getMessage());
            return ResultadoPagamento.ERRO;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private String buscarSituacaoPendencia(Connection conn, int pendenciaId, int matriculaId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT situacao FROM PendenciasFinanceiras WHERE id = ? AND matricula_id = ?")) {
            ps.setInt(1, pendenciaId);
            ps.setInt(2, matriculaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("situacao") : null;
            }
        }
    }

    private ResultadoPagamento encerrar(Connection conn, ResultadoPagamento resultado) throws SQLException {
        conn.rollback();
        return resultado;
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

    /** Gera as mensalidades ainda ausentes para matrículas novas ou antigas. */
    public void sincronizarPendenciasMensais() {
        String sql = """
                SELECT m.id, m.data_inicio, p.nome, p.valor, p.duracao_dias
                FROM Matriculas m
                JOIN Planos p ON m.plano_id = p.id
                """;
        String inserir = """
                INSERT OR IGNORE INTO PendenciasFinanceiras
                    (matricula_id, tipo, descricao, valor, data_vencimento, situacao)
                VALUES (?, 'MENSALIDADE', ?, ?, ?, 'PENDENTE')
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             PreparedStatement ps = conn.prepareStatement(inserir)) {

            while (rs.next()) {
                int parcelas = Math.max(1, (int) Math.ceil(rs.getInt("duracao_dias") / 30.0));
                double valorMensal = rs.getDouble("valor") / parcelas;
                LocalDate inicio = LocalDate.parse(rs.getString("data_inicio"));
                for (int i = 0; i < parcelas; i++) {
                    ps.setInt(1, rs.getInt("id"));
                    ps.setString(2, "Mensalidade " + (i + 1) + "/" + parcelas + " - " + rs.getString("nome"));
                    ps.setDouble(3, valorMensal);
                    ps.setString(4, inicio.plusMonths(i).toString());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            reconciliarPagamentosLegados(conn);
            try (Statement atualizar = conn.createStatement()) {
                atualizar.executeUpdate("""
                        UPDATE PendenciasFinanceiras
                        SET situacao = CASE
                            WHEN situacao = 'PAGA' THEN 'PAGA'
                            WHEN data_vencimento < date('now') THEN 'VENCIDA'
                            ELSE 'PENDENTE'
                        END
                        """);
            }

        } catch (SQLException e) {
            System.err.println("[ERRO] PagamentoDAO.sincronizarPendenciasMensais: " + e.getMessage());
        }
    }

    private void reconciliarPagamentosLegados(Connection conn) throws SQLException {
        record Legado(int id, int matriculaId, String dataPagamento) {}
        List<Legado> legados = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT id, matricula_id, data_pagamento
                FROM Pagamentos WHERE tipo_pagamento = 'LEGADO'
                ORDER BY data_pagamento, id
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) legados.add(new Legado(
                    rs.getInt("id"), rs.getInt("matricula_id"), rs.getString("data_pagamento")));
        }

        String buscarPendencia = """
                SELECT id FROM PendenciasFinanceiras
                WHERE matricula_id = ? AND situacao <> 'PAGA' AND pagamento_id IS NULL
                ORDER BY CASE WHEN data_vencimento <= ? THEN 0 ELSE 1 END, data_vencimento
                LIMIT 1
                """;
        try (PreparedStatement buscar = conn.prepareStatement(buscarPendencia);
             PreparedStatement baixar = conn.prepareStatement("""
                     UPDATE PendenciasFinanceiras SET situacao = 'PAGA', pagamento_id = ? WHERE id = ?
                     """);
             PreparedStatement classificar = conn.prepareStatement(
                     "UPDATE Pagamentos SET tipo_pagamento = ? WHERE id = ?")) {
            for (Legado legado : legados) {
                buscar.setInt(1, legado.matriculaId());
                buscar.setString(2, legado.dataPagamento());
                Integer pendenciaId = null;
                try (ResultSet rs = buscar.executeQuery()) {
                    if (rs.next()) pendenciaId = rs.getInt("id");
                }
                if (pendenciaId != null) {
                    baixar.setInt(1, legado.id());
                    baixar.setInt(2, pendenciaId);
                    baixar.executeUpdate();
                }
                classificar.setString(1, pendenciaId == null ? "OUTRO" : "MENSALIDADE");
                classificar.setInt(2, legado.id());
                classificar.executeUpdate();
            }
        }
    }

    /** Lista pendências, opcionalmente filtradas por matrícula. */
    public List<PendenciaFinanceira> listarPendencias(Integer matriculaId) {
        sincronizarPendenciasMensais();
        String sql = """
                SELECT pf.*, a.nome AS nome_aluno
                FROM PendenciasFinanceiras pf
                JOIN Matriculas m ON pf.matricula_id = m.id
                JOIN Alunos a ON m.aluno_id = a.id
                WHERE (? IS NULL OR pf.matricula_id = ?)
                ORDER BY pf.data_vencimento, a.nome
                """;
        List<PendenciaFinanceira> lista = new ArrayList<>();
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (matriculaId == null) {
                ps.setNull(1, Types.INTEGER);
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(1, matriculaId);
                ps.setInt(2, matriculaId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearPendencia(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] PagamentoDAO.listarPendencias: " + e.getMessage());
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
        p.setTipo(rs.getString("tipo_pagamento"));
        return p;
    }

    private PendenciaFinanceira mapearPendencia(ResultSet rs) throws SQLException {
        PendenciaFinanceira p = new PendenciaFinanceira();
        p.setId(rs.getInt("id"));
        p.setMatriculaId(rs.getInt("matricula_id"));
        p.setTipo(rs.getString("tipo"));
        p.setDescricao(rs.getString("descricao"));
        p.setValor(rs.getDouble("valor"));
        p.setDataVencimento(rs.getString("data_vencimento"));
        p.setSituacao(rs.getString("situacao"));
        p.setNomeAluno(rs.getString("nome_aluno"));
        return p;
    }
}
