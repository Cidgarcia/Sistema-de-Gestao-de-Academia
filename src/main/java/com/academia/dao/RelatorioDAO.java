package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado na recuperação e consolidação de dados para Relatórios.
 */
public class RelatorioDAO {

    /**
     * Consulta alunos que possuem matrícula ativa no dia atual.
     *
     * @param filtroTexto Nome ou CPF do aluno (opcional).
     * @param planoId     ID do plano para filtro (opcional).
     * @return Lista de alunos ativos.
     */
    public List<RelatorioAlunoAtivoDTO> listarAlunosAtivos(String filtroTexto, Integer planoId) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id AS aluno_id, a.nome, a.cpf, a.telefone, a.email,
                       p.nome AS plano_nome, m.data_inicio, m.data_fim,
                       CAST((julianday(m.data_fim) - julianday(date('now', 'localtime'))) AS INTEGER) AS dias_restantes
                FROM Alunos a
                JOIN Matriculas m ON a.id = m.aluno_id
                JOIN Planos p ON m.plano_id = p.id
                WHERE m.ativa = 1
                  AND date('now', 'localtime') BETWEEN m.data_inicio AND m.data_fim
                """);

        List<Object> params = new ArrayList<>();

        if (filtroTexto != null && !filtroTexto.trim().isEmpty()) {
            String termo = "%" + filtroTexto.trim() + "%";
            sql.append(" AND (a.nome LIKE ? OR a.cpf LIKE ?)");
            params.add(termo);
            params.add(termo);
        }

        if (planoId != null && planoId > 0) {
            sql.append(" AND m.plano_id = ?");
            params.add(planoId);
        }

        sql.append(" ORDER BY a.nome ASC");

        List<RelatorioAlunoAtivoDTO> lista = new ArrayList<>();
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new RelatorioAlunoAtivoDTO(
                            rs.getInt("aluno_id"),
                            rs.getString("nome"),
                            rs.getString("cpf"),
                            rs.getString("telefone"),
                            rs.getString("email"),
                            rs.getString("plano_nome"),
                            rs.getString("data_inicio"),
                            rs.getString("data_fim"),
                            rs.getInt("dias_restantes")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] RelatorioDAO.listarAlunosAtivos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Consulta relatório de matrículas com filtros por período, situação, plano e texto.
     */
    public List<RelatorioMatriculaDTO> listarMatriculas(LocalDate inicio, LocalDate fim,
                                                        String situacao, Integer planoId, String filtroTexto) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.id AS matricula_id, a.nome AS aluno_nome, a.cpf,
                       p.nome AS plano_nome, p.valor AS plano_valor,
                       m.data_inicio, m.data_fim, m.ativa
                FROM Matriculas m
                JOIN Alunos a ON m.aluno_id = a.id
                JOIN Planos p ON m.plano_id = p.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (inicio != null) {
            sql.append(" AND m.data_inicio >= ?");
            params.add(inicio.toString());
        }
        if (fim != null) {
            sql.append(" AND m.data_inicio <= ?");
            params.add(fim.toString());
        }
        if (planoId != null && planoId > 0) {
            sql.append(" AND m.plano_id = ?");
            params.add(planoId);
        }
        if (filtroTexto != null && !filtroTexto.trim().isEmpty()) {
            String termo = "%" + filtroTexto.trim() + "%";
            sql.append(" AND (a.nome LIKE ? OR a.cpf LIKE ?)");
            params.add(termo);
            params.add(termo);
        }

        if (situacao != null && !situacao.isBlank() && !situacao.equalsIgnoreCase("TODAS")) {
            switch (situacao.toUpperCase()) {
                case "ATIVAS" -> sql.append(" AND m.ativa = 1 AND date('now', 'localtime') BETWEEN m.data_inicio AND m.data_fim");
                case "VENCIDAS" -> sql.append(" AND m.ativa = 1 AND m.data_fim < date('now', 'localtime')");
                case "CANCELADAS" -> sql.append(" AND m.ativa = 0");
            }
        }

        sql.append(" ORDER BY m.data_inicio DESC, m.id DESC");

        List<RelatorioMatriculaDTO> lista = new ArrayList<>();
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            LocalDate hoje = LocalDate.now();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean ativa = rs.getInt("ativa") == 1;
                    String dataFimStr = rs.getString("data_fim");
                    String sit;
                    if (!ativa) {
                        sit = "Cancelada";
                    } else if (dataFimStr != null && LocalDate.parse(dataFimStr).isBefore(hoje)) {
                        sit = "Vencida";
                    } else {
                        sit = "Ativa";
                    }

                    lista.add(new RelatorioMatriculaDTO(
                            rs.getInt("matricula_id"),
                            rs.getString("aluno_nome"),
                            rs.getString("cpf"),
                            rs.getString("plano_nome"),
                            rs.getDouble("plano_valor"),
                            rs.getString("data_inicio"),
                            dataFimStr,
                            sit
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[ERRO] RelatorioDAO.listarMatriculas: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Consulta relatório de pagamentos por período, forma de pagamento, tipo e aluno.
     */
    public List<RelatorioPagamentoDTO> listarPagamentos(LocalDate inicio, LocalDate fim,
                                                        String formaPagamento, String tipo, String filtroTexto) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id AS pagamento_id, p.matricula_id, a.nome AS aluno_nome,
                       p.data_pagamento, p.tipo_pagamento, p.forma_pagamento,
                       p.valor_pago, p.observacoes
                FROM Pagamentos p
                JOIN Matriculas m ON p.matricula_id = m.id
                JOIN Alunos a ON m.aluno_id = a.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (inicio != null) {
            sql.append(" AND p.data_pagamento >= ?");
            params.add(inicio.toString());
        }
        if (fim != null) {
            sql.append(" AND p.data_pagamento <= ?");
            params.add(fim.toString());
        }
        if (formaPagamento != null && !formaPagamento.isBlank() && !formaPagamento.equalsIgnoreCase("TODAS")) {
            sql.append(" AND p.forma_pagamento = ?");
            params.add(formaPagamento.toUpperCase());
        }
        if (tipo != null && !tipo.isBlank() && !tipo.equalsIgnoreCase("TODOS")) {
            sql.append(" AND p.tipo_pagamento = ?");
            params.add(tipo.toUpperCase());
        }
        if (filtroTexto != null && !filtroTexto.trim().isEmpty()) {
            String termo = "%" + filtroTexto.trim() + "%";
            sql.append(" AND (a.nome LIKE ? OR a.cpf LIKE ?)");
            params.add(termo);
            params.add(termo);
        }

        sql.append(" ORDER BY p.data_pagamento DESC, p.id DESC");

        List<RelatorioPagamentoDTO> lista = new ArrayList<>();
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new RelatorioPagamentoDTO(
                            rs.getInt("pagamento_id"),
                            rs.getInt("matricula_id"),
                            rs.getString("aluno_nome"),
                            rs.getString("data_pagamento"),
                            rs.getString("tipo_pagamento"),
                            rs.getString("forma_pagamento"),
                            rs.getDouble("valor_pago"),
                            rs.getString("observacoes")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] RelatorioDAO.listarPagamentos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Consulta relatório de frequência/acessos por período e aluno.
     */
    public List<RelatorioFrequenciaDTO> listarFrequencias(LocalDate inicio, LocalDate fim, String filtroTexto) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id AS frequencia_id, f.data_hora_entrada, a.nome AS aluno_nome, a.cpf,
                       m.id AS matricula_id, p.nome AS plano_nome
                FROM Frequencias f
                JOIN Alunos a ON f.aluno_id = a.id
                LEFT JOIN Matriculas m ON f.matricula_id = m.id
                LEFT JOIN Planos p ON m.plano_id = p.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (inicio != null) {
            sql.append(" AND substr(f.data_hora_entrada, 1, 10) >= ?");
            params.add(inicio.toString());
        }
        if (fim != null) {
            sql.append(" AND substr(f.data_hora_entrada, 1, 10) <= ?");
            params.add(fim.toString());
        }
        if (filtroTexto != null && !filtroTexto.trim().isEmpty()) {
            String termo = "%" + filtroTexto.trim() + "%";
            sql.append(" AND (a.nome LIKE ? OR a.cpf LIKE ?)");
            params.add(termo);
            params.add(termo);
        }

        sql.append(" ORDER BY f.data_hora_entrada DESC, f.id DESC");

        List<RelatorioFrequenciaDTO> lista = new ArrayList<>();
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int matId = rs.getInt("matricula_id");
                    String plano = rs.getString("plano_nome");
                    String matPlano = (matId > 0 && plano != null) ? "#" + matId + " (" + plano + ")" : "Avulso / Legado";
                    String status = matId > 0 ? "Liberado" : "Registrado";

                    lista.add(new RelatorioFrequenciaDTO(
                            rs.getInt("frequencia_id"),
                            rs.getString("data_hora_entrada"),
                            rs.getString("aluno_nome"),
                            rs.getString("cpf"),
                            matPlano,
                            status
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] RelatorioDAO.listarFrequencias: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Consulta relatório de avaliações físicas por período, aluno e texto.
     */
    public List<RelatorioAvaliacaoDTO> listarAvaliacoes(LocalDate inicio, LocalDate fim,
                                                        Integer alunoId, String filtroTexto) {
        StringBuilder sql = new StringBuilder("""
                SELECT af.id AS avaliacao_id, af.data_avaliacao, a.nome AS aluno_nome, u.nome AS instrutor_nome,
                       af.peso_kg, af.altura_cm, af.imc, af.gordura_perc, af.massa_muscular, af.observacoes
                FROM AvaliacoesFisicas af
                JOIN Alunos a ON af.aluno_id = a.id
                JOIN Usuarios u ON af.instrutor_id = u.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (inicio != null) {
            sql.append(" AND af.data_avaliacao >= ?");
            params.add(inicio.toString());
        }
        if (fim != null) {
            sql.append(" AND af.data_avaliacao <= ?");
            params.add(fim.toString());
        }
        if (alunoId != null && alunoId > 0) {
            sql.append(" AND af.aluno_id = ?");
            params.add(alunoId);
        }
        if (filtroTexto != null && !filtroTexto.trim().isEmpty()) {
            String termo = "%" + filtroTexto.trim() + "%";
            sql.append(" AND (a.nome LIKE ? OR a.cpf LIKE ?)");
            params.add(termo);
            params.add(termo);
        }

        sql.append(" ORDER BY af.data_avaliacao DESC, af.id DESC");

        List<RelatorioAvaliacaoDTO> lista = new ArrayList<>();
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new RelatorioAvaliacaoDTO(
                            rs.getInt("avaliacao_id"),
                            rs.getString("data_avaliacao"),
                            rs.getString("aluno_nome"),
                            rs.getString("instrutor_nome"),
                            (Double) rs.getObject("peso_kg"),
                            (Double) rs.getObject("altura_cm"),
                            (Double) rs.getObject("imc"),
                            (Double) rs.getObject("gordura_perc"),
                            (Double) rs.getObject("massa_muscular"),
                            rs.getString("observacoes")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] RelatorioDAO.listarAvaliacoes: " + e.getMessage());
        }
        return lista;
    }
}
