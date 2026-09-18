package com.academia;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.dao.PlanoDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.Plano;
import com.academia.validation.PlanoValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlanoTest {

    private static final Path BANCO_TESTE;

    static {
        try {
            BANCO_TESTE = Files.createTempFile("academia-plano-", ".sqlite");
            System.setProperty("academia.db.path", BANCO_TESTE.toString());
            try (var conexao = DriverManager.getConnection("jdbc:sqlite:" + BANCO_TESTE);
                 var stmt = conexao.createStatement()) {
                stmt.execute("""
                        CREATE TABLE Planos (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            nome TEXT NOT NULL, descricao TEXT,
                            valor REAL NOT NULL, duracao_dias INTEGER NOT NULL
                        )
                        """);
            }
        } catch (IOException | SQLException e) {
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
    void validaCamposValorEDuracao() {
        assertNull(PlanoValidator.validar("Mensal", "Musculação", "Uso pessoal", 120, 30));
        assertEquals("Informe as condições de utilização.",
                PlanoValidator.validar("Mensal", "Musculação", "", 120, 30));
        assertEquals("O valor deve ser maior que zero.",
                PlanoValidator.validar("Mensal", "Musculação", "Uso pessoal", 0, 30));
        assertEquals("A duração deve ser maior que zero.",
                PlanoValidator.validar("Mensal", "Musculação", "Uso pessoal", 120, 0));
    }

    @Test
    void executaCrudEPreservaPlanoUsadoEmMatricula() {
        PlanoDAO dao = new PlanoDAO();
        Plano plano = novoPlano("Teste");
        assertTrue(dao.inserir(plano));
        assertEquals("Uso pessoal", dao.listarTodos().stream()
                .filter(p -> p.getId() == plano.getId()).findFirst().orElseThrow().getCondicoesUtilizacao());

        plano.setValor(150);
        assertTrue(dao.atualizar(plano));
        assertEquals(150, dao.listarTodos().stream()
                .filter(p -> p.getId() == plano.getId()).findFirst().orElseThrow().getValor());

        Aluno aluno = novoAluno();
        assertTrue(new AlunoDAO().inserir(aluno));
        Matricula matricula = new Matricula(0, aluno.getId(), plano.getId(),
                LocalDate.now().toString(), LocalDate.now().plusDays(30).toString(), true);
        assertTrue(new MatriculaDAO().inserir(matricula));
        assertTrue(dao.estaEmUso(plano.getId()));
        assertFalse(dao.excluir(plano.getId()));

        Plano livre = novoPlano("Livre");
        assertTrue(dao.inserir(livre));
        assertTrue(dao.excluir(livre.getId()));
    }

    private static Plano novoPlano(String nome) {
        Plano plano = new Plano();
        plano.setNome(nome);
        plano.setDescricao("Musculação");
        plano.setCondicoesUtilizacao("Uso pessoal");
        plano.setValor(120);
        plano.setDuracaoDias(30);
        return plano;
    }

    private static Aluno novoAluno() {
        Aluno aluno = new Aluno();
        aluno.setNome("Aluno Plano");
        aluno.setCpf("987.654.321-00");
        aluno.setEmail("plano@email.com");
        aluno.setTelefone("(71) 99999-9999");
        return aluno;
    }
}
