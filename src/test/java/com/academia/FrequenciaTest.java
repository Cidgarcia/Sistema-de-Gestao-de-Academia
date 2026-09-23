package com.academia;

import com.academia.dao.FrequenciaDAO;
import com.academia.database.ConexaoSQLite;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import static com.academia.dao.FrequenciaDAO.Status.*;
import static org.junit.jupiter.api.Assertions.*;

class FrequenciaTest {

    @Test
    void registroHistoricoEIntegridadeEmBancoExistente() throws Exception {
        Path banco = Files.createTempFile("academia-frequencia-", ".sqlite");
        System.setProperty("academia.db.path", banco.toString());
        try {
            Connection conn = ConexaoSQLite.getConexao();
            LocalDate hoje = LocalDate.now();
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("INSERT INTO Alunos (id, nome, cpf) VALUES (101, 'Vigente', '529.982.247-25')");
                st.executeUpdate("INSERT INTO Alunos (id, nome, cpf) VALUES (102, 'Vencido', '111.222.333-44')");
                st.executeUpdate("INSERT INTO Alunos (id, nome, cpf) VALUES (103, 'Futuro', '222.333.444-55')");
                st.executeUpdate("INSERT INTO Alunos (id, nome, cpf) VALUES (104, 'Sem plano', '333.444.555-66')");
                st.executeUpdate("INSERT INTO Alunos (id, nome, cpf) VALUES (105, 'Cancelado', '444.555.666-77')");
                st.executeUpdate("INSERT INTO Matriculas (id, aluno_id, plano_id, data_inicio, data_fim, ativa) VALUES "
                        + "(201,101,2,'" + hoje.minusDays(2) + "','" + hoje.plusDays(30) + "',1),"
                        + "(202,102,2,'" + hoje.minusDays(60) + "','" + hoje.minusDays(1) + "',1),"
                        + "(203,103,2,'" + hoje.plusDays(1) + "','" + hoje.plusDays(30) + "',1),"
                        + "(205,105,2,'" + hoje.minusDays(2) + "','" + hoje.plusDays(30) + "',0)");
            }

            FrequenciaDAO dao = new FrequenciaDAO();
            assertEquals(NAO_ENCONTRADO, dao.registrarEntrada("99999999999").status());
            assertEquals(NAO_ENCONTRADO, dao.registrarEntrada("999").status());
            assertEquals(NAO_ENCONTRADO, dao.registrarEntrada("101").status());
            assertEquals(NAO_ENCONTRADO, dao.registrarEntrada("1-01").status());
            assertEquals(SEM_MATRICULA_VALIDA, dao.registrarEntrada("33344455566").status());
            assertEquals(SEM_MATRICULA_VALIDA, dao.registrarEntrada("202").status());
            assertEquals(SEM_MATRICULA_VALIDA, dao.registrarEntrada("203").status());
            assertEquals(SEM_MATRICULA_VALIDA, dao.registrarEntrada("205").status());
            assertEquals(LIBERADO, dao.registrarEntrada("529.982.247-25").status());
            assertEquals(REPETIDO, dao.registrarEntrada("52998224725").status());
            assertEquals(REPETIDO, dao.registrarEntrada("201").status());
            assertEquals(1, dao.listarHistorico(null, null, null, null).size());
            assertEquals("Liberado", dao.listarHistorico("201", hoje, hoje, null).get(0).getStatusAcesso());
            assertEquals(1, dao.listarHistorico("529.982.247-25", hoje, hoje, null).size());
            assertTrue(dao.listarHistorico("201", hoje.plusDays(1), null, null).isEmpty());

            try (Statement st = ConexaoSQLite.getConexao().createStatement()) {
                st.executeUpdate("UPDATE Frequencias SET data_hora_entrada = '" + hoje.minusDays(1) + " 12:00:00' WHERE aluno_id = 101");
            }
            assertEquals(LIBERADO, dao.registrarEntrada("201").status());
            assertEquals(2, dao.listarHistorico("201", null, null, null).size());
            assertEquals(1, dao.listarHistorico("201", hoje, hoje, null).size());

            try (Statement st = ConexaoSQLite.getConexao().createStatement()) {
                st.executeUpdate("UPDATE Matriculas SET ativa = 0 WHERE id = 201");
            }
            assertEquals(SEM_MATRICULA_VALIDA, dao.registrarEntrada("201").status());
            assertEquals("Liberado", dao.listarHistorico("201", null, null, 1).get(0).getStatusAcesso());
            try (Statement st = ConexaoSQLite.getConexao().createStatement()) {
                for (int dia = 2; dia <= 12; dia++) {
                    st.executeUpdate("INSERT INTO Frequencias (aluno_id, matricula_id, data_hora_entrada) VALUES "
                            + "(101, 201, '" + hoje.minusDays(dia) + " 12:00:00')");
                }
            }
            assertEquals(10, dao.listarHistoricoRecente(10).size());
            assertEquals(13, dao.listarHistorico("201", null, null, null).size());

            ConexaoSQLite.fecharConexao();
            Path antigo = Files.createTempFile("academia-frequencia-antigo-", ".sqlite");
            try {
                try (Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + antigo);
                     Statement st = c.createStatement()) {
                    st.execute("CREATE TABLE Frequencias (id INTEGER PRIMARY KEY, aluno_id INTEGER NOT NULL, data_hora_entrada TEXT)");
                    st.execute("INSERT INTO Frequencias VALUES (1, 101, '2026-01-01 09:00:00')");
                }
                System.setProperty("academia.db.path", antigo.toString());
                try (Statement st = ConexaoSQLite.getConexao().createStatement();
                     ResultSet rs = st.executeQuery("PRAGMA table_info(Frequencias)")) {
                    boolean encontrou = false;
                    while (rs.next()) if ("matricula_id".equals(rs.getString("name"))) encontrou = true;
                    assertTrue(encontrou);
                }
                try (Statement st = ConexaoSQLite.getConexao().createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Frequencias WHERE id = 1")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            } finally {
                ConexaoSQLite.fecharConexao();
                Files.deleteIfExists(antigo);
            }
        } finally {
            ConexaoSQLite.fecharConexao();
            System.clearProperty("academia.db.path");
            Files.deleteIfExists(banco);
        }
    }
}
