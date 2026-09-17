package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Frequencia;
import com.academia.model.FrequenciaDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela Frequencias (UC 07).
 */
public class FrequenciaDAO {

    /**
     * Registra a entrada de um aluno no banco de dados.
     *
     * @param alunoId ID do aluno.
     * @return true se inserido com sucesso.
     */
    public boolean registrarEntrada(int alunoId) {
        String sql = "INSERT INTO Frequencias (aluno_id) VALUES (?)";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, alunoId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] FrequenciaDAO.registrarEntrada: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retorna o histórico recente de entradas, juntando com dados do aluno e status.
     * Limitado ao número de registros passado.
     *
     * @param limite Quantidade máxima de registros a retornar.
     * @return Lista de {@link FrequenciaDTO}.
     */
    public List<FrequenciaDTO> listarHistoricoRecente(int limite) {
        // Query para obter as frequências mais recentes, junto com o aluno,
        // verificando se ele possui uma matrícula ativa e válida hoje.
        String sql = """
                SELECT f.data_hora_entrada, a.nome AS aluno_nome, a.cpf, a.id AS aluno_id,
                       p.nome AS plano_nome,
                       (CASE WHEN m.id IS NOT NULL AND m.ativa = 1 AND m.data_fim >= date('now') THEN 1 ELSE 0 END) AS liberado
                FROM Frequencias f
                JOIN Alunos a ON f.aluno_id = a.id
                LEFT JOIN Matriculas m ON a.id = m.aluno_id AND m.ativa = 1 AND m.data_fim >= date('now')
                LEFT JOIN Planos p ON m.plano_id = p.id
                GROUP BY f.id
                ORDER BY f.data_hora_entrada DESC
                LIMIT ?
                """;

        List<FrequenciaDTO> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dataHora = rs.getString("data_hora_entrada");
                    String nome = rs.getString("aluno_nome");
                    String cpf = rs.getString("cpf");
                    String id = String.valueOf(rs.getInt("aluno_id"));
                    
                    String matriculaCpf = "CPF: " + (cpf != null && !cpf.isBlank() ? cpf : "N/A") + " | ID: #" + id;
                    
                    boolean liberado = rs.getInt("liberado") == 1;
                    String status = liberado ? "Liberado" : "Bloqueado";
                    
                    String plano = rs.getString("plano_nome");
                    String detalhe = liberado ? ("Plano " + (plano != null ? plano : "Ativo")) : "Mensalidade Atrasada / Vencida";

                    lista.add(new FrequenciaDTO(dataHora, nome, matriculaCpf, status, detalhe));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERRO] FrequenciaDAO.listarHistoricoRecente: " + e.getMessage());
        }
        return lista;
    }
}
