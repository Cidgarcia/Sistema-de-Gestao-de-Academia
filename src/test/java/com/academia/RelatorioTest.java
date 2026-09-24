package com.academia;

import com.academia.dao.*;
import com.academia.database.ConexaoSQLite;
import com.academia.model.*;
import com.academia.util.RelatorioPdfService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioTest {

    private static Path BANCO_TESTE;
    private static Aluno alunoTeste;
    private static Plano planoTeste;
    private static Matricula matriculaTeste;

    @BeforeAll
    static void setup() throws IOException {
        BANCO_TESTE = Files.createTempFile("academia-relatorio-teste-", ".sqlite");
        System.setProperty("academia.db.path", BANCO_TESTE.toString());

        // Cria dados base para testes de relatórios
        AlunoDAO alunoDAO = new AlunoDAO();
        alunoTeste = new Aluno(0, "Carlos Relatorio", "712.345.678-90", "carlos@email.com",
                "(71) 98888-7777", "Rua das Flores, 100", "1995-05-15", "Aluno teste relatorio",
                LocalDate.now().toString());
        assertTrue(alunoDAO.inserir(alunoTeste));

        PlanoDAO planoDAO = new PlanoDAO();
        planoTeste = new Plano(0, "Plano Relatorio Teste", "Descricao", "Livre", 150.00, 30);
        assertTrue(planoDAO.inserir(planoTeste));

        MatriculaDAO matriculaDAO = new MatriculaDAO();
        LocalDate hoje = LocalDate.now();
        matriculaTeste = new Matricula(0, alunoTeste.getId(), planoTeste.getId(),
                hoje.minusDays(5).toString(), hoje.plusDays(25).toString(), true);
        assertTrue(matriculaDAO.inserir(matriculaTeste));

        PagamentoDAO pagamentoDAO = new PagamentoDAO();
        pagamentoDAO.sincronizarPendenciasMensais();
        List<PendenciaFinanceira> pendencias = pagamentoDAO.listarPendencias(matriculaTeste.getId());
        Integer pendenciaId = pendencias.isEmpty() ? null : pendencias.get(0).getId();
        Pagamento pagamento = new Pagamento(0, matriculaTeste.getId(), 150.00, hoje.toString(), "PIX", "Teste de Relatorio");
        pagamento.setTipo("MENSALIDADE");
        assertEquals(PagamentoDAO.ResultadoPagamento.SUCESSO, pagamentoDAO.registrarPagamento(pagamento, pendenciaId));

        FrequenciaDAO freqDAO = new FrequenciaDAO();
        freqDAO.registrarEntrada(alunoTeste.getCpf());

        AvaliacaoFisicaDAO avDAO = new AvaliacaoFisicaDAO();
        AvaliacaoFisica av = new AvaliacaoFisica();
        av.setAlunoId(alunoTeste.getId());
        av.setInstrutorId(2); // ID do instrutor padrão criado no schema.sql
        av.setDataAvaliacao(hoje.toString());
        av.setPesoKg(80.0);
        av.setAlturaCm(180.0);
        av.setGorduraPerc(16.5);
        av.setMassaMuscular(38.0);
        av.setObservacoes("Avaliação inicial teste");
        assertTrue(avDAO.inserir(av));
    }

    @AfterAll
    static void tearDown() throws IOException {
        ConexaoSQLite.fecharConexao();
        Files.deleteIfExists(BANCO_TESTE);
    }

    @Test
    void testRelatorioAlunosAtivos() {
        RelatorioDAO dao = new RelatorioDAO();
        List<RelatorioAlunoAtivoDTO> lista = dao.listarAlunosAtivos(alunoTeste.getNome(), planoTeste.getId());

        assertFalse(lista.isEmpty(), "Deve encontrar ao menos um aluno ativo");
        RelatorioAlunoAtivoDTO item = lista.get(0);
        assertEquals("Carlos Relatorio", item.getNome());
        assertEquals("Plano Relatorio Teste", item.getPlanoNome());
        assertTrue(item.getDiasRestantes() > 0, "Dias restantes deve ser positivo");
        assertNotNull(item.getDataFimFormatada());
    }

    @Test
    void testRelatorioMatriculas() {
        RelatorioDAO dao = new RelatorioDAO();
        LocalDate hoje = LocalDate.now();
        List<RelatorioMatriculaDTO> lista = dao.listarMatriculas(
                hoje.minusDays(10), hoje.plusDays(10), "ATIVAS", planoTeste.getId(), "Carlos"
        );

        assertFalse(lista.isEmpty(), "Deve encontrar a matrícula ativa");
        RelatorioMatriculaDTO item = lista.get(0);
        assertEquals("Carlos Relatorio", item.getAlunoNome());
        assertEquals("Ativa", item.getSituacao());
        assertEquals(150.0, item.getValor(), 0.001);
    }

    @Test
    void testRelatorioPagamentos() {
        RelatorioDAO dao = new RelatorioDAO();
        LocalDate hoje = LocalDate.now();
        List<RelatorioPagamentoDTO> lista = dao.listarPagamentos(
                hoje.minusDays(2), hoje.plusDays(2), "PIX", "MENSALIDADE", "Carlos"
        );

        assertFalse(lista.isEmpty(), "Deve encontrar o pagamento registrado");
        RelatorioPagamentoDTO item = lista.get(0);
        assertEquals("Carlos Relatorio", item.getAlunoNome());
        assertEquals("PIX", item.getFormaPagamento());
        assertEquals(150.0, item.getValorPago(), 0.001);
    }

    @Test
    void testRelatorioFrequencias() {
        RelatorioDAO dao = new RelatorioDAO();
        LocalDate hoje = LocalDate.now();
        List<RelatorioFrequenciaDTO> lista = dao.listarFrequencias(hoje, hoje, "Carlos");

        assertFalse(lista.isEmpty(), "Deve encontrar a entrada de frequência");
        RelatorioFrequenciaDTO item = lista.get(0);
        assertEquals("Carlos Relatorio", item.getAlunoNome());
        assertNotNull(item.getDataHoraEntradaFormatada());
    }

    @Test
    void testRelatorioAvaliacoes() {
        RelatorioDAO dao = new RelatorioDAO();
        LocalDate hoje = LocalDate.now();
        List<RelatorioAvaliacaoDTO> lista = dao.listarAvaliacoes(hoje.minusDays(1), hoje.plusDays(1), alunoTeste.getId(), "Carlos");

        assertFalse(lista.isEmpty(), "Deve encontrar a avaliação física");
        RelatorioAvaliacaoDTO item = lista.get(0);
        assertEquals("Carlos Relatorio", item.getAlunoNome());
        assertEquals(80.0, item.getPesoKg());
        assertEquals(180.0, item.getAlturaCm());
        assertNotNull(item.getImc());
        assertEquals("Peso normal", item.getClassificacaoImc());
    }

    @Test
    void testExportacaoPdfs() throws Exception {
        RelatorioDAO dao = new RelatorioDAO();
        LocalDate hoje = LocalDate.now();

        List<RelatorioAlunoAtivoDTO> alunos = dao.listarAlunosAtivos(null, null);
        File pdfAlunos = File.createTempFile("teste_alunos_", ".pdf");
        RelatorioPdfService.gerarPdfAlunosAtivos(alunos, "Sem restrições", pdfAlunos);
        assertTrue(pdfAlunos.exists() && pdfAlunos.length() > 0);
        pdfAlunos.delete();

        List<RelatorioMatriculaDTO> mats = dao.listarMatriculas(null, null, "TODAS", null, null);
        File pdfMats = File.createTempFile("teste_mats_", ".pdf");
        RelatorioPdfService.gerarPdfMatriculas(mats, "Sem restrições", pdfMats);
        assertTrue(pdfMats.exists() && pdfMats.length() > 0);
        pdfMats.delete();

        List<RelatorioPagamentoDTO> pags = dao.listarPagamentos(null, null, null, null, null);
        File pdfPags = File.createTempFile("teste_pags_", ".pdf");
        RelatorioPdfService.gerarPdfPagamentos(pags, "Sem restrições", pdfPags);
        assertTrue(pdfPags.exists() && pdfPags.length() > 0);
        pdfPags.delete();

        List<RelatorioFrequenciaDTO> freqs = dao.listarFrequencias(null, null, null);
        File pdfFreqs = File.createTempFile("teste_freqs_", ".pdf");
        RelatorioPdfService.gerarPdfFrequencia(freqs, "Sem restrições", pdfFreqs);
        assertTrue(pdfFreqs.exists() && pdfFreqs.length() > 0);
        pdfFreqs.delete();

        List<RelatorioAvaliacaoDTO> avs = dao.listarAvaliacoes(null, null, null, null);
        File pdfAvs = File.createTempFile("teste_avs_", ".pdf");
        RelatorioPdfService.gerarPdfAvaliacoes(avs, "Sem restrições", pdfAvs);
        assertTrue(pdfAvs.exists() && pdfAvs.length() > 0);
        pdfAvs.delete();
    }

    @Test
    void testCarregamentoFxml() throws Exception {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit já iniciado
        }

        java.util.concurrent.CompletableFuture<Boolean> future = new java.util.concurrent.CompletableFuture<>();
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        App.class.getResource("/com/academia/view/relatorio.fxml")
                );
                javafx.scene.Parent root = loader.load();
                assertNotNull(root);
                assertNotNull(loader.getController());

                assertNotNull(new javafx.fxml.FXMLLoader(
                        App.class.getResource("/com/academia/view/login.fxml")).load());
                for (String perfil : List.of("FUNCIONARIO", "INSTRUTOR")) {
                    javafx.fxml.FXMLLoader painelLoader = new javafx.fxml.FXMLLoader(
                            App.class.getResource("/com/academia/view/painel-principal.fxml"));
                    javafx.scene.Parent painel = painelLoader.load();
                    com.academia.controller.PainelPrincipalController controller = painelLoader.getController();
                    controller.inicializar(new Usuario(1, "Teste", "teste", "", perfil));
                    javafx.scene.control.TabPane abas = (javafx.scene.control.TabPane) painel.lookup("#tabPanePrincipal");
                    assertNotNull(abas.getTabs().stream().filter(aba -> "Relatórios".equals(aba.getText()))
                            .findFirst().orElseThrow().getContent());
                    javafx.scene.control.Button checkIn = (javafx.scene.control.Button) painel.lookup("#btnCheckInRapido");
                    javafx.scene.control.Button novaMatricula = (javafx.scene.control.Button) painel.lookup("#btnNovaMatriculaRapida");
                    assertEquals("INSTRUTOR".equals(perfil), checkIn.isDisabled());
                    assertEquals("INSTRUTOR".equals(perfil), novaMatricula.isDisabled());
                }
                future.complete(true);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        assertTrue(future.get(10, java.util.concurrent.TimeUnit.SECONDS));
    }
}
