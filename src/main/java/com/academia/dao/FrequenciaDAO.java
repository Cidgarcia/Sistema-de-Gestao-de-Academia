package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.FrequenciaDTO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acesso aos registros de entrada dos alunos (UC 07). */
public class FrequenciaDAO {

    public enum Status { LIBERADO, NAO_ENCONTRADO, SEM_MATRICULA_VALIDA, REPETIDO, ERRO }

    public record ResultadoRegistro(Status status, String nome, String identificacao, String detalhe) {}

    /** Aceita CPF com ou sem máscara ou o ID da matrícula (não o ID do aluno). */
    public ResultadoRegistro registrarEntrada(String identificador) {
        String digitos = normalizar(identificador);
        if (digitos == null) return new ResultadoRegistro(Status.NAO_ENCONTRADO, null, null, "Informe um CPF ou ID de matrícula válido.");

        boolean porCpf = digitos.length() == 11;
        String busca = porCpf ? """
                SELECT a.id AS aluno_id, a.nome, a.cpf, m.id AS matricula_id, m.ativa,
                       m.data_inicio, m.data_fim, p.nome AS plano
                FROM Alunos a
                LEFT JOIN Matriculas m ON m.aluno_id = a.id
                LEFT JOIN Planos p ON p.id = m.plano_id
                WHERE replace(replace(a.cpf, '.', ''), '-', '') = ?
                ORDER BY CASE WHEN m.ativa = 1 AND date('now','localtime') BETWEEN m.data_inicio AND m.data_fim
                              THEN 0 ELSE 1 END, m.id DESC LIMIT 1
                """ : """
                SELECT a.id AS aluno_id, a.nome, a.cpf, m.id AS matricula_id, m.ativa,
                       m.data_inicio, m.data_fim, p.nome AS plano
                FROM Matriculas m
                JOIN Alunos a ON a.id = m.aluno_id
                JOIN Planos p ON p.id = m.plano_id
                WHERE m.id = ?
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(busca)) {
            if (porCpf) ps.setString(1, digitos);
            else ps.setLong(1, Long.parseLong(digitos));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new ResultadoRegistro(Status.NAO_ENCONTRADO, null, null, "Cadastro ou matrícula não encontrada. Confira o número.");

                int alunoId = rs.getInt("aluno_id");
                int matriculaId = rs.getInt("matricula_id");
                String nome = rs.getString("nome");
                String identificacao = "CPF: " + rs.getString("cpf") + " | Matrícula: #" + (matriculaId == 0 ? "-" : matriculaId);
                String motivo = null;
                LocalDate hoje = LocalDate.now();
                if (matriculaId == 0) motivo = "Aluno sem matrícula.";
                else if (rs.getInt("ativa") != 1) motivo = "Matrícula cancelada.";
                else if (LocalDate.parse(rs.getString("data_inicio")).isAfter(hoje)) motivo = "Matrícula ainda não iniciou.";
                else if (LocalDate.parse(rs.getString("data_fim")).isBefore(hoje)) motivo = "Matrícula vencida.";
                if (motivo != null) return new ResultadoRegistro(Status.SEM_MATRICULA_VALIDA, nome, identificacao, motivo);

                // A validação final e a gravação ocorrem numa única instrução SQL.
                try (PreparedStatement insert = conn.prepareStatement("""
                        INSERT INTO Frequencias (aluno_id, matricula_id)
                        SELECT ?, ? WHERE EXISTS (
                            SELECT 1 FROM Matriculas
                            WHERE id = ? AND aluno_id = ? AND ativa = 1
                              AND date('now','localtime') BETWEEN data_inicio AND data_fim
                        ) AND NOT EXISTS (
                            SELECT 1 FROM Frequencias
                            WHERE aluno_id = ? AND data_hora_entrada >= datetime('now','localtime','-10 seconds')
                        )
                        """)) {
                    insert.setInt(1, alunoId);
                    insert.setInt(2, matriculaId);
                    insert.setInt(3, matriculaId);
                    insert.setInt(4, alunoId);
                    insert.setInt(5, alunoId);
                    if (insert.executeUpdate() == 0)
                        return new ResultadoRegistro(Status.REPETIDO, nome, identificacao, "Entrada recente já registrada. Aguarde 10 segundos.");
                }
                return new ResultadoRegistro(Status.LIBERADO, nome, identificacao, "Plano " + rs.getString("plano"));
            }
        } catch (SQLException | NumberFormatException | java.time.format.DateTimeParseException e) {
            System.err.println("[ERRO] FrequenciaDAO.registrarEntrada: " + e.getMessage());
            return new ResultadoRegistro(Status.ERRO, null, null, "Não foi possível registrar a entrada. Tente novamente.");
        }
    }

    /** Consulta os registros reais; linhas antigas não possuem matrícula vinculada. */
    public List<FrequenciaDTO> listarHistorico(String identificador, LocalDate inicio, LocalDate fim, Integer limite) {
        String digitos = identificador == null || identificador.isBlank() ? null : normalizar(identificador);
        if (identificador != null && !identificador.isBlank() && digitos == null) return List.of();
        boolean porCpf = digitos != null && digitos.length() == 11;
        String sql = """
                SELECT f.id, f.data_hora_entrada, a.nome, a.cpf, m.id AS matricula_id, p.nome AS plano
                FROM Frequencias f
                JOIN Alunos a ON a.id = f.aluno_id
                LEFT JOIN Matriculas m ON m.id = f.matricula_id
                LEFT JOIN Planos p ON p.id = m.plano_id
                WHERE (? IS NULL OR a.id IN (
                    SELECT aluno_id FROM Matriculas WHERE id = ?
                ) OR replace(replace(a.cpf, '.', ''), '-', '') = ?)
                  AND (? IS NULL OR substr(f.data_hora_entrada, 1, 10) >= ?)
                  AND (? IS NULL OR substr(f.data_hora_entrada, 1, 10) <= ?)
                ORDER BY f.data_hora_entrada DESC, f.id DESC
                """ + (limite == null ? "" : " LIMIT ?");
        List<FrequenciaDTO> registros = new ArrayList<>();
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, digitos);
            ps.setLong(2, digitos != null && !porCpf ? Long.parseLong(digitos) : -1);
            ps.setString(3, porCpf ? digitos : "");
            String inicioSql = inicio == null ? null : inicio.toString();
            String fimSql = fim == null ? null : fim.toString();
            ps.setString(4, inicioSql);
            ps.setString(5, inicioSql);
            ps.setString(6, fimSql);
            ps.setString(7, fimSql);
            if (limite != null) ps.setInt(8, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int matriculaId = rs.getInt("matricula_id");
                    String idTexto = matriculaId == 0 ? "Anterior" : "Matrícula #" + matriculaId;
                    String status = matriculaId == 0 ? "Anterior" : "Liberado";
                    String detalhe = matriculaId == 0 ? "Registro anterior sem validação armazenada" : "Plano " + rs.getString("plano");
                    registros.add(new FrequenciaDTO(rs.getString("data_hora_entrada"), rs.getString("nome"),
                            "CPF: " + rs.getString("cpf") + " | " + idTexto, status, detalhe));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("[ERRO] FrequenciaDAO.listarHistorico: " + e.getMessage());
        }
        return registros;
    }

    public List<FrequenciaDTO> listarHistoricoRecente(int limite) {
        return listarHistorico(null, null, null, limite);
    }

    private static String normalizar(String entrada) {
        if (entrada == null) return null;
        String valor = entrada.trim();
        if (!valor.matches("\\d{1,11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) return null;
        return valor.replaceAll("\\D", "");
    }
}
