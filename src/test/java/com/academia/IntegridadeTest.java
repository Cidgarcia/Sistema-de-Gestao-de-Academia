package com.academia;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.dao.TreinoDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;
import com.academia.model.Exercicio;
import com.academia.model.Matricula;
import com.academia.model.TreinoDivisao;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntegridadeTest {

    private static final Path BANCO_TESTE;

    static {
        try {
            BANCO_TESTE = Files.createTempFile("academia-integridade-", ".sqlite");
            System.setProperty("academia.db.path", BANCO_TESTE.toString());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterAll
    static void limparBanco() throws IOException {
        ConexaoSQLite.fecharConexao();
        Files.deleteIfExists(BANCO_TESTE);
        System.clearProperty("academia.db.path");
    }

    @Test
    void bancoRejeitaPerfilValoresEMatriculaDuplicada() throws SQLException {
        Connection conn = ConexaoSQLite.getConexao();
        assertThrows(SQLException.class, () -> conn.createStatement().executeUpdate(
                "INSERT INTO Usuarios(nome, login, senha, perfil) VALUES ('X', 'x', 'x', 'INVALIDO')"));
        assertThrows(SQLException.class, () -> conn.createStatement().executeUpdate(
                "INSERT INTO Planos(nome, valor, duracao_dias) VALUES ('Inválido', 0, 30)"));

        Aluno aluno = criarAluno("Integridade", "529.982.247-25");
        MatriculaDAO dao = new MatriculaDAO();
        assertTrue(dao.inserir(novaMatricula(aluno.getId())));
        assertThrows(SQLException.class, () -> conn.createStatement().executeUpdate("""
                INSERT INTO Matriculas(aluno_id, plano_id, data_inicio, data_fim, ativa)
                VALUES (%d, 2, date('now'), date('now', '+30 days'), 1)
                """.formatted(aluno.getId())));
    }

    @Test
    void falhaAoSalvarTreinoRestauraDadosAnteriores() throws SQLException {
        Aluno aluno = criarAluno("Rollback", "111.444.777-35");
        TreinoDAO dao = new TreinoDAO();
        assertTrue(dao.salvarTreinos(aluno.getId(), List.of(divisao("Treino original", 3))));

        assertFalse(dao.salvarTreinos(aluno.getId(), List.of(divisao("Treino inválido", 0))));
        List<TreinoDivisao> persistidas = dao.listarDivisoesPorAluno(aluno.getId());
        assertEquals(1, persistidas.size());
        assertEquals("Treino original", persistidas.get(0).getNomeDivisao());
        assertEquals(3, persistidas.get(0).getExercicios().get(0).getSeries());
    }

    @Test
    void cancelamentoPreservaHistoricoEExclusaoAplicaCascata() throws SQLException {
        Aluno aluno = criarAluno("Exclusão", "123.456.789-09");
        Matricula matricula = novaMatricula(aluno.getId());
        MatriculaDAO dao = new MatriculaDAO();
        assertTrue(dao.inserir(matricula));
        assertTrue(dao.cancelar(matricula.getId()));

        assertEquals(1, contar(ConexaoSQLite.getConexao(),
                "SELECT count(*) FROM Matriculas WHERE id = " + matricula.getId() + " AND ativa = 0"));
        assertTrue(new AlunoDAO().excluir(aluno.getId()));
        assertEquals(0, contar(ConexaoSQLite.getConexao(),
                "SELECT count(*) FROM Matriculas WHERE id = " + matricula.getId()));
        assertTrue(indiceExiste(ConexaoSQLite.getConexao(), "idx_frequencias_aluno_data"));
    }

    private static Aluno criarAluno(String nome, String cpf) {
        Aluno aluno = new Aluno();
        aluno.setNome(nome);
        aluno.setCpf(cpf);
        assertTrue(new AlunoDAO().inserir(aluno));
        return aluno;
    }

    private static Matricula novaMatricula(int alunoId) {
        return new Matricula(0, alunoId, 2, LocalDate.now().toString(),
                LocalDate.now().plusDays(30).toString(), true);
    }

    private static TreinoDivisao divisao(String nome, int series) {
        TreinoDivisao divisao = new TreinoDivisao(nome);
        Exercicio exercicio = new Exercicio(1);
        exercicio.setGrupoMuscular("Peito");
        exercicio.setNome("Supino");
        exercicio.setSeries(series);
        exercicio.setRepeticoes("10");
        divisao.getExercicios().add(exercicio);
        return divisao;
    }

    private static int contar(Connection conn, String sql) throws SQLException {
        try (var rs = conn.createStatement().executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean indiceExiste(Connection conn, String nome) throws SQLException {
        try (var ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ?")) {
            ps.setString(1, nome);
            return ps.executeQuery().next();
        }
    }
}
