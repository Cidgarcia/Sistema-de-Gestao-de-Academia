package com.academia;

import com.academia.controller.PainelPrincipalController;
import com.academia.dao.AlunoDAO;
import com.academia.dao.FrequenciaDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.dao.PagamentoDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;
import com.academia.model.AlunoMatriculaDTO;
import com.academia.model.FrequenciaDTO;
import com.academia.model.Matricula;
import com.academia.model.Usuario;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de medição de desempenho com volume de dados ampliado:
 * - 100 alunos
 * - 100 matrículas
 * - 1.000 registros de frequência
 */
public class DesempenhoTest {

    private static Path bancoTeste;

    @BeforeAll
    static void configurarBanco() throws IOException, SQLException {
        bancoTeste = Files.createTempFile("academia-desempenho-", ".sqlite");
        System.setProperty("academia.db.path", bancoTeste.toString());

        povoarMassaDeDados(100, 1000);
    }

    @AfterAll
    static void limpar() throws IOException {
        ConexaoSQLite.fecharConexao();
        Files.deleteIfExists(bancoTeste);
        System.clearProperty("academia.db.path");
    }

    private static void povoarMassaDeDados(int totalAlunos, int totalFrequencias) throws SQLException {
        Connection conn = ConexaoSQLite.getConexao();
        conn.setAutoCommit(false);

        LocalDate hoje = LocalDate.now();

        // 1. Inserir 100 alunos
        String sqlAluno = """
                INSERT INTO Alunos (id, nome, cpf, email, telefone, data_nascimento, data_cadastro)
                VALUES (?, ?, ?, ?, ?, '1995-05-10', '2026-01-01')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlAluno)) {
            for (int i = 1; i <= totalAlunos; i++) {
                String cpfNum = String.format("%011d", i);
                String cpfFormatado = String.format("%s.%s.%s-%s",
                        cpfNum.substring(0, 3), cpfNum.substring(3, 6),
                        cpfNum.substring(6, 9), cpfNum.substring(9, 11));
                ps.setInt(1, i);
                ps.setString(2, "Aluno Teste Desempenho " + i);
                ps.setString(3, cpfFormatado);
                ps.setString(4, "aluno" + i + "@email.com");
                ps.setString(5, "(11) 98765-4321");
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // 2. Inserir 100 matrículas ativas (associando ao Plano ID 2 'Básico' que existe por padrão)
        String sqlMatricula = """
                INSERT INTO Matriculas (id, aluno_id, plano_id, data_inicio, data_fim, ativa)
                VALUES (?, ?, 2, ?, ?, 1)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlMatricula)) {
            for (int i = 1; i <= totalAlunos; i++) {
                ps.setInt(1, i);
                ps.setInt(2, i);
                ps.setString(3, hoje.minusDays(10).toString());
                ps.setString(4, hoje.plusDays(30).toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // 3. Inserir 1.000 frequências distribuídas entre os alunos
        String sqlFreq = """
                INSERT INTO Frequencias (aluno_id, matricula_id, data_hora_entrada)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlFreq)) {
            for (int i = 1; i <= totalFrequencias; i++) {
                int alunoId = (i % totalAlunos) + 1;
                LocalDate dataEntrada = hoje.minusDays(i % 60);
                String horaEntrada = String.format("%02d:%02d:00", (i % 14) + 6, (i * 7) % 60);
                ps.setInt(1, alunoId);
                ps.setInt(2, alunoId);
                ps.setString(3, dataEntrada + " " + horaEntrada);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        conn.commit();
        conn.setAutoCommit(true);
    }

    @Test
    void medirDesempenhoConsultas() {
        AlunoDAO alunoDAO = new AlunoDAO();
        FrequenciaDAO frequenciaDAO = new FrequenciaDAO();
        MatriculaDAO matriculaDAO = new MatriculaDAO();
        PagamentoDAO pagamentoDAO = new PagamentoDAO();

        System.out.println("=== INÍCIO DAS MEDIÇÕES DE DESEMPENHO (100 ALUNOS / 1000 FREQUÊNCIAS) ===");

        // Teste 1: Buscar aluno por nome
        long t0 = System.nanoTime();
        List<Aluno> buscaNome = alunoDAO.buscarPorNome("Aluno Teste Desempenho 50");
        long tBuscaNomeMs = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(1, buscaNome.size());
        System.out.printf("[MÉTRICA] Busca de Aluno por Nome: %d ms\n", tBuscaNomeMs);

        // Teste 2: Listar todos os alunos com matrículas (Left Join complexo - alimenta tela de Alunos)
        t0 = System.nanoTime();
        List<AlunoMatriculaDTO> listaAlunos = alunoDAO.listarAlunosComMatriculas(null);
        long tListarAlunosMs = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(100, listaAlunos.size());
        System.out.printf("[MÉTRICA] Listagem Completa de Alunos + Matrículas: %d ms\n", tListarAlunosMs);

        // Teste 3: Registrar entrada na recepção por CPF (validação de matrícula vigente + inserção)
        t0 = System.nanoTime();
        FrequenciaDAO.ResultadoRegistro resEntrada = frequenciaDAO.registrarEntrada("00000000050");
        long tRegistroEntradaMs = (System.nanoTime() - t0) / 1_000_000;
        assertNotNull(resEntrada);
        System.out.printf("[MÉTRICA] Registro de Entrada por CPF na Recepção: %d ms (Status: %s)\n", tRegistroEntradaMs, resEntrada.status());

        // Teste 4: Consulta de Histórico de Frequências (1000 registros com JOIN em Alunos, Matriculas e Planos)
        t0 = System.nanoTime();
        List<FrequenciaDTO> historico1000 = frequenciaDAO.listarHistorico(null, null, null, null);
        long tHistoricoTotalMs = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(historico1000.size() >= 1000);
        System.out.printf("[MÉTRICA] Histórico Completo de Frequências (%d linhas): %d ms\n", historico1000.size(), tHistoricoTotalMs);

        List<FrequenciaDTO> historico250 = frequenciaDAO.listarHistorico(null, null, null, 250);
        assertEquals(250, historico250.size(), "A consulta sem filtros deve respeitar o limite da interface");

        // Teste 5: Consulta Recente Limitada de Frequências (Modo otimizado de abertura de tela)
        t0 = System.nanoTime();
        List<FrequenciaDTO> historico10 = frequenciaDAO.listarHistoricoRecente(10);
        long tHistoricoRecenteMs = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(10, historico10.size());
        System.out.printf("[MÉTRICA] Histórico Recente de Frequências (Limit 10): %d ms\n", tHistoricoRecenteMs);

        // Teste 6: Sincronização e Listagem de Pendências Financeiras (UC04)
        t0 = System.nanoTime();
        pagamentoDAO.listarPendencias(null);
        long tPendenciasMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("[MÉTRICA] Sincronização e Listagem de Pendências Financeiras: %d ms\n", tPendenciasMs);

        System.out.println("=== FIM DAS MEDIÇÕES ===");

        // Garantir que todas as consultas críticas rodem abaixo de limites aceitáveis para UI desktop (< 300ms)
        assertTrue(tBuscaNomeMs < 300, "Busca por nome muito lenta: " + tBuscaNomeMs + "ms");
        assertTrue(tListarAlunosMs < 500, "Listagem de alunos muito lenta: " + tListarAlunosMs + "ms");
        assertTrue(tRegistroEntradaMs < 300, "Registro de entrada na recepção muito lento: " + tRegistroEntradaMs + "ms");
        assertTrue(tHistoricoRecenteMs < 100, "Abertura de histórico recente lenta: " + tHistoricoRecenteMs + "ms");
    }

    @Test
    void medirCadastroAtualizacaoEAberturaDasTelas() throws Exception {
        AlunoDAO dao = new AlunoDAO();
        Aluno novo = new Aluno(0, "Aluno Novo Desempenho", "999.999.999-99",
                "novo@email.com", "(11) 98765-4321", "Rua Teste", "1995-05-10", "", null);

        long cadastroMs;
        long atualizacaoMs;
        try {
            long inicio = System.nanoTime();
            assertTrue(dao.inserir(novo));
            cadastroMs = (System.nanoTime() - inicio) / 1_000_000;
            assertTrue(novo.getId() > 0);

            novo.setNome("Aluno Atualizado Desempenho");
            inicio = System.nanoTime();
            assertTrue(dao.atualizar(novo));
            atualizacaoMs = (System.nanoTime() - inicio) / 1_000_000;
            assertEquals(1, dao.buscarPorNome(novo.getNome()).size());
        } finally {
            if (novo.getId() > 0) assertTrue(dao.excluir(novo.getId()));
        }
        System.out.printf("[MÉTRICA] Cadastro de aluno: %d ms; atualização: %d ms%n",
                cadastroMs, atualizacaoMs);

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // O JavaFX já foi iniciado por outro teste.
        }
        CompletableFuture<long[]> tempos = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                long[] duracoes = new long[3];
                long t0 = System.nanoTime();
                assertNotNull(new FXMLLoader(App.class.getResource("/com/academia/view/login.fxml")).load());
                duracoes[0] = (System.nanoTime() - t0) / 1_000_000;

                int posicao = 1;
                for (String perfil : List.of("FUNCIONARIO", "INSTRUTOR")) {
                    FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/academia/view/painel-principal.fxml"));
                    t0 = System.nanoTime();
                    assertNotNull(loader.load());
                    PainelPrincipalController controller = loader.getController();
                    controller.inicializar(new Usuario(1, "Teste", "teste", "", perfil));
                    duracoes[posicao++] = (System.nanoTime() - t0) / 1_000_000;
                }
                tempos.complete(duracoes);
            } catch (Throwable erro) {
                tempos.completeExceptionally(erro);
            }
        });
        long[] abertura = tempos.get(20, TimeUnit.SECONDS);
        System.out.printf("[MÉTRICA] Carregamento FXML: login %d ms; painel Funcionário %d ms; painel Instrutor %d ms%n",
                abertura[0], abertura[1], abertura[2]);
        assertTrue(cadastroMs < 500 && atualizacaoMs < 500, "Cadastro ou atualização excedeu 500 ms");
        for (long duracao : abertura) assertTrue(duracao < 2_000, "Carregamento FXML excedeu 2 s: " + duracao);
    }

    @Test
    void compararBuscaCpfComESemIndice() throws SQLException {
        String filtro = " WHERE replace(replace(cpf, '.', ''), '-', '') = ?";
        String semIndice = "SELECT id FROM Alunos NOT INDEXED" + filtro;
        String comIndice = "SELECT id FROM Alunos" + filtro;
        try (Connection conn = ConexaoSQLite.getConexao()) {
            String planoSemIndice = planoConsulta(conn, semIndice);
            String planoComIndice = planoConsulta(conn, comIndice);
            assertTrue(planoSemIndice.contains("SCAN Alunos"), planoSemIndice);
            assertTrue(planoComIndice.contains("idx_alunos_cpf_limpo"), planoComIndice);
            System.out.printf("[PLANO] Antes (sem índice): %s%n", planoSemIndice);
            System.out.printf("[PLANO] Depois (com índice): %s%n", planoComIndice);
            System.out.printf("[COMPARAÇÃO] 1000 buscas por CPF: sem índice %d ms; com índice %d ms%n",
                    medirBuscaCpf(conn, semIndice), medirBuscaCpf(conn, comIndice));
        }
    }

    private static String planoConsulta(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("EXPLAIN QUERY PLAN " + sql)) {
            ps.setString(1, "00000000050");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString("detail");
            }
        }
    }

    private static long medirBuscaCpf(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "00000000050");
            for (int i = 0; i < 20; i++) {
                try (ResultSet rs = ps.executeQuery()) { assertTrue(rs.next()); }
            }
            long inicio = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                try (ResultSet rs = ps.executeQuery()) { assertTrue(rs.next()); }
            }
            return (System.nanoTime() - inicio) / 1_000_000;
        }
    }
}
