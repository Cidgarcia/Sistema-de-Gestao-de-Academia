package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.AvaliacaoFisica;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela AvaliacoesFisicas (UC 05).
 *
 * <p>Armazena e recupera as avaliações físicas dos alunos, acessíveis
 * somente por usuários com perfil INSTRUTOR.</p>
 */
public class AvaliacaoFisicaDAO {

    /**
     * Insere uma nova avaliação física no banco de dados.
     *
     * @param avaliacao Objeto {@link AvaliacaoFisica} com os dados a serem salvos.
     * @return {@code true} se inserido com sucesso.
     */
    public boolean inserir(AvaliacaoFisica avaliacao) {
        // Garante que o IMC seja calculado antes de salvar
        avaliacao.calcularImc();

        String sql = """
                INSERT INTO AvaliacoesFisicas
                    (aluno_id, instrutor_id, data_avaliacao, peso_kg, altura_cm,
                     imc, gordura_perc, massa_muscular, observacoes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, avaliacao.getAlunoId());
            ps.setInt(2, avaliacao.getInstrutorId());
            ps.setString(3, avaliacao.getDataAvaliacao());
            ps.setObject(4, avaliacao.getPesoKg());
            ps.setObject(5, avaliacao.getAlturaCm());
            ps.setObject(6, avaliacao.getImc());
            ps.setObject(7, avaliacao.getGorduraPerc());
            ps.setObject(8, avaliacao.getMassaMuscular());
            ps.setString(9, avaliacao.getObservacoes());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) avaliacao.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AvaliacaoFisicaDAO.inserir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lista todas as avaliações físicas de um aluno específico.
     *
     * @param alunoId ID do aluno.
     * @return Lista de avaliações ordenadas da mais recente para a mais antiga.
     */
    public List<AvaliacaoFisica> listarPorAluno(int alunoId) {
        String sql = """
                SELECT af.*, a.nome AS nome_aluno, u.nome AS nome_instrutor
                FROM AvaliacoesFisicas af
                JOIN Alunos a ON af.aluno_id = a.id
                JOIN Usuarios u ON af.instrutor_id = u.id
                WHERE af.aluno_id = ?
                ORDER BY af.data_avaliacao DESC
                """;
        List<AvaliacaoFisica> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AvaliacaoFisicaDAO.listarPorAluno: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Lista todas as avaliações físicas (todas os alunos).
     *
     * @return Lista completa de avaliações.
     */
    public List<AvaliacaoFisica> listarTodas() {
        String sql = """
                SELECT af.*, a.nome AS nome_aluno, u.nome AS nome_instrutor
                FROM AvaliacoesFisicas af
                JOIN Alunos a ON af.aluno_id = a.id
                JOIN Usuarios u ON af.instrutor_id = u.id
                ORDER BY af.data_avaliacao DESC
                """;
        List<AvaliacaoFisica> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) lista.add(mapearResultSet(rs));

        } catch (SQLException e) {
            System.err.println("[ERRO] AvaliacaoFisicaDAO.listarTodas: " + e.getMessage());
        }
        return lista;
    }

    /** Mapeia uma linha do ResultSet para um objeto {@link AvaliacaoFisica}. */
    private AvaliacaoFisica mapearResultSet(ResultSet rs) throws SQLException {
        AvaliacaoFisica av = new AvaliacaoFisica();
        av.setId(rs.getInt("id"));
        av.setAlunoId(rs.getInt("aluno_id"));
        av.setInstrutorId(rs.getInt("instrutor_id"));
        av.setDataAvaliacao(rs.getString("data_avaliacao"));
        av.setPesoKg((Double) rs.getObject("peso_kg"));
        av.setAlturaCm((Double) rs.getObject("altura_cm"));
        av.setImc((Double) rs.getObject("imc"));
        av.setGorduraPerc((Double) rs.getObject("gordura_perc"));
        av.setMassaMuscular((Double) rs.getObject("massa_muscular"));
        av.setObservacoes(rs.getString("observacoes"));
        av.setNomeAluno(rs.getString("nome_aluno"));
        av.setNomeInstrutor(rs.getString("nome_instrutor"));
        return av;
    }
}

