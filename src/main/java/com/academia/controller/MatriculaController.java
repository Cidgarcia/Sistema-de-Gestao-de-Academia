package com.academia.controller;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.dao.PlanoDAO;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.Plano;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

/**
 * Controller: Realizar Matrícula — UC 03.
 *
 * <p>Permite efetivar a matrícula de um aluno em um plano, calculando
 * automaticamente a data de fim com base na duração do plano escolhido.</p>
 */
public class MatriculaController {

    // ── Formulário ────────────────────────────────────────────────────────

    @FXML private ComboBox<Aluno>     comboAluno;
    @FXML private ComboBox<Plano>     comboPlano;
    @FXML private DatePicker          campDataInicio;
    @FXML private Label               labelDataFim;

    // ── Tabela ────────────────────────────────────────────────────────────

    @FXML private TableView<Matricula>           tabelaMatriculas;
    @FXML private TableColumn<Matricula, Integer> colId;
    @FXML private TableColumn<Matricula, String>  colAluno;
    @FXML private TableColumn<Matricula, String>  colPlano;
    @FXML private TableColumn<Matricula, String>  colInicio;
    @FXML private TableColumn<Matricula, String>  colFim;
    @FXML private TableColumn<Matricula, Boolean> colAtiva;

    @FXML private Label labelStatus;

    private final MatriculaDAO matriculaDAO = new MatriculaDAO();
    private final AlunoDAO     alunoDAO     = new AlunoDAO();
    private final PlanoDAO     planoDAO     = new PlanoDAO();

    /**
     * Inicializa o controller: popula combos e configura tabela.
     */
    @FXML
    public void initialize() {
        // Popula os ComboBoxes com dados do banco
        comboAluno.setItems(FXCollections.observableArrayList(alunoDAO.listarTodos()));
        comboPlano.setItems(FXCollections.observableArrayList(planoDAO.listarTodos()));

        // Configura exibição dos combos
        comboAluno.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Aluno a)    { return a == null ? "" : a.getNome(); }
            public Aluno fromString(String s)  { return null; }
        });
        comboPlano.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Plano p)    { return p == null ? "" : p.toString(); }
            public Plano fromString(String s)  { return null; }
        });

        // Ao mudar o plano ou data de início, recalcula data de fim
        comboPlano.valueProperty().addListener((obs, ant, sel) -> calcularDataFim());
        campDataInicio.valueProperty().addListener((obs, ant, sel) -> calcularDataFim());

        // Data de início padrão: hoje
        campDataInicio.setValue(LocalDate.now());

        configurarColunas();
        carregarMatriculas();
    }

    /** Recalcula e exibe a data de término conforme plano e data de início. */
    private void calcularDataFim() {
        Plano planoSelecionado = comboPlano.getValue();
        LocalDate dataInicio = campDataInicio.getValue();

        if (planoSelecionado != null && dataInicio != null) {
            LocalDate dataFim = dataInicio.plusDays(planoSelecionado.getDuracaoDias());
            labelDataFim.setText("Data de Término: " + dataFim);
        } else {
            labelDataFim.setText("Data de Término: —");
        }
    }

    /** Ação do botão "Matricular". */
    @FXML
    private void onMatricularClicado() {
        Aluno aluno = comboAluno.getValue();
        Plano plano = comboPlano.getValue();
        LocalDate dataInicio = campDataInicio.getValue();

        if (aluno == null || plano == null || dataInicio == null) {
            exibirStatus("⚠ Selecione aluno, plano e data de início.");
            return;
        }

        LocalDate dataFim = dataInicio.plusDays(plano.getDuracaoDias());

        Matricula matricula = new Matricula();
        matricula.setAlunoId(aluno.getId());
        matricula.setPlanoId(plano.getId());
        matricula.setDataInicio(dataInicio.toString());
        matricula.setDataFim(dataFim.toString());
        matricula.setAtiva(true);

        if (matriculaDAO.inserir(matricula)) {
            exibirStatus("✔ Matrícula de '" + aluno.getNome() + "' efetuada até " + dataFim + "!");
            limparFormulario();
            carregarMatriculas();
        } else {
            exibirStatus("✗ Erro ao realizar matrícula.");
        }
    }

    /**
     * Recarrega os combos de Aluno e Plano com dados atuais do banco.
     * Chamado pelo PainelPrincipalController sempre que esta aba for selecionada.
     */
    public void refresh() {
        comboAluno.setItems(FXCollections.observableArrayList(alunoDAO.listarTodos()));
        comboPlano.setItems(FXCollections.observableArrayList(planoDAO.listarTodos()));
        carregarMatriculas();
    }

    /** Ação do botão "Cancelar Matrícula". */
    @FXML
    private void onCancelarMatriculaClicado() {
        Matricula sel = tabelaMatriculas.getSelectionModel().getSelectedItem();
        if (sel == null) { exibirStatus("Selecione uma matrícula para cancelar."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancelar matrícula de '" + sel.getNomeAluno() + "'?",
                ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (matriculaDAO.cancelar(sel.getId())) {
                    exibirStatus("✔ Matrícula cancelada.");
                    carregarMatriculas();
                } else {
                    exibirStatus("✗ Erro ao cancelar matrícula.");
                }
            }
        });
    }

    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAluno.setCellValueFactory(new PropertyValueFactory<>("nomeAluno"));
        colPlano.setCellValueFactory(new PropertyValueFactory<>("nomePlano"));
        colInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colAtiva.setCellValueFactory(new PropertyValueFactory<>("ativa"));

        // Coluna booleana: exibe "Sim" / "Não"
        colAtiva.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item ? "✔ Ativa" : "✗ Cancelada");
                setStyle(item ? "-fx-text-fill: #4caf50;" : "-fx-text-fill: #f44336;");
            }
        });
    }

    private void carregarMatriculas() {
        tabelaMatriculas.setItems(
                FXCollections.observableArrayList(matriculaDAO.listarTodas())
        );
    }

    private void limparFormulario() {
        comboAluno.setValue(null);
        comboPlano.setValue(null);
        campDataInicio.setValue(LocalDate.now());
        labelDataFim.setText("Data de Término: —");
        labelStatus.setText("");
    }

    private void exibirStatus(String msg) { labelStatus.setText(msg); }
}

