package com.academia;

import atlantafx.base.theme.PrimerDark;
import com.academia.database.ConexaoSQLite;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Classe principal da aplicação JavaFX.
 *
 * <p>
 * Inicializa o tema AtlantaFX (PrimerDark), carrega a tela de login
 * e gerencia o ciclo de vida da aplicação.
 * </p>
 *
 * <p>
 * Para executar: {@code mvn javafx:run}
 * </p>
 */
public class App extends Application {

    /** Largura padrão da janela principal. */
    private static final double LARGURA = 1100;

    /** Altura padrão da janela principal. */
    private static final double ALTURA = 700;

    /**
     * Ponto de entrada JavaFX — chamado após {@link #main(String[])}.
     *
     * @param stage Janela principal fornecida pelo framework JavaFX.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // ── 1. Aplica o tema AtlantaFX (visual moderno) ────────────────────
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // ── 2. Carrega a tela de Login ──────────────────────────────────────
        Parent raiz = FXMLLoader.load(
                Objects.requireNonNull(
                        App.class.getResource("/com/academia/view/login.fxml"),
                        "login.fxml não encontrado nos resources!"));

        // ── 3. Configura e exibe a janela ───────────────────────────────────
        Scene cena = new Scene(raiz, LARGURA, ALTURA);
        stage.setTitle("Sistema de Gestão de Academia");
        stage.setScene(cena);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    /**
     * Chamado pelo JavaFX ao fechar a aplicação.
     * Encerra a conexão com o banco de dados de forma segura.
     */
    @Override
    public void stop() {
        ConexaoSQLite.fecharConexao();
        System.out.println("[INFO] Aplicação encerrada.");
    }

    /**
     * Ponto de entrada Java padrão.
     *
     * @param args Argumentos de linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        launch(args);
    }
}
