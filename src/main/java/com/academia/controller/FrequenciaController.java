package com.academia.controller;

import com.academia.dao.FrequenciaDAO;
import com.academia.model.FrequenciaDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class FrequenciaController {

    @FXML private TextField txtBusca;
    @FXML private Label lblAguardando;
    @FXML private VBox vboxStatus;
    @FXML private Label lblAvatar;
    @FXML private Label lblStatusAcesso;
    @FXML private Label lblNomeAluno;
    @FXML private Label lblMatriculaCpf;
    @FXML private Label lblDetalhePlano;
    @FXML private TableView<FrequenciaDTO> tabelaHistorico;
    @FXML private TextField txtFiltroAluno;
    @FXML private DatePicker dataInicioFiltro;
    @FXML private DatePicker dataFimFiltro;
    @FXML private Label lblHistorico;

    private final FrequenciaDAO frequenciaDAO = new FrequenciaDAO();
    private boolean historicoFiltrado;

    @FXML
    public void initialize() {
        configurarTabela();
        carregarHistorico();
        
        // Mantém o foco no campo de busca para facilitar leitura de código de barras
        Platform.runLater(() -> txtBusca.requestFocus());
    }

    /** Chamado pelo main controller quando a aba é selecionada. */
    public void refresh() {
        if (historicoFiltrado) consultarHistorico();
        else carregarHistorico();
        limparStatus();
        Platform.runLater(() -> txtBusca.requestFocus());
    }

    private void configurarTabela() {
        TableColumn<FrequenciaDTO, String> colHora = new TableColumn<>("Data/Hora");
        colHora.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDataHoraEntrada()));
        colHora.setPrefWidth(150);

        TableColumn<FrequenciaDTO, String> colAluno = new TableColumn<>("Aluno");
        colAluno.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeAluno()));
        colAluno.setPrefWidth(200);

        TableColumn<FrequenciaDTO, String> colMatricula = new TableColumn<>("Documento/ID");
        colMatricula.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMatriculaCpf()));
        colMatricula.setPrefWidth(200);

        TableColumn<FrequenciaDTO, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusAcesso()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    if ("Liberado".equalsIgnoreCase(item)) {
                        setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                    } else if ("Anterior".equalsIgnoreCase(item)) {
                        setStyle("-fx-background-color: #6e7681; -fx-text-fill: white; -fx-background-radius: 5;");
                    } else {
                        setStyle("-fx-background-color: #da3633; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                    }
                }
            }
        });
        colStatus.setPrefWidth(120);

        TableColumn<FrequenciaDTO, String> colPlano = new TableColumn<>("Detalhe");
        colPlano.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDetalhePlano()));
        colPlano.setPrefWidth(250);

        tabelaHistorico.getColumns().setAll(colHora, colAluno, colMatricula, colStatus, colPlano);
    }

    private void carregarHistorico() {
        List<FrequenciaDTO> lista = frequenciaDAO.listarHistoricoRecente(10);
        ObservableList<FrequenciaDTO> dados = FXCollections.observableArrayList(lista);
        tabelaHistorico.setItems(dados);
        lblHistorico.setText("Últimas 10 entradas");
    }

    @FXML
    private void consultarHistorico() {
        LocalDate inicio = dataInicioFiltro.getValue();
        LocalDate fim = dataFimFiltro.getValue();
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            lblHistorico.setText("A data inicial deve ser anterior à final.");
            return;
        }
        historicoFiltrado = true;
        tabelaHistorico.setItems(FXCollections.observableArrayList(
                frequenciaDAO.listarHistorico(txtFiltroAluno.getText(), inicio, fim, null)));
        lblHistorico.setText("Histórico completo: " + tabelaHistorico.getItems().size() + " registro(s)");
    }

    @FXML
    private void limparFiltro() {
        txtFiltroAluno.clear();
        dataInicioFiltro.setValue(null);
        dataFimFiltro.setValue(null);
        historicoFiltrado = false;
        carregarHistorico();
    }

    @FXML
    private void onBuscaRealizada(ActionEvent event) {
        String termo = txtBusca.getText().trim();
        if (termo.isEmpty()) return;

        FrequenciaDAO.ResultadoRegistro resultado = frequenciaDAO.registrarEntrada(termo);
        if (resultado.status() == FrequenciaDAO.Status.NAO_ENCONTRADO || resultado.status() == FrequenciaDAO.Status.ERRO)
            mostrarErro(resultado.detalhe());
        else
            mostrarResultado(new FrequenciaDTO(null, resultado.nome(), resultado.identificacao(),
                    resultado.status() == FrequenciaDAO.Status.LIBERADO ? "Liberado" : "Bloqueado", resultado.detalhe()));

        if (historicoFiltrado) consultarHistorico();
        else carregarHistorico();

        txtBusca.clear();
        txtBusca.requestFocus();
    }

    private void mostrarResultado(FrequenciaDTO dto) {
        lblNomeAluno.setText(dto.getNomeAluno());
        lblMatriculaCpf.setText(dto.getMatriculaCpf());
        lblDetalhePlano.setText(dto.getDetalhePlano());
        lblStatusAcesso.setText(dto.getStatusAcesso().toUpperCase());

        if ("Liberado".equalsIgnoreCase(dto.getStatusAcesso())) {
            vboxStatus.setStyle("-fx-background-color: #238636; -fx-background-radius: 10; -fx-padding: 40;");
            lblAvatar.setStyle("-fx-font-size: 80px; -fx-text-fill: white; -fx-background-color: #2ea043; -fx-background-radius: 100; -fx-padding: 20; -fx-min-width: 140; -fx-min-height: 140; -fx-alignment: center;");
        } else {
            vboxStatus.setStyle("-fx-background-color: #da3633; -fx-background-radius: 10; -fx-padding: 40;");
            lblAvatar.setStyle("-fx-font-size: 80px; -fx-text-fill: white; -fx-background-color: #b62324; -fx-background-radius: 100; -fx-padding: 20; -fx-min-width: 140; -fx-min-height: 140; -fx-alignment: center;");
        }
    }

    private void mostrarErro(String mensagem) {
        lblNomeAluno.setText("-");
        lblMatriculaCpf.setText("ID/CPF: -");
        lblDetalhePlano.setText("Plano: -");
        lblStatusAcesso.setText(mensagem);
        
        vboxStatus.setStyle("-fx-background-color: #da3633; -fx-background-radius: 10; -fx-padding: 40;");
        lblAvatar.setStyle("-fx-font-size: 80px; -fx-text-fill: white; -fx-background-color: #b62324; -fx-background-radius: 100; -fx-padding: 20; -fx-min-width: 140; -fx-min-height: 140; -fx-alignment: center;");
    }

    private void limparStatus() {
        lblNomeAluno.setText("-");
        lblMatriculaCpf.setText("ID/CPF: -");
        lblDetalhePlano.setText("Plano: -");
        lblStatusAcesso.setText("Aguardando...");
        vboxStatus.setStyle("-fx-background-color: #161b22; -fx-background-radius: 10; -fx-padding: 40;");
        lblAvatar.setStyle("-fx-font-size: 80px; -fx-text-fill: #8b949e; -fx-background-color: #21262d; -fx-background-radius: 100; -fx-padding: 20; -fx-min-width: 140; -fx-min-height: 140; -fx-alignment: center;");
    }
}
