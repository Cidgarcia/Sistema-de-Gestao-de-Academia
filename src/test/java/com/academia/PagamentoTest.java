package com.academia;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.dao.PagamentoDAO;
import com.academia.dao.PlanoDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.Pagamento;
import com.academia.model.PendenciaFinanceira;
import com.academia.model.Plano;
import com.academia.validation.PagamentoValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagamentoTest {

    private static final Path BANCO_TESTE;

    static {
        try {
            BANCO_TESTE = Files.createTempFile("academia-pagamento-", ".sqlite");
            System.setProperty("academia.db.path", BANCO_TESTE.toString());
            try (var conexao = DriverManager.getConnection("jdbc:sqlite:" + BANCO_TESTE);
                 var stmt = conexao.createStatement()) {
                stmt.execute("""
                        CREATE TABLE Pagamentos (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            matricula_id INTEGER NOT NULL,
                            valor_pago REAL NOT NULL,
                            data_pagamento TEXT NOT NULL,
                            forma_pagamento TEXT NOT NULL,
                            observacoes TEXT
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
    void rejeitaValorZeroENegativo() {
        assertEquals("O valor deve ser maior que zero.", PagamentoValidator.validar(0, "OUTRO", null));
        assertEquals("O valor deve ser maior que zero.", PagamentoValidator.validar(-1, "OUTRO", null));
        assertNull(PagamentoValidator.validar(10, "OUTRO", null));
    }

    @Test
    void pagaMensalidadeNormalVencidaEImpedeInexistenteOuDuplicada() throws SQLException {
        Aluno aluno = novoAluno();
        assertTrue(new AlunoDAO().inserir(aluno));

        Matricula normal = matricular(aluno.getId(), novoPlano("Normal"), LocalDate.now());
        Matricula vencida = matricular(aluno.getId(), novoPlano("Vencido"), LocalDate.now().minusDays(10));
        Matricula legado = matricular(aluno.getId(), novoPlano("Legado"), LocalDate.now().minusDays(20));

        PagamentoDAO dao = new PagamentoDAO();
        inserirPagamentoLegado(legado.getId());
        assertEquals("PAGA", unica(dao.listarPendencias(legado.getId())).getSituacao());
        PendenciaFinanceira pendenciaNormal = unica(dao.listarPendencias(normal.getId()));
        PendenciaFinanceira pendenciaVencida = unica(dao.listarPendencias(vencida.getId()));
        assertEquals("PENDENTE", pendenciaNormal.getSituacao());
        assertEquals("VENCIDA", pendenciaVencida.getSituacao());

        assertEquals(PagamentoDAO.ResultadoPagamento.SUCESSO,
                dao.registrarPagamento(novoPagamento(normal, pendenciaNormal.getValor(), "MENSALIDADE"), pendenciaNormal.getId()));
        assertEquals(PagamentoDAO.ResultadoPagamento.SUCESSO,
                dao.registrarPagamento(novoPagamento(vencida, pendenciaVencida.getValor(), "MENSALIDADE"), pendenciaVencida.getId()));
        assertEquals("PAGA", unica(dao.listarPendencias(vencida.getId())).getSituacao());

        assertEquals(PagamentoDAO.ResultadoPagamento.PENDENCIA_JA_PAGA,
                dao.registrarPagamento(novoPagamento(vencida, pendenciaVencida.getValor(), "MENSALIDADE"), pendenciaVencida.getId()));
        assertEquals(PagamentoDAO.ResultadoPagamento.PENDENCIA_INEXISTENTE,
                dao.registrarPagamento(novoPagamento(normal, 10, "MENSALIDADE"), 999999));

        assertEquals(PagamentoDAO.ResultadoPagamento.SUCESSO,
                dao.registrarPagamento(novoPagamento(normal, 10, "OUTRO"), null));
        assertTrue(dao.totalRecebidoNoDia(LocalDate.now().toString()) > 10);
    }

    private static void inserirPagamentoLegado(int matriculaId) throws SQLException {
        try (var ps = ConexaoSQLite.getConexao().prepareStatement("""
                INSERT INTO Pagamentos
                    (matricula_id, valor_pago, data_pagamento, tipo_pagamento, forma_pagamento, observacoes)
                VALUES (?, 120, ?, 'LEGADO', 'PIX', 'Pagamento anterior à atualização')
                """)) {
            ps.setInt(1, matriculaId);
            ps.setString(2, LocalDate.now().toString());
            ps.executeUpdate();
        }
    }

    private static PendenciaFinanceira unica(List<PendenciaFinanceira> pendencias) {
        assertEquals(1, pendencias.size());
        return pendencias.get(0);
    }

    private static Matricula matricular(int alunoId, Plano plano, LocalDate inicio) {
        assertTrue(new PlanoDAO().inserir(plano));
        Matricula matricula = new Matricula(0, alunoId, plano.getId(), inicio.toString(),
                inicio.plusDays(30).toString(), true);
        assertTrue(new MatriculaDAO().inserir(matricula));
        return matricula;
    }

    private static Pagamento novoPagamento(Matricula matricula, double valor, String tipo) {
        Pagamento pagamento = new Pagamento();
        pagamento.setMatriculaId(matricula.getId());
        pagamento.setValorPago(valor);
        pagamento.setDataPagamento(LocalDate.now().toString());
        pagamento.setTipo(tipo);
        pagamento.setFormaPagamento("PIX");
        pagamento.setObservacoes("Teste");
        return pagamento;
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
        aluno.setNome("Aluno Pagamento");
        aluno.setCpf("111.444.777-35");
        aluno.setEmail("pagamento@email.com");
        aluno.setTelefone("(71) 99999-9999");
        return aluno;
    }
}
