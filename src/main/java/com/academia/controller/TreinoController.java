package com.academia.controller;

import com.academia.dao.AlunoDAO;
import com.academia.dao.TreinoDAO;
import com.academia.model.Aluno;
import com.academia.model.Exercicio;
import com.academia.model.TreinoDivisao;
import com.academia.model.Usuario;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TreinoController {

    @FXML
    private ComboBox<Aluno> comboAluno;
    @FXML
    private Button btnDuplicar;
    @FXML
    private Button btnExportar;
    @FXML
    private Button btnSalvar;
    @FXML
    private TabPane tabPaneTreinos;
    @FXML
    private Button btnAdicionarExercicio;
    @FXML
    private Label labelTotalExercicios;
    @FXML
    private Label labelTotalSeries;
    @FXML
    private Label labelDataEdicao;

    private Usuario usuarioLogado;
    private final AlunoDAO alunoDAO = new AlunoDAO();
    private final TreinoDAO treinoDAO = new TreinoDAO();

    private final Tab tabNovoGrupo = new Tab("+ Novo Grupo");

    @FXML
    public void initialize() {
        carregarAlunos();

        // Configura Tab Especial de Adicionar Grupo
        tabNovoGrupo.setClosable(false);
        tabNovoGrupo.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b949e;");
        tabPaneTreinos.getTabs().add(tabNovoGrupo);

        tabPaneTreinos.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabNovoGrupo) {
                adicionarNovaDivisao("Nova Divisão", true);
            }
        });

        comboAluno.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                carregarTreinoAluno(newVal);
            }
        });
    }

    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
        aplicarPermissoes();
    }

    private void aplicarPermissoes() {
        boolean isInstrutor = usuarioLogado != null && usuarioLogado.isInstrutor();
        btnSalvar.setDisable(!isInstrutor);
        btnAdicionarExercicio.setDisable(!isInstrutor);

        // Se não for instrutor, não permite adicionar ou fechar abas
        if (!isInstrutor) {
            tabPaneTreinos.getTabs().remove(tabNovoGrupo);
            tabPaneTreinos.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        } else {
            if (!tabPaneTreinos.getTabs().contains(tabNovoGrupo)) {
                tabPaneTreinos.getTabs().add(tabNovoGrupo);
            }
            tabPaneTreinos.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        }

        // Atualiza as tabelas existentes
        for (Tab tab : tabPaneTreinos.getTabs()) {
            if (tab != tabNovoGrupo && tab.getContent() instanceof TableView) {
                ((TableView<?>) tab.getContent()).setEditable(isInstrutor);
            }
        }
    }

    private void carregarAlunos() {
        comboAluno.getItems().clear();
        comboAluno.getItems().addAll(alunoDAO.listarTodos());
    }

    public void refresh() {
        Aluno selecionado = comboAluno.getSelectionModel().getSelectedItem();
        carregarAlunos();
        if (selecionado != null) {
            // Tenta reselecionar o aluno (já que a lista foi recarregada)
            comboAluno.getItems().stream()
                    .filter(a -> a.getId() == selecionado.getId())
                    .findFirst()
                    .ifPresent(a -> comboAluno.getSelectionModel().select(a));
        }
    }

    private void carregarTreinoAluno(Aluno aluno) {
        // Limpa abas atuais
        tabPaneTreinos.getTabs().clear();
        if (usuarioLogado != null && usuarioLogado.isInstrutor()) {
            tabPaneTreinos.getTabs().add(tabNovoGrupo);
        }

        List<TreinoDivisao> divisoes = treinoDAO.listarDivisoesPorAluno(aluno.getId());

        if (divisoes.isEmpty()) {
            labelDataEdicao.setText("Última edição: Nunca");
            if (usuarioLogado != null && usuarioLogado.isInstrutor()) {
                adicionarNovaDivisao("Treino A", false);
            }
        } else {
            labelDataEdicao.setText(
                    "Última edição: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            for (TreinoDivisao div : divisoes) {
                adicionarAbaDivisao(div);
            }
            // Seleciona a primeira aba
            if (!tabPaneTreinos.getTabs().isEmpty() && tabPaneTreinos.getTabs().get(0) != tabNovoGrupo) {
                tabPaneTreinos.getSelectionModel().select(0);
            }
        }
        atualizarTotais();
    }

    private void adicionarNovaDivisao(String nomePadrao, boolean pedirNome) {
        String nome = nomePadrao;
        if (pedirNome) {
            TextInputDialog dialog = new TextInputDialog(nomePadrao);
            dialog.setTitle("Nova Divisão");
            dialog.setHeaderText("Nome da Divisão de Treino");
            dialog.setContentText("Nome:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                nome = result.get().trim();
            } else {
                // Seleciona aba anterior
                if (tabPaneTreinos.getTabs().size() > 1) {
                    tabPaneTreinos.getSelectionModel().select(tabPaneTreinos.getTabs().size() - 2);
                }
                return;
            }
        }

        TreinoDivisao novaDivisao = new TreinoDivisao(nome);
        adicionarAbaDivisao(novaDivisao);
    }

    private void adicionarAbaDivisao(TreinoDivisao divisao) {
        Tab aba = new Tab(divisao.getNomeDivisao());
        aba.setUserData(divisao);

        TableView<Exercicio> tabela = criarTabelaExercicios(divisao.getExercicios());
        aba.setContent(tabela);

        // Insere a aba antes do botão "+ Novo Grupo" (se existir)
        int indexNovoGrupo = tabPaneTreinos.getTabs().indexOf(tabNovoGrupo);
        if (indexNovoGrupo >= 0) {
            tabPaneTreinos.getTabs().add(indexNovoGrupo, aba);
        } else {
            tabPaneTreinos.getTabs().add(aba);
        }

        tabPaneTreinos.getSelectionModel().select(aba);

        // Listener para atualizar totais quando a lista mudar
        divisao.getExercicios().addListener((ListChangeListener<Exercicio>) c -> atualizarTotais());

        // Se a aba for fechada, atualiza totais
        aba.setOnClosed(e -> atualizarTotais());
    }

    @SuppressWarnings("unchecked")
    private TableView<Exercicio> criarTabelaExercicios(List<Exercicio> listaExercicios) {
        TableView<Exercicio> tabela = new TableView<>();
        tabela.setEditable(usuarioLogado != null && usuarioLogado.isInstrutor());
        tabela.getItems().addAll(listaExercicios);

        TableColumn<Exercicio, Integer> colOrdem = new TableColumn<>("#");
        colOrdem.setCellValueFactory(new PropertyValueFactory<>("ordem"));
        colOrdem.setPrefWidth(40);
        colOrdem.setCellFactory(celulaEditavel(new IntegerStringConverter()));
        colOrdem.setOnEditCommit(e -> e.getRowValue().setOrdem(e.getNewValue() != null ? e.getNewValue() : 0));

        TableColumn<Exercicio, String> colGrupo = new TableColumn<>("Grupo Muscular");
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupoMuscular"));
        colGrupo.setCellFactory(celulaEditavel(new javafx.util.converter.DefaultStringConverter()));
        colGrupo.setPrefWidth(120);
        colGrupo.setOnEditCommit(e -> e.getRowValue().setGrupoMuscular(e.getNewValue()));

        TableColumn<Exercicio, String> colNome = new TableColumn<>("Exercício");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setCellFactory(celulaEditavel(new javafx.util.converter.DefaultStringConverter()));
        colNome.setPrefWidth(200);
        colNome.setOnEditCommit(e -> e.getRowValue().setNome(e.getNewValue()));

        TableColumn<Exercicio, Integer> colSeries = new TableColumn<>("Séries");
        colSeries.setCellValueFactory(new PropertyValueFactory<>("series"));
        colSeries.setCellFactory(celulaEditavel(new IntegerStringConverter()));
        colSeries.setPrefWidth(60);
        colSeries.setOnEditCommit(e -> e.getRowValue().setSeries(e.getNewValue() != null ? e.getNewValue() : 0));

        TableColumn<Exercicio, String> colReps = new TableColumn<>("Reps");
        colReps.setCellValueFactory(new PropertyValueFactory<>("repeticoes"));
        colReps.setCellFactory(celulaEditavel(new javafx.util.converter.DefaultStringConverter()));
        colReps.setPrefWidth(80);
        colReps.setOnEditCommit(e -> e.getRowValue().setRepeticoes(e.getNewValue()));

        // Coluna Carga com cor verde (custom CellFactory)
        TableColumn<Exercicio, String> colCarga = new TableColumn<>("Carga (kg)");
        colCarga.setCellValueFactory(new PropertyValueFactory<>("carga"));
        colCarga.setCellFactory(tc -> new TextFieldTableCell<>(new javafx.util.converter.DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (isEditing() && getGraphic() instanceof javafx.scene.control.TextField) {
                    javafx.scene.control.TextField textField = (javafx.scene.control.TextField) getGraphic();
                    javafx.application.Platform.runLater(textField::selectAll);
                    textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            try {
                                commitEdit(textField.getText());
                            } catch (Exception ex) {
                                cancelEdit();
                            }
                        }
                    });
                }
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Aplica cor verde (success) se houver valor
                    setStyle("-fx-text-fill: -color-success-emphasis; -fx-font-weight: bold;");
                }
            }
        });
        colCarga.setPrefWidth(80);
        colCarga.setOnEditCommit(e -> e.getRowValue().setCarga(e.getNewValue()));

        TableColumn<Exercicio, Integer> colDesc = new TableColumn<>("Descanso (s)");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descanso"));
        colDesc.setCellFactory(celulaEditavel(new IntegerStringConverter()));
        colDesc.setPrefWidth(80);
        colDesc.setOnEditCommit(e -> e.getRowValue().setDescanso(e.getNewValue() != null ? e.getNewValue() : 0));

        TableColumn<Exercicio, String> colObs = new TableColumn<>("Observações");
        colObs.setCellValueFactory(new PropertyValueFactory<>("observacoes"));
        colObs.setCellFactory(celulaEditavel(new javafx.util.converter.DefaultStringConverter()));
        colObs.setPrefWidth(150);
        colObs.setOnEditCommit(e -> e.getRowValue().setObservacoes(e.getNewValue()));

        // Coluna de Ações (Excluir)
        TableColumn<Exercicio, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setPrefWidth(70);
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnExcluir = new Button();
            {
                // Usando ícone do feather via ikonli se disponível, caso contrário texto
                btnExcluir.setText("X");
                btnExcluir.setStyle("-fx-text-fill: -color-danger-emphasis; -fx-background-color: transparent;");
                btnExcluir.setOnAction(event -> {
                    Exercicio ex = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(ex);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || (!usuarioLogado.isInstrutor())) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(btnExcluir);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        tabela.getColumns().addAll(colOrdem, colGrupo, colNome, colSeries, colReps, colCarga, colDesc, colObs,
                colAcoes);

        return tabela;
    }

    @FXML
    private void onAdicionarExercicioClicado() {
        Tab abaAtual = tabPaneTreinos.getSelectionModel().getSelectedItem();
        if (abaAtual != null && abaAtual != tabNovoGrupo && abaAtual.getContent() instanceof TableView) {
            @SuppressWarnings("unchecked")
            TableView<Exercicio> tabela = (TableView<Exercicio>) abaAtual.getContent();
            Exercicio novoEx = new Exercicio(tabela.getItems().size() + 1);
            tabela.getItems().add(novoEx);
        }
    }

    @FXML
    private void onSalvarClicado() {
        Aluno alunoSelecionado = comboAluno.getSelectionModel().getSelectedItem();
        if (alunoSelecionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Selecione um aluno primeiro!");
            alert.showAndWait();
            return;
        }

        List<TreinoDivisao> divisoesParaSalvar = new ArrayList<>();

        for (Tab tab : tabPaneTreinos.getTabs()) {
            if (tab != tabNovoGrupo && tab.getUserData() instanceof TreinoDivisao) {
                TreinoDivisao div = (TreinoDivisao) tab.getUserData();

                // Pega os itens da tabela caso o ObservableList não esteja sincronizado (embora
                // devesse estar)
                if (tab.getContent() instanceof TableView) {
                    @SuppressWarnings("unchecked")
                    TableView<Exercicio> tabela = (TableView<Exercicio>) tab.getContent();
                    div.getExercicios().setAll(tabela.getItems());
                }
                divisoesParaSalvar.add(div);
            }
        }

        boolean sucesso = treinoDAO.salvarTreinos(alunoSelecionado.getId(), divisoesParaSalvar);

        Alert alert = new Alert(sucesso ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle("Salvar Treino");
        alert.setHeaderText(sucesso ? "Treino salvo com sucesso!" : "Erro ao salvar treino.");
        alert.showAndWait();

        if (sucesso) {
            labelDataEdicao.setText(
                    "Última edição: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
    }

    @FXML
    private void onExportarClicado() {
        Aluno alunoSelecionado = comboAluno.getSelectionModel().getSelectedItem();
        if (alunoSelecionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Selecione um aluno primeiro!");
            alert.showAndWait();
            return;
        }

        List<TreinoDivisao> divisoesParaExportar = new ArrayList<>();
        for (Tab tab : tabPaneTreinos.getTabs()) {
            if (tab != tabNovoGrupo && tab.getUserData() instanceof TreinoDivisao) {
                TreinoDivisao div = (TreinoDivisao) tab.getUserData();
                if (tab.getContent() instanceof TableView) {
                    @SuppressWarnings("unchecked")
                    TableView<Exercicio> tabela = (TableView<Exercicio>) tab.getContent();
                    // Atualiza a lista com o que está na tela antes de exportar
                    div.getExercicios().setAll(tabela.getItems());
                }
                divisoesParaExportar.add(div);
            }
        }

        if (divisoesParaExportar.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Não há treinos para exportar.");
            alert.showAndWait();
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Salvar Ficha de Treino em PDF");
        fileChooser.setInitialFileName("Treino_" + alunoSelecionado.getNome().replace(" ", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Arquivos PDF", "*.pdf"));
        
        // Pega a janela atual a partir do botão
        java.io.File file = fileChooser.showSaveDialog(btnExportar.getScene().getWindow());

        if (file != null) {
            try {
                com.academia.util.PdfGenerator.gerarFichaTreino(alunoSelecionado, divisoesParaExportar, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "PDF gerado com sucesso em:\n" + file.getAbsolutePath());
                alert.setTitle("Exportação Concluída");
                alert.setHeaderText("Sucesso");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao gerar PDF: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void atualizarTotais() {
        int totalEx = 0;
        int totalSeries = 0;
        for (Tab tab : tabPaneTreinos.getTabs()) {
            if (tab != tabNovoGrupo && tab.getContent() instanceof TableView) {
                @SuppressWarnings("unchecked")
                TableView<Exercicio> tabela = (TableView<Exercicio>) tab.getContent();
                totalEx += tabela.getItems().size();
                totalSeries += tabela.getItems().stream().mapToInt(Exercicio::getSeries).sum();
            }
        }
        labelTotalExercicios.setText("Total de Exercícios: " + totalEx);
        labelTotalSeries.setText("Total de Séries: " + totalSeries);
    }

    private <T> javafx.util.Callback<TableColumn<Exercicio, T>, TableCell<Exercicio, T>> celulaEditavel(
            javafx.util.StringConverter<T> converter) {
        return tc -> new TextFieldTableCell<>(converter) {
            @Override
            public void startEdit() {
                super.startEdit();
                if (isEditing() && getGraphic() instanceof javafx.scene.control.TextField) {
                    javafx.scene.control.TextField textField = (javafx.scene.control.TextField) getGraphic();
                    javafx.application.Platform.runLater(textField::selectAll);
                    textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            try {
                                commitEdit(converter.fromString(textField.getText()));
                            } catch (Exception e) {
                                cancelEdit();
                            }
                        }
                    });
                }
            }
        };
    }
}
