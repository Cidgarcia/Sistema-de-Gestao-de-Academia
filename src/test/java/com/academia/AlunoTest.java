package com.academia;

import com.academia.dao.AlunoDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;
import com.academia.validation.AlunoValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AlunoTest {

    private static final Path BANCO_TESTE;

    static {
        try {
            BANCO_TESTE = Files.createTempFile("academia-teste-", ".sqlite");
            System.setProperty("academia.db.path", BANCO_TESTE.toString());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterAll
    static void limparBanco() throws IOException {
        ConexaoSQLite.fecharConexao();
        Files.deleteIfExists(BANCO_TESTE);
    }

    @Test
    void validaCpfEmailETelefone() {
        assertNull(AlunoValidator.validar("Maria", "529.982.247-25", "maria@email.com", "(71) 99999-9999"));
        assertEquals("Informe um CPF válido.", AlunoValidator.validar("Maria", "111.111.111-11", "maria@email.com", "(71) 99999-9999"));
        assertEquals("Informe um e-mail válido.", AlunoValidator.validar("Maria", "529.982.247-25", "maria@", "(71) 99999-9999"));
        assertEquals("Informe um telefone válido com DDD.", AlunoValidator.validar("Maria", "529.982.247-25", "maria@email.com", "9999-9999"));
    }

    @Test
    void cadastraEditaImpedeCpfDuplicadoEExclui() {
        AlunoDAO dao = new AlunoDAO();
        Aluno aluno = novoAluno("Maria", "529.982.247-25");

        assertTrue(dao.inserir(aluno));
        assertTrue(aluno.getId() > 0);
        assertTrue(dao.cpfJaCadastrado(aluno.getCpf(), 0));
        assertFalse(dao.cpfJaCadastrado(aluno.getCpf(), aluno.getId()));

        Aluno duplicado = novoAluno("Outra pessoa", aluno.getCpf());
        assertFalse(dao.inserir(duplicado));

        aluno.setNome("Maria Atualizada");
        assertTrue(dao.atualizar(aluno));
        assertEquals("Maria Atualizada", dao.buscarPorId(aluno.getId()).getNome());

        assertTrue(dao.excluir(aluno.getId()));
        assertNull(dao.buscarPorId(aluno.getId()));
    }

    private static Aluno novoAluno(String nome, String cpf) {
        Aluno aluno = new Aluno();
        aluno.setNome(nome);
        aluno.setCpf(cpf);
        aluno.setEmail("maria@email.com");
        aluno.setTelefone("(71) 99999-9999");
        aluno.setEndereco("Rua de Teste, 10");
        return aluno;
    }
}
