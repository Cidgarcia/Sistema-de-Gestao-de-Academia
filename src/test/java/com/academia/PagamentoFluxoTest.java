package com.academia;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.dao.PagamentoDAO;
import com.academia.dao.PlanoDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.PendenciaFinanceira;
import com.academia.model.Plano;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PagamentoFluxoTest {

    @Test
    void consultaAlunoSemMatriculaEEncerraQuandoNaoHaPendencias() throws Exception {
        Path banco = Files.createTempFile("academia-caixa-", ".sqlite");
        System.setProperty("academia.db.path", banco.toString());
        try {
            Aluno semMatricula = aluno("Aluno Sem Matrícula", "529.982.247-25");
            Aluno matriculado = aluno("Aluno Matriculado", "111.444.777-35");
            Plano plano = new Plano();
            plano.setNome("Plano Caixa");
            plano.setDescricao("Teste");
            plano.setCondicoesUtilizacao("Teste");
            plano.setValor(120);
            plano.setDuracaoDias(30);
            assertTrue(new PlanoDAO().inserir(plano));
            Matricula matricula = new Matricula(0, matriculado.getId(), plano.getId(),
                    LocalDate.now().toString(), LocalDate.now().plusDays(30).toString(), true);
            assertTrue(new MatriculaDAO().inserir(matricula));
            PagamentoDAO dao = new PagamentoDAO();
            assertEquals(1, dao.listarPendenciasPorAluno(matriculado.getId()).size());

            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException ignored) {
                // JavaFX já iniciado por outro teste.
            }
            CompletableFuture<Void> resultado = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/academia/view/pagamento.fxml"));
                    loader.load();
                    TextField busca = (TextField) loader.getNamespace().get("campoBuscaAluno");
                    ListView<Aluno> resultados = (ListView<Aluno>) loader.getNamespace().get("listaResultadosAluno");
                    TableView<PendenciaFinanceira> pendencias =
                            (TableView<PendenciaFinanceira>) loader.getNamespace().get("tabelaAtrasados");
                    Button confirmar = (Button) loader.getNamespace().get("btnConfirmarPagamento");
                    Label status = (Label) loader.getNamespace().get("labelStatus");
                    ComboBox<String> formas = (ComboBox<String>) loader.getNamespace().get("comboFormaPagamento");
                    ComboBox<String> tipos = (ComboBox<String>) loader.getNamespace().get("comboTipoPagamento");
                    TextField valor = (TextField) loader.getNamespace().get("campValor");

                    assertFalse(formas.getItems().contains("BOLETO"));
                    busca.setText("Sem Matrícula");
                    assertEquals(1, resultados.getItems().size());
                    resultados.getSelectionModel().selectFirst();
                    assertTrue(pendencias.getItems().isEmpty());
                    assertTrue(confirmar.isDisable());
                    assertTrue(status.getText().contains("situação regular"));

                    busca.setText("Matriculado");
                    resultados.getSelectionModel().selectFirst();
                    assertEquals(1, pendencias.getItems().size());
                    assertFalse(confirmar.isDisable());

                    tipos.setValue("OUTRO");
                    valor.setText("15");
                    confirmar.fire();
                    assertEquals(1, dao.listarTodos().size());
                    assertEquals("PENDENTE", dao.listarPendenciasPorAluno(matriculado.getId()).get(0).getSituacao());

                    busca.setText("Matriculado");
                    resultados.getSelectionModel().selectFirst();
                    pendencias.getSelectionModel().selectFirst();
                    confirmar.fire();
                    busca.setText("Matriculado");
                    resultados.getSelectionModel().selectFirst();
                    assertEquals("PAGA", pendencias.getItems().get(0).getSituacao());
                    assertTrue(confirmar.isDisable());
                    assertTrue(status.getText().contains("situação regular"));
                    assertEquals(2, dao.listarTodos().size());
                    resultado.complete(null);
                } catch (Throwable erro) {
                    resultado.completeExceptionally(erro);
                }
            });
            resultado.get(20, TimeUnit.SECONDS);
            assertEquals("PAGA", dao.listarPendenciasPorAluno(matriculado.getId()).get(0).getSituacao());
        } finally {
            ConexaoSQLite.fecharConexao();
            System.clearProperty("academia.db.path");
            Files.deleteIfExists(banco);
        }
    }

    private static Aluno aluno(String nome, String cpf) {
        Aluno aluno = new Aluno();
        aluno.setNome(nome);
        aluno.setCpf(cpf);
        aluno.setEmail("teste@email.com");
        aluno.setTelefone("(71) 99999-9999");
        assertTrue(new AlunoDAO().inserir(aluno));
        return aluno;
    }
}
