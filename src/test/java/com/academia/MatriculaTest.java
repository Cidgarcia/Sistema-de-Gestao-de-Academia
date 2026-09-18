package com.academia;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.validation.MatriculaValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MatriculaTest {

    private static final Path BANCO_TESTE;

    static {
        try {
            BANCO_TESTE = Files.createTempFile("academia-matricula-", ".sqlite");
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
    void validaDatasDaMatricula() {
        LocalDate hoje = LocalDate.now();
        assertNull(MatriculaValidator.validarDatas(hoje, hoje.plusDays(30)));
        assertEquals("A data inicial não pode estar no passado.",
                MatriculaValidator.validarDatas(hoje.minusDays(1), hoje.plusDays(29)));
        assertEquals("A data final deve ser posterior à data inicial.",
                MatriculaValidator.validarDatas(hoje, hoje));
    }

    @Test
    void matriculaCancelaEImpedeDuplicadaAtiva() {
        Aluno aluno = novoAluno();
        assertTrue(new AlunoDAO().inserir(aluno));

        MatriculaDAO dao = new MatriculaDAO();
        Matricula primeira = novaMatricula(aluno.getId());
        assertTrue(dao.inserir(primeira));
        assertTrue(dao.existeAtiva(aluno.getId(), 2));

        assertFalse(dao.inserir(novaMatricula(aluno.getId())));
        assertTrue(dao.cancelar(primeira.getId()));
        assertFalse(dao.existeAtiva(aluno.getId(), 2));
        assertTrue(dao.inserir(novaMatricula(aluno.getId())));
    }

    private static Aluno novoAluno() {
        Aluno aluno = new Aluno();
        aluno.setNome("Aluno Matrícula");
        aluno.setCpf("123.456.789-09");
        aluno.setEmail("matricula@email.com");
        aluno.setTelefone("(71) 99999-9999");
        return aluno;
    }

    private static Matricula novaMatricula(int alunoId) {
        Matricula matricula = new Matricula();
        matricula.setAlunoId(alunoId);
        matricula.setPlanoId(2);
        matricula.setDataInicio(LocalDate.now().toString());
        matricula.setDataFim(LocalDate.now().plusDays(30).toString());
        matricula.setAtiva(true);
        return matricula;
    }
}
