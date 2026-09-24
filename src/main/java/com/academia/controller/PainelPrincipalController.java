package com.academia.controller;

import com.academia.App;
import com.academia.dao.PagamentoDAO;
import com.academia.dao.UsuarioDAO;
import com.academia.model.Usuario;
import com.academia.observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Controller: Painel Principal da aplicação.
 *
 * <p>Implementa a interface {@link Observer} para receber notificações do
 * {@link PagamentoController} (Subject) e atualizar o indicador de
 * "Total Recebido no Dia" em tempo real — Padrão Observer.</p>
 *
 * <p>Também controla a visibilidade das abas conforme o perfil do usuário e
 * recarrega os dados de cada aba ao ser selecionada (evitando combos vazios).</p>
 */
public class PainelPrincipalController implements Observer {

    // ── Componentes da interface ──────────────────────────────────────────

    @FXML private Label    labelUsuario;
    @FXML private Label    labelTotalDia;
    @FXML private Label    labelBreadcrumb;
    @FXML private TabPane  tabPanePrincipal;

    // Botões da Sidebar GymCore
    @FXML private Button btnNavAlunos;
    @FXML private Button btnNavFrequencia;
    @FXML private Button btnNavPlanos;
    @FXML private Button btnNavMatriculas;
    @FXML private Button btnNavPagamentos;
    @FXML private Button btnNavAvaliacao;
    @FXML private Button btnNavTreinos;
    @FXML private Button btnNavRelatorios;
    @FXML private Button btnCheckInRapido;
    @FXML private Button btnNovaMatriculaRapida;

    // Abas do TabPane
    @FXML private Tab tabAlunos;
    @FXML private Tab tabFrequencia;
    @FXML private Tab tabPlanos;
    @FXML private Tab tabMatriculas;
    @FXML private Tab tabPagamentos;
    @FXML private Tab tabAvaliacao;
    @FXML private Tab tabTreinos;
    @FXML private Tab tabRelatorios;

    /** Usuário autenticado, recebido via {@link #inicializar(Usuario)}. */
    private Usuario usuarioLogado;

    /** DAO para calcular o total recebido no dia. */
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();

    // ── Referências aos controllers filhos (para chamar refresh) ──────────
    private AlunoController                alunoCtrl;
    private FrequenciaController           frequenciaCtrl;
    private MatriculaController            matriculaCtrl;
    private PagamentoController            pagamentoCtrl;
    private AvaliacaoFisicaController      avaliacaoCtrl;
    private TreinoController               treinoCtrl;
    private RelatorioController            relatorioCtrl;

    /**
     * Inicializa o painel com o usuário autenticado.
     * Deve ser chamado pelo {@link LoginController} após a autenticação.
     *
     * @param usuario Usuário que acabou de fazer login.
     */
    public void inicializar(Usuario usuario) {
        this.usuarioLogado = usuario;
        labelUsuario.setText(usuario.getNome() + " (" + usuario.getPerfil() + ")");

        if (usuario.isInstrutor()) {
            // Instrutor acessa apenas Alunos, Avaliação Física, Treinos e Relatórios
            tabFrequencia.setDisable(true);
            tabPlanos.setDisable(true);
            tabMatriculas.setDisable(true);
            tabPagamentos.setDisable(true);

            if (btnNavFrequencia != null) btnNavFrequencia.setDisable(true);
            if (btnNavPlanos != null) btnNavPlanos.setDisable(true);
            if (btnNavMatriculas != null) btnNavMatriculas.setDisable(true);
            if (btnNavPagamentos != null) btnNavPagamentos.setDisable(true);
            btnCheckInRapido.setDisable(true);
            btnNovaMatriculaRapida.setDisable(true);
        } else {
            // Funcionário não acessa Avaliação Física nem Treinos
            tabAvaliacao.setDisable(true);
            tabTreinos.setDisable(true);

            if (btnNavAvaliacao != null) btnNavAvaliacao.setDisable(true);
            if (btnNavTreinos != null) btnNavTreinos.setDisable(true);
        }

        // Carrega cada aba com seu respectivo FXML e guarda a referência do controller
        alunoCtrl              = carregarAba(tabAlunos,              "/com/academia/view/aluno.fxml",                AlunoController.class);
        if (alunoCtrl != null) alunoCtrl.configurarAcesso(usuario);
        frequenciaCtrl         = carregarAba(tabFrequencia,          "/com/academia/view/frequencia.fxml",           FrequenciaController.class);
        /* PlanoController não precisa de refresh */
                                 carregarAba(tabPlanos,              "/com/academia/view/plano.fxml",                null);
        matriculaCtrl          = carregarAba(tabMatriculas,          "/com/academia/view/matricula.fxml",            MatriculaController.class);
        pagamentoCtrl          = carregarAba(tabPagamentos,          "/com/academia/view/pagamento.fxml",            PagamentoController.class);
        avaliacaoCtrl          = carregarAba(tabAvaliacao,           "/com/academia/view/avaliacao-fisica.fxml",     AvaliacaoFisicaController.class);
        treinoCtrl             = carregarAba(tabTreinos,             "/com/academia/view/treino.fxml",               TreinoController.class);
        relatorioCtrl          = carregarAba(tabRelatorios,          "/com/academia/view/relatorio.fxml",            RelatorioController.class);

        // Conecta o PainelPrincipal como Observer do PagamentoController (Subject)
        if (pagamentoCtrl != null) {
            pagamentoCtrl.setPainelObserver(this);
        }

        // Repassa o usuário logado ao AvaliacaoFisicaController
        if (avaliacaoCtrl != null) {
            avaliacaoCtrl.setUsuarioLogado(usuario);
        }
        
        // Repassa o usuário logado ao TreinoController
        if (treinoCtrl != null) {
            treinoCtrl.setUsuarioLogado(usuario);
        }

        // Repassa o usuário logado ao RelatorioController
        if (relatorioCtrl != null) {
            relatorioCtrl.setUsuarioLogado(usuario);
        }

        // ── Listener de troca de aba: recarrega dados ao selecionar ──────
        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener(
                (obs, tabAnterior, tabSelecionada) -> {
                    atualizarEstiloNav(tabSelecionada);

                    if (tabSelecionada == tabAlunos && alunoCtrl != null) {
                        alunoCtrl.refresh();
                    } else if (tabSelecionada == tabFrequencia && frequenciaCtrl != null) {
                        frequenciaCtrl.refresh();
                    } else if (tabSelecionada == tabMatriculas && matriculaCtrl != null) {
                        matriculaCtrl.refresh();
                    } else if (tabSelecionada == tabPagamentos && pagamentoCtrl != null) {
                        pagamentoCtrl.refresh();
                    } else if (tabSelecionada == tabAvaliacao && avaliacaoCtrl != null) {
                        avaliacaoCtrl.refresh();
                    } else if (tabSelecionada == tabTreinos && treinoCtrl != null) {
                        treinoCtrl.refresh();
                    } else if (tabSelecionada == tabRelatorios && relatorioCtrl != null) {
                        relatorioCtrl.refresh();
                    }
                }
        );

        // Atualiza estilo inicial para Alunos
        atualizarEstiloNav(tabAlunos);

        // Atualiza o indicador de total do dia ao abrir o painel
        atualizarTotalDia();
    }

    /**
     * Implementação do método {@link Observer#atualizar} — Padrão Observer.
     *
     * <p>Chamado pelo {@link PagamentoController} (Subject) sempre que um
     * novo pagamento for registrado. Atualiza o label de total do dia.</p>
     *
     * @param evento Tipo do evento (ex.: "PAGAMENTO_REGISTRADO").
     * @param dado   Valor do pagamento registrado como {@link Double}.
     */
    @Override
    public void atualizar(String evento, Object dado) {
        if ("PAGAMENTO_REGISTRADO".equals(evento)) {
            // Garante que a atualização ocorra na Thread JavaFX (thread safety)
            Platform.runLater(this::atualizarTotalDia);
        }
    }

    /**
     * Consulta o banco e atualiza o label com o total recebido hoje.
     */
    private void atualizarTotalDia() {
        String hoje = LocalDate.now().toString();
        double total = pagamentoDAO.totalRecebidoNoDia(hoje);
        labelTotalDia.setText(String.format("Total Recebido Hoje: R$ %.2f", total));
    }

    /**
     * Carrega um arquivo FXML dentro de uma aba e retorna o controller tipado.
     *
     * @param aba          A aba que receberá o conteúdo.
     * @param caminhoFxml  Caminho do arquivo FXML nos resources.
     * @param tipoCtrl     Tipo esperado do controller (para cast seguro); null se não importa.
     * @return O controller carregado, ou {@code null} em caso de erro ou tipo incompatível.
     */
    @SuppressWarnings("unchecked")
    private <T> T carregarAba(Tab aba, String caminhoFxml, Class<T> tipoCtrl) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(App.class.getResource(caminhoFxml))
            );
            Parent conteudo = loader.load();
            aba.setContent(conteudo);

            Object ctrl = loader.getController();
            if (tipoCtrl != null && tipoCtrl.isInstance(ctrl)) {
                return (T) ctrl;
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("[ERRO] Falha ao carregar aba '" +
                    aba.getText() + "': " + e.getMessage());
        }
        return null;
    }

    /** Ação do botão "Sair" — retorna para a tela de Login. */
    @FXML
    private void onSairClicado() throws IOException {
        Parent raiz = FXMLLoader.load(
                Objects.requireNonNull(App.class.getResource("/com/academia/view/login.fxml"))
        );
        Stage stage = (Stage) labelUsuario.getScene().getWindow();
        stage.setScene(new Scene(raiz, 1100, 700));
        stage.setTitle("GymCore");
    }

    @FXML
    private void onAlterarSenhaClicado() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Alterar senha");
        dialogo.initOwner(labelUsuario.getScene().getWindow());

        PasswordField senhaAtual = new PasswordField();
        PasswordField senhaNova = new PasswordField();
        PasswordField confirmacao = new PasswordField();
        Label erro = new Label();
        erro.setStyle("-fx-text-fill: #f85149;");
        erro.setWrapText(true);

        GridPane campos = new GridPane();
        campos.setHgap(10);
        campos.setVgap(10);
        campos.setPadding(new Insets(20));
        campos.addRow(0, new Label("Senha atual"), senhaAtual);
        campos.addRow(1, new Label("Nova senha"), senhaNova);
        campos.addRow(2, new Label("Confirmar nova senha"), confirmacao);
        campos.add(erro, 0, 3, 2, 1);
        dialogo.getDialogPane().setContent(campos);

        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);
        Button botaoSalvar = (Button) dialogo.getDialogPane().lookupButton(salvar);
        botaoSalvar.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            if (senhaAtual.getText().isEmpty()) {
                erro.setText("Informe a senha atual.");
            } else if (senhaNova.getText().length() < 8) {
                erro.setText("A nova senha deve ter pelo menos 8 caracteres.");
            } else if (!senhaNova.getText().equals(confirmacao.getText())) {
                erro.setText("A confirmação da nova senha não confere.");
            } else if (!new UsuarioDAO().alterarSenha(
                    usuarioLogado.getId(), senhaAtual.getText(), senhaNova.getText())) {
                erro.setText("Senha atual incorreta ou não foi possível alterar a senha.");
            } else {
                return;
            }
            evento.consume();
        });

        dialogo.showAndWait().ifPresent(resultado -> {
            if (resultado == salvar) {
                new Alert(Alert.AlertType.INFORMATION, "Senha alterada com sucesso.").showAndWait();
            }
        });
    }

    // ── Navegação via Sidebar GymCore ─────────────────────────────────────
    @FXML private void onNavAlunos()     { tabPanePrincipal.getSelectionModel().select(tabAlunos); }
    @FXML private void onNavFrequencia() { tabPanePrincipal.getSelectionModel().select(tabFrequencia); }
    @FXML private void onNavPlanos()     { tabPanePrincipal.getSelectionModel().select(tabPlanos); }
    @FXML private void onNavMatriculas() { tabPanePrincipal.getSelectionModel().select(tabMatriculas); }
    @FXML private void onNavPagamentos() { tabPanePrincipal.getSelectionModel().select(tabPagamentos); }
    @FXML private void onNavAvaliacao()  { tabPanePrincipal.getSelectionModel().select(tabAvaliacao); }
    @FXML private void onNavTreinos()    { tabPanePrincipal.getSelectionModel().select(tabTreinos); }
    @FXML private void onNavRelatorios() { tabPanePrincipal.getSelectionModel().select(tabRelatorios); }

    // ── Ações Rápidas da TopBar ───────────────────────────────────────────
    @FXML private void onCheckInRapido() {
        if (usuarioLogado != null && !usuarioLogado.isInstrutor())
            tabPanePrincipal.getSelectionModel().select(tabFrequencia);
    }
    @FXML private void onNovaMatriculaRapida() {
        if (usuarioLogado != null && !usuarioLogado.isInstrutor())
            tabPanePrincipal.getSelectionModel().select(tabMatriculas);
    }

    private void atualizarEstiloNav(Tab selecionada) {
        Button[] botoes = {btnNavAlunos, btnNavFrequencia, btnNavPlanos, btnNavMatriculas,
                           btnNavPagamentos, btnNavAvaliacao, btnNavTreinos, btnNavRelatorios};
        for (Button b : botoes) {
            if (b != null) b.getStyleClass().remove("gymcore-nav-btn-active");
        }

        if (selecionada == tabAlunos) {
            destacar(btnNavAlunos, "GymCore › Alunos");
        } else if (selecionada == tabFrequencia) {
            destacar(btnNavFrequencia, "GymCore › Recepção e Acessos");
        } else if (selecionada == tabPlanos) {
            destacar(btnNavPlanos, "GymCore › Planos de Assinatura");
        } else if (selecionada == tabMatriculas) {
            destacar(btnNavMatriculas, "GymCore › Matrículas");
        } else if (selecionada == tabPagamentos) {
            destacar(btnNavPagamentos, "GymCore › Caixa e Pagamentos");
        } else if (selecionada == tabAvaliacao) {
            destacar(btnNavAvaliacao, "GymCore › Avaliações Físicas");
        } else if (selecionada == tabTreinos) {
            destacar(btnNavTreinos, "GymCore › Prescrição de Treinos");
        } else if (selecionada == tabRelatorios) {
            destacar(btnNavRelatorios, "GymCore › Relatórios Gerenciais");
        }
    }

    private void destacar(Button btn, String breadcrumb) {
        if (btn != null && !btn.getStyleClass().contains("gymcore-nav-btn-active")) {
            btn.getStyleClass().add("gymcore-nav-btn-active");
        }
        if (labelBreadcrumb != null) {
            labelBreadcrumb.setText(breadcrumb);
        }
    }
}
