package com.academia;

import com.academia.controller.AlunoController;
import com.academia.dao.AlunoDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Usuario;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AlunoPermissaoTest {

    @Test
    void somenteFuncionarioPodeCadastrarMesmoSeAcaoForDisparadaDiretamente() throws Exception {
        Path banco = Files.createTempFile("academia-permissao-", ".sqlite");
        System.setProperty("academia.db.path", banco.toString());
        try {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException ignored) {
                // JavaFX já iniciado por outra classe de teste.
            }
            CompletableFuture<Void> resultado = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/academia/view/aluno.fxml"));
                    loader.load();
                    AlunoController controller = loader.getController();
                    Button salvar = (Button) loader.getNamespace().get("btnSalvarAluno");
                    Button cancelarMatricula = (Button) loader.getNamespace().get("btnCancelarMatricula");
                    ScrollPane painelCadastro = (ScrollPane) loader.getNamespace().get("painelCadastro");
                    TextField nome = (TextField) loader.getNamespace().get("campNome");
                    TextField cpf = (TextField) loader.getNamespace().get("campCpf");
                    TextField email = (TextField) loader.getNamespace().get("campEmail");
                    TextField telefone = (TextField) loader.getNamespace().get("campTelefone");

                    controller.configurarAcesso(new Usuario(1, "Instrutor", "instrutor", "", "INSTRUTOR"));
                    assertTrue(painelCadastro.isDisable());
                    assertTrue(cancelarMatricula.isDisabled());
                    nome.setText("Aluno de Teste");
                    cpf.setText("529.982.247-25");
                    email.setText("teste@email.com");
                    telefone.setText("(71) 99999-9999");
                    salvar.getOnAction().handle(new ActionEvent(salvar, salvar));
                    assertTrue(new AlunoDAO().listarTodos().isEmpty());

                    controller.configurarAcesso(new Usuario(2, "Funcionário", "admin", "", "FUNCIONARIO"));
                    assertFalse(painelCadastro.isDisable());
                    assertFalse(cancelarMatricula.isDisabled());
                    salvar.getOnAction().handle(new ActionEvent(salvar, salvar));
                    assertEquals(1, new AlunoDAO().listarTodos().size());
                    resultado.complete(null);
                } catch (Throwable erro) {
                    resultado.completeExceptionally(erro);
                }
            });
            resultado.get(20, TimeUnit.SECONDS);
        } finally {
            ConexaoSQLite.fecharConexao();
            System.clearProperty("academia.db.path");
            Files.deleteIfExists(banco);
        }
    }
}
