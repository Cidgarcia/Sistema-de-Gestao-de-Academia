package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Exercicio;
import com.academia.model.TreinoDivisao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TreinoDAO {

    /**
     * Retorna todas as divisões de treino (com seus exercícios) de um aluno.
     */
    public List<TreinoDivisao> listarDivisoesPorAluno(int alunoId) {
        List<TreinoDivisao> divisoes = new ArrayList<>();
        String sqlDivisao = "SELECT * FROM FichasTreino WHERE aluno_id = ? ORDER BY id";
        String sqlExercicio = "SELECT * FROM ExerciciosTreino WHERE ficha_id = ? ORDER BY ordem";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement stmtDiv = conn.prepareStatement(sqlDivisao);
             PreparedStatement stmtEx = conn.prepareStatement(sqlExercicio)) {

            stmtDiv.setInt(1, alunoId);
            ResultSet rsDiv = stmtDiv.executeQuery();

            while (rsDiv.next()) {
                TreinoDivisao div = new TreinoDivisao();
                div.setId(rsDiv.getInt("id"));
                div.setAlunoId(rsDiv.getInt("aluno_id"));
                div.setNomeDivisao(rsDiv.getString("nome_divisao"));

                stmtEx.setInt(1, div.getId());
                ResultSet rsEx = stmtEx.executeQuery();

                while (rsEx.next()) {
                    Exercicio ex = new Exercicio();
                    ex.setId(rsEx.getInt("id"));
                    ex.setFichaId(rsEx.getInt("ficha_id"));
                    ex.setOrdem(rsEx.getInt("ordem"));
                    ex.setGrupoMuscular(rsEx.getString("grupo_muscular"));
                    ex.setNome(rsEx.getString("nome"));
                    ex.setSeries(rsEx.getInt("series"));
                    ex.setRepeticoes(rsEx.getString("repeticoes"));
                    ex.setCarga(rsEx.getString("carga"));
                    ex.setDescanso(rsEx.getInt("descanso"));
                    ex.setObservacoes(rsEx.getString("observacoes"));
                    div.getExercicios().add(ex);
                }
                divisoes.add(div);
            }

        } catch (SQLException e) {
            System.err.println("[ERRO] Falha ao carregar treinos: " + e.getMessage());
        }

        return divisoes;
    }

    /**
     * Salva as divisões de treino de um aluno.
     * Como a edição pode remover divisões e exercícios, a estratégia mais limpa
     * é excluir as antigas e inserir as novas dentro de uma transação.
     */
    public boolean salvarTreinos(int alunoId, List<TreinoDivisao> divisoes) {
        String sqlDelete = "DELETE FROM FichasTreino WHERE aluno_id = ?";
        String sqlInsertDivisao = "INSERT INTO FichasTreino (aluno_id, nome_divisao) VALUES (?, ?)";
        String sqlInsertExercicio = "INSERT INTO ExerciciosTreino (ficha_id, ordem, grupo_muscular, nome, series, repeticoes, carga, descanso, observacoes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = ConexaoSQLite.getConexao();
        try {
            conn.setAutoCommit(false);

            // 1. Apaga treinos antigos (CASCADE remove os exercícios)
            try (PreparedStatement stmtDel = conn.prepareStatement(sqlDelete)) {
                stmtDel.setInt(1, alunoId);
                stmtDel.executeUpdate();
            }

            // 2. Insere as novas divisões
            try (PreparedStatement stmtDiv = conn.prepareStatement(sqlInsertDivisao, PreparedStatement.RETURN_GENERATED_KEYS);
                 PreparedStatement stmtEx = conn.prepareStatement(sqlInsertExercicio)) {

                for (TreinoDivisao div : divisoes) {
                    stmtDiv.setInt(1, alunoId);
                    stmtDiv.setString(2, div.getNomeDivisao());
                    stmtDiv.executeUpdate();

                    ResultSet rs = stmtDiv.getGeneratedKeys();
                    if (rs.next()) {
                        int fichaId = rs.getInt(1);

                        // 3. Insere os exercícios dessa divisão
                        for (Exercicio ex : div.getExercicios()) {
                            stmtEx.setInt(1, fichaId);
                            stmtEx.setInt(2, ex.getOrdem());
                            stmtEx.setString(3, ex.getGrupoMuscular() != null ? ex.getGrupoMuscular() : "");
                            stmtEx.setString(4, ex.getNome() != null ? ex.getNome() : "");
                            stmtEx.setInt(5, ex.getSeries());
                            stmtEx.setString(6, ex.getRepeticoes() != null ? ex.getRepeticoes() : "");
                            stmtEx.setString(7, ex.getCarga() != null ? ex.getCarga() : "");
                            stmtEx.setInt(8, ex.getDescanso());
                            stmtEx.setString(9, ex.getObservacoes() != null ? ex.getObservacoes() : "");
                            stmtEx.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("[ERRO] Falha ao salvar treinos: " + e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("[ERRO] Falha no rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                // ignora
            }
        }
    }
}
