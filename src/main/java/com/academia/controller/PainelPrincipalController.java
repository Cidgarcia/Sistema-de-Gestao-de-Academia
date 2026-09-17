package com.academia.controller;

import com.academia.App;
import com.academia.dao.PagamentoDAO;
import com.academia.model.Usuario;
import com.academia.observer.Observer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
    @FXML private TabPane  tabPanePrincipal;

    // Abas do TabPane
    @FXML private Tab tabAlunos;
    @FXML private Tab tabPlanos;
    @FXML private Tab tabMatriculas;
    @FXML private Tab tabPagamentos;
    @FXML private Tab tabAvaliacao;

    /** Usuário autenticado, recebido via {@link #inicializar(Usuario)}. */
    private Usuario usuarioLogado;

    /** DAO para calcular o total recebido no dia. */
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();

    // ── Referências aos controllers filhos (para chamar refresh) ──────────
    private AlunoController                alunoCtrl;
    private MatriculaController            matriculaCtrl;
    private PagamentoController            pagamentoCtrl;
    private AvaliacaoFisicaController      avaliacaoCtrl;

    /**
     * Inicializa o painel com o usuário autenticado.
     * Deve ser chamado pelo {@link LoginController} após a autenticação.
     *
     * @param usuario Usuário que acabou de fazer login.
     */
    public void inicializar(Usuario usuario) {
        this.usuarioLogado = usuario;
        labelUsuario.setText("Usuário: " + usuario.getNome() + " | " + usuario.getPerfil());

        // Restringe a aba de Avaliação Física apenas a Instrutores
        if (!usuario.isInstrutor()) {
            tabAvaliacao.setDisable(true);
        }

        // Carrega cada aba com seu respectivo FXML e guarda a referência do controller
        alunoCtrl              = carregarAba(tabAlunos,              "/com/academia/view/aluno.fxml",                AlunoController.class);
        /* PlanoController não precisa de refresh */
                                 carregarAba(tabPlanos,              "/com/academia/view/plano.fxml",                null);
        matriculaCtrl          = carregarAba(tabMatriculas,          "/com/academia/view/matricula.fxml",            MatriculaController.class);
        pagamentoCtrl          = carregarAba(tabPagamentos,          "/com/academia/view/pagamento.fxml",            PagamentoController.class);
        avaliacaoCtrl          = carregarAba(tabAvaliacao,           "/com/academia/view/avaliacao-fisica.fxml",     AvaliacaoFisicaController.class);

        // Conecta o PainelPrincipal como Observer do PagamentoController (Subject)
        if (pagamentoCtrl != null) {
            pagamentoCtrl.setPainelObserver(this);
        }

        // Repassa o usuário logado ao AvaliacaoFisicaController
        if (avaliacaoCtrl != null) {
            avaliacaoCtrl.setUsuarioLogado(usuario);
        }

        // ── Listener de troca de aba: recarrega dados ao selecionar ──────
        tabPanePrincipal.getSelectionModel().selectedItemProperty().addListener(
                (obs, tabAnterior, tabSelecionada) -> {
                    if (tabSelecionada == tabMatriculas && matriculaCtrl != null) {
                        matriculaCtrl.refresh();
                    } else if (tabSelecionada == tabPagamentos && pagamentoCtrl != null) {
                        pagamentoCtrl.refresh();
                    } else if (tabSelecionada == tabAvaliacao && avaliacaoCtrl != null) {
                        avaliacaoCtrl.refresh();
                    }
                }
        );

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
        stage.setTitle("Sistema de Gestão de Academia");
    }
}
