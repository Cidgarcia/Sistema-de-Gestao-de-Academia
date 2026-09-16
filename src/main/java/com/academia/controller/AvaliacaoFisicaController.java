package com.academia.controller;

import com.academia.dao.AlunoDAO;
import com.academia.dao.AvaliacaoFisicaDAO;
import com.academia.model.Aluno;
import com.academia.model.AvaliacaoFisica;
import com.academia.model.Usuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

/**
 * Controller: Registrar Avaliação Física — UC 05.
 *
 * <p>Acesso restrito a usuários com perfil INSTRUTOR.
 * Permite registrar peso, altura, gordura corporal, massa muscular e
 * observações, calculando o IMC automaticamente.</p>
 *
 * <p>O usuário logado é injetado pelo {@link PainelPrincipalController}
 * via {@link #setUsuarioLogado(Usuario)}.</p>
 */
public class AvaliacaoFisicaController {

    // ── Formulário ────────────────────────────────────────────────────────

    @FXML private ComboBox<Aluno> comboAluno;
    @FXML private DatePicker      campData;
    @FXML private TextField       campPeso;
    @FXML private TextField       campAltura;
    @FXML private Label           labelImc;
    @FXML private TextField       campGordura;
    @FXML private TextField       campMassa;
    @FXML private TextArea        campObservacoes;

    // ── Tabela ────────────────────────────────────────────────────────────

    @FXML private TableView<AvaliacaoFisica>          tabelaAvaliacoes;
    @FXML private TableColumn<AvaliacaoFisica, String>  colAluno;
    @FXML private TableColumn<AvaliacaoFisica, String>  colData;
    @FXML private TableColumn<AvaliacaoFisica, Double>  colPeso;
    @FXML private TableColumn<AvaliacaoFisica, Double>  colAltura;
    @FXML private TableColumn<AvaliacaoFisica, Double>  colImc;
    @FXML private TableColumn<AvaliacaoFisica, Double>  colGordura;
    @FXML private TableColumn<AvaliacaoFisica, String>  colInstrutor;

    @FXML private Label labelStatus;

    private final AvaliacaoFisicaDAO avaliacaoDAO = new AvaliacaoFisicaDAO();
    private final AlunoDAO           alunoDAO     = new AlunoDAO();

    /** Usuário Instrutor logado — injetado pelo PainelPrincipalController. */
    private Usuario usuarioLogado;

    /**
     * Define o usuário logado. Deve ser chamado antes de qualquer interação.
     * Exibe aviso se o perfil não for INSTRUTOR.
     *
     * @param usuario Usuário autenticado.
     */
    public void setUsuarioLogado(Usuario usuario) {
        this.usuarioLogado = usuario;
        if (!usuario.isInstrutor()) {
            labelStatus.setText("⚠ Acesso restrito a Instrutores.");
        }
    }

    /**
     * Recarrega a lista de alunos no combo.
     * Chamado pelo PainelPrincipalController ao selecionar esta aba.
     */
    public void refresh() {
        comboAluno.setItems(FXCollections.observableArrayList(alunoDAO.listarTodos()));
        carregarTodasAvaliacoes();
    }

    /**
     * Inicializa o controller: configura combos, tabela e listeners.
     */
    @FXML
    public void initialize() {
        // Popula combo de alunos
        comboAluno.setItems(FXCollections.observableArrayList(alunoDAO.listarTodos()));
        comboAluno.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Aluno a)   { return a == null ? "" : a.getNome(); }
            public Aluno fromString(String s) { return null; }
        });

        // Ao selecionar aluno, carrega suas avaliações anteriores
        comboAluno.valueProperty().addListener((obs, ant, sel) -> {
            if (sel != null) carregarAvaliacoesPorAluno(sel.getId());
        });

        // Atualiza o IMC calculado ao digitar peso ou altura
        campPeso.textProperty().addListener((obs, ant, novo) -> atualizarImcCalculado());
        campAltura.textProperty().addListener((obs, ant, novo) -> atualizarImcCalculado());

        // Data padrão: hoje
        campData.setValue(LocalDate.now());

        configurarColunas();
        carregarTodasAvaliacoes();
    }

    /** Ação do botão "Salvar Avaliação". */
    @FXML
    private void onSalvarClicado() {
        // Verifica se o usuário tem permissão
        if (usuarioLogado == null || !usuarioLogado.isInstrutor()) {
            exibirStatus("⚠ Apenas Instrutores podem registrar avaliações.");
            return;
        }

        if (!validarCampos()) return;

        AvaliacaoFisica av = montarObjetoAvaliacao();

        if (avaliacaoDAO.inserir(av)) {
            exibirStatus("✔ Avaliação registrada com sucesso! IMC: " +
                    String.format("%.2f", av.getImc()));
            limparFormulario();
            carregarTodasAvaliacoes();
        } else {
            exibirStatus("✗ Erro ao salvar avaliação.");
        }
    }

    /** Ação do botão "Limpar". */
    @FXML
    private void onLimparClicado() {
        limparFormulario();
    }

    // ── Auxiliares ────────────────────────────────────────────────────────

    /**
     * Calcula e exibe o IMC em tempo real conforme o usuário digita.
     */
    private void atualizarImcCalculado() {
        try {
            double peso   = Double.parseDouble(campPeso.getText().replace(",", "."));
            double altura = Double.parseDouble(campAltura.getText().replace(",", "."));
            if (altura > 0) {
                double alturaM = altura / 100.0;
                double imc = peso / (alturaM * alturaM);
                labelImc.setText(String.format("IMC calculado: %.2f", imc));
            }
        } catch (NumberFormatException e) {
            labelImc.setText("IMC calculado: —");
        }
    }

    private void carregarAvaliacoesPorAluno(int alunoId) {
        tabelaAvaliacoes.setItems(
                FXCollections.observableArrayList(avaliacaoDAO.listarPorAluno(alunoId))
        );
    }

    private void carregarTodasAvaliacoes() {
        tabelaAvaliacoes.setItems(
                FXCollections.observableArrayList(avaliacaoDAO.listarTodas())
        );
    }

    private void configurarColunas() {
        colAluno.setCellValueFactory(new PropertyValueFactory<>("nomeAluno"));
        colData.setCellValueFactory(new PropertyValueFactory<>("dataAvaliacao"));
        colPeso.setCellValueFactory(new PropertyValueFactory<>("pesoKg"));
        colAltura.setCellValueFactory(new PropertyValueFactory<>("alturaCm"));
        colImc.setCellValueFactory(new PropertyValueFactory<>("imc"));
        colGordura.setCellValueFactory(new PropertyValueFactory<>("gorduraPerc"));
        colInstrutor.setCellValueFactory(new PropertyValueFactory<>("nomeInstrutor"));

        // Formata as colunas numéricas com 2 casas decimais
        formatarColunaDecimal(colPeso, "kg");
        formatarColunaDecimal(colAltura, "cm");
        formatarColunaDecimal(colImc, "");
        formatarColunaDecimal(colGordura, "%");
    }

    /** Formata uma coluna com casas decimais e sufixo de unidade. */
    private void formatarColunaDecimal(TableColumn<AvaliacaoFisica, Double> col, String sufixo) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%.2f %s", item, sufixo).trim());
            }
        });
    }

    private boolean validarCampos() {
        if (comboAluno.getValue() == null) {
            exibirStatus("⚠ Selecione um aluno.");
            return false;
        }
        return true;
    }

    private AvaliacaoFisica montarObjetoAvaliacao() {
        AvaliacaoFisica av = new AvaliacaoFisica();
        av.setAlunoId(comboAluno.getValue().getId());
        av.setInstrutorId(usuarioLogado.getId());
        av.setDataAvaliacao(campData.getValue() != null
                ? campData.getValue().toString()
                : LocalDate.now().toString());

        av.setPesoKg(parsarDouble(campPeso.getText()));
        av.setAlturaCm(parsarDouble(campAltura.getText()));
        av.setGorduraPerc(parsarDouble(campGordura.getText()));
        av.setMassaMuscular(parsarDouble(campMassa.getText()));
        av.setObservacoes(campObservacoes.getText().trim());

        // IMC calculado automaticamente pelo model
        av.calcularImc();
        return av;
    }

    /** Converte String para Double com segurança, retornando null se inválido. */
    private Double parsarDouble(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return Double.parseDouble(texto.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void limparFormulario() {
        comboAluno.setValue(null);
        campData.setValue(LocalDate.now());
        campPeso.clear();
        campAltura.clear();
        campGordura.clear();
        campMassa.clear();
        campObservacoes.clear();
        labelImc.setText("IMC calculado: —");
        labelStatus.setText("");
    }

    private void exibirStatus(String msg) { labelStatus.setText(msg); }
}

