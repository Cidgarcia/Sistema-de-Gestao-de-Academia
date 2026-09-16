package com.academia.controller;

import com.academia.App;
import com.academia.dao.UsuarioDAO;
import com.academia.model.Usuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller: Tela de Login — Autenticação do Usuário.
 *
 * <p>Valida as credenciais informadas (login, senha, perfil) contra o banco
 * de dados e, em caso de sucesso, redireciona para o Painel Principal,
 * passando o usuário autenticado.</p>
 */
public class LoginController {

    // ── Componentes da interface (injetados pelo FXML) ────────────────────

    @FXML private TextField     campLogin;
    @FXML private PasswordField campSenha;
    @FXML private ComboBox<String> comboPerfil;
    @FXML private Label         labelErro;
    @FXML private Button        btnEntrar;

    /** DAO responsável por autenticar o usuário. */
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * Inicializa o controller após o FXML ter sido carregado.
     * Popula o ComboBox com os perfis disponíveis.
     */
    @FXML
    public void initialize() {
        comboPerfil.getItems().addAll("FUNCIONARIO", "INSTRUTOR");
        comboPerfil.getSelectionModel().selectFirst();
        labelErro.setVisible(false);

        // Permite submeter o formulário pressionando Enter no campo de senha
        campSenha.setOnAction(this::onEntrarClicado);
    }

    /**
     * Ação do botão "Entrar".
     * Valida os campos, autentica o usuário e navega para o Painel Principal.
     *
     * @param event Evento de clique no botão.
     */
    @FXML
    private void onEntrarClicado(ActionEvent event) {
        labelErro.setVisible(false);

        // Validação básica dos campos
        String login  = campLogin.getText().trim();
        String senha  = campSenha.getText();
        String perfil = comboPerfil.getValue();

        if (login.isEmpty() || senha.isEmpty()) {
            exibirErro("Preencha login e senha.");
            return;
        }

        // Autentica no banco de dados
        Usuario usuario = usuarioDAO.autenticar(login, senha, perfil);

        if (usuario == null) {
            exibirErro("Credenciais inválidas. Verifique login, senha e perfil.");
            campSenha.clear();
            return;
        }

        // ── Autenticado com sucesso → abre o Painel Principal ────────────
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(
                            App.class.getResource("/com/academia/view/painel-principal.fxml")
                    )
            );
            Parent raiz = loader.load();

            // Passa o usuário autenticado para o controller do painel
            PainelPrincipalController painelCtrl = loader.getController();
            painelCtrl.inicializar(usuario);

            Stage stage = (Stage) btnEntrar.getScene().getWindow();
            stage.setScene(new Scene(raiz, 1100, 700));
            stage.setTitle("Academia — Bem-vindo, " + usuario.getNome());

        } catch (IOException e) {
            exibirErro("Erro ao carregar o painel principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Exibe uma mensagem de erro na tela de login. */
    private void exibirErro(String mensagem) {
        labelErro.setText(mensagem);
        labelErro.setVisible(true);
    }
}

