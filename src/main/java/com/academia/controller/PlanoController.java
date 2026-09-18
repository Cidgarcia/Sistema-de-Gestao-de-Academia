package com.academia.controller;

import com.academia.dao.PlanoDAO;
import com.academia.model.Plano;
import com.academia.validation.PlanoValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller: Cadastro de Planos — UC 02.
 *
 * <p>Permite criar, editar e excluir os planos oferecidos pela academia,
 * com nome, descrição, valor e duração em dias.</p>
 */
public class PlanoController {

    // ── Campos do formulário ──────────────────────────────────────────────

    @FXML private TextField  campNome;
    @FXML private TextArea   campDescricao;
    @FXML private TextArea   campCondicoes;
    @FXML private TextField  campValor;
    @FXML private TextField  campDuracao;
    @FXML private Label      labelTituloForm;

    // ── Tabela de listagem ────────────────────────────────────────────────

    @FXML private TableView<Plano>        tabelaPlanos;
    @FXML private TableColumn<Plano, Integer> colId;
    @FXML private TableColumn<Plano, String>  colNome;
    @FXML private TableColumn<Plano, String>  colDescricao;
    @FXML private TableColumn<Plano, String>  colCondicoes;
    @FXML private TableColumn<Plano, Double>  colValor;
    @FXML private TableColumn<Plano, Integer> colDuracao;

    @FXML private Label labelStatus;

    private final PlanoDAO planoDAO  = new PlanoDAO();
    private       int      idEmEdicao = 0;

    /**
     * Inicializa o controller: configura colunas e carrega dados.
     */
    @FXML
    public void initialize() {
        configurarColunas();
        carregarPlanos();

        tabelaPlanos.getSelectionModel().selectedItemProperty().addListener(
                (obs, ant, sel) -> { if (sel != null) preencherFormulario(sel); }
        );
    }

    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colCondicoes.setCellValueFactory(new PropertyValueFactory<>("condicoesUtilizacao"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colDuracao.setCellValueFactory(new PropertyValueFactory<>("duracaoDias"));

        // Formata a coluna de valor como moeda
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("R$ %.2f", item));
            }
        });
    }

    private void carregarPlanos() {
        ObservableList<Plano> lista = FXCollections.observableArrayList(planoDAO.listarTodos());
        tabelaPlanos.setItems(lista);
    }

    /** Ação do botão "Salvar". */
    @FXML
    private void onSalvarClicado() {
        if (!validarCampos()) return;

        Plano plano = montarObjetoPlano();

        if (idEmEdicao == 0) {
            if (planoDAO.inserir(plano)) {
                exibirStatus("✔ Plano '" + plano.getNome() + "' cadastrado!");
                limparFormulario();
                carregarPlanos();
            } else {
                exibirStatus("✗ Erro ao cadastrar plano.");
            }
        } else {
            plano.setId(idEmEdicao);
            if (planoDAO.atualizar(plano)) {
                exibirStatus("✔ Plano atualizado com sucesso!");
                limparFormulario();
                carregarPlanos();
            } else {
                exibirStatus("✗ Erro ao atualizar plano.");
            }
        }
    }

    /** Ação do botão "Excluir". */
    @FXML
    private void onExcluirClicado() {
        Plano selecionado = tabelaPlanos.getSelectionModel().getSelectedItem();
        if (selecionado == null) { exibirStatus("Selecione um plano."); return; }
        if (planoDAO.estaEmUso(selecionado.getId())) {
            exibirStatus("⚠ Este plano possui matrículas vinculadas e não pode ser excluído.");
            return;
        }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir plano '" + selecionado.getNome() + "'?",
                ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (planoDAO.excluir(selecionado.getId())) {
                    exibirStatus("✔ Plano excluído.");
                    limparFormulario();
                    carregarPlanos();
                } else {
                    exibirStatus("✗ Não é possível excluir: plano em uso.");
                }
            }
        });
    }

    /** Ação do botão "Novo". */
    @FXML
    private void onNovoClicado() {
        limparFormulario();
        tabelaPlanos.getSelectionModel().clearSelection();
    }

    /** Cancela o cadastro ou a edição sem salvar. */
    @FXML
    private void onCancelarClicado() {
        limparFormulario();
        tabelaPlanos.getSelectionModel().clearSelection();
        exibirStatus("ℹ Operação cancelada. Nenhum dado foi salvo.");
    }

    // ── Auxiliares ────────────────────────────────────────────────────────

    private boolean validarCampos() {
        try {
            double valor = Double.parseDouble(campValor.getText().replace(",", "."));
            int duracao = Integer.parseInt(campDuracao.getText());
            String erro = PlanoValidator.validar(campNome.getText(), campDescricao.getText(),
                    campCondicoes.getText(), valor, duracao);
            if (erro != null) {
                exibirStatus("⚠ " + erro);
                return false;
            }
        } catch (NumberFormatException e) {
            exibirStatus("⚠ 'Valor' e 'Duração' devem ser números.");
            return false;
        }
        return true;
    }

    private Plano montarObjetoPlano() {
        Plano p = new Plano();
        p.setNome(campNome.getText().trim());
        p.setDescricao(campDescricao.getText().trim());
        p.setCondicoesUtilizacao(campCondicoes.getText().trim());
        p.setValor(Double.parseDouble(campValor.getText().replace(",", ".")));
        p.setDuracaoDias(Integer.parseInt(campDuracao.getText().trim()));
        return p;
    }

    private void preencherFormulario(Plano plano) {
        idEmEdicao = plano.getId();
        campNome.setText(plano.getNome());
        campDescricao.setText(plano.getDescricao());
        campCondicoes.setText(plano.getCondicoesUtilizacao());
        campValor.setText(String.format("%.2f", plano.getValor()));
        campDuracao.setText(String.valueOf(plano.getDuracaoDias()));
        if (labelTituloForm != null) labelTituloForm.setText("Editando Plano");
    }

    private void limparFormulario() {
        idEmEdicao = 0;
        campNome.clear();
        campDescricao.clear();
        campCondicoes.clear();
        campValor.clear();
        campDuracao.clear();
        labelStatus.setText("");
        if (labelTituloForm != null) labelTituloForm.setText("Novo Plano");
    }

    private void exibirStatus(String msg) { labelStatus.setText(msg); }
}
