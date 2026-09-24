package com.academia.controller;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.model.Aluno;
import com.academia.model.AlunoMatriculaDTO;
import com.academia.model.Usuario;
import com.academia.validation.AlunoValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller: Cadastro de Alunos — UC 01.
 *
 * <p>
 * Permite cadastrar, editar e listar alunos da academia.
 * Agora também lista e permite cancelar as matrículas diretamente.
 * </p>
 */
public class AlunoController {

    // ── Campos do formulário ──────────────────────────────────────────────

    @FXML
    private TextField campNome;
    @FXML
    private TextField campCpf;
    @FXML
    private TextField campEmail;
    @FXML
    private TextField campTelefone;
    @FXML
    private TextField campEndereco; // Adicionado: campo de endereço
    @FXML
    private DatePicker campDataNascimento;
    @FXML
    private TextArea campObservacoes;
    @FXML
    private ScrollPane painelCadastro;
    @FXML
    private Button btnCancelarMatricula;

    private boolean podeAlterar;

    public void configurarAcesso(Usuario usuario) {
        podeAlterar = usuario != null && "FUNCIONARIO".equals(usuario.getPerfil());
        painelCadastro.setDisable(!podeAlterar);
        btnCancelarMatricula.setDisable(!podeAlterar);
    }

    // ── Controles da listagem ─────────────────────────────────────────────

    @FXML
    private TextField campPesquisa;
    @FXML
    private Label labelStatus;
    @FXML
    private Button btnTodos;
    @FXML
    private Button btnAtivos;
    @FXML
    private Button btnCancelados;
    @FXML
    private Label labelContador;

    // ── Tabela de listagem ────────────────────────────────────────────────

    @FXML
    private TableView<AlunoMatriculaDTO> tabelaAlunos;
    @FXML
    private TableColumn<AlunoMatriculaDTO, Integer> colId;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colNome;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colCpf;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colEmail;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colTelefone;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colEndereco; // Opcional na tabela
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colDataNasc;

    // Novas colunas da matrícula
    @FXML
    private TableColumn<AlunoMatriculaDTO, Integer> colMatriculaId;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colPlano;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colInicio;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colFim;
    @FXML
    private TableColumn<AlunoMatriculaDTO, String> colStatus;

    /** DAOs para operações no banco. */
    private final AlunoDAO alunoDAO = new AlunoDAO();
    private final MatriculaDAO matriculaDAO = new MatriculaDAO();

    /** ID do aluno selecionado para edição (0 = nenhum). */
    private int idEmEdicao = 0;

    private String filtroAtual = "TODOS";
    private List<AlunoMatriculaDTO> listaCompleta = null;

    /**
     * Inicializa o controller: configura as colunas da tabela e carrega os dados.
     */
    @FXML
    public void initialize() {
        configurarColunas();
        carregarAlunos();
        aplicarMascaras();

        // Ao clicar numa linha da tabela, preenche o formulário para edição
        tabelaAlunos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, selecionado) -> {
                    if (selecionado != null)
                        preencherFormulario(selecionado);
                });

        // Listener para barra de pesquisa em tempo real
        campPesquisa.textProperty().addListener((obs, oldV, newV) -> aplicarFiltrosEBusca());
    }

    /** Configura o mapeamento das colunas da tabela. */
    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("alunoId"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        
        // Se houver a coluna colEndereco no seu FXML, descomente a linha abaixo:
        if (colEndereco != null) colEndereco.setCellValueFactory(new PropertyValueFactory<>("endereco"));
        
        colDataNasc.setCellValueFactory(new PropertyValueFactory<>("dataNascimento"));

        colMatriculaId.setCellValueFactory(new PropertyValueFactory<>("matriculaId"));
        colPlano.setCellValueFactory(new PropertyValueFactory<>("nomePlano"));
        colInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("situacao"));

        // Customização de célula para exibir Status Colorido
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "ATIVO" -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                        case "VENCIDO" -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                        case "CANCELADO" -> setStyle("-fx-text-fill: #8b949e;");
                        default -> setStyle("-fx-text-fill: #8b949e;");
                    }
                }
            }
        });
    }

    /** Busca todos os alunos e matrículas no banco e aplica os filtros atuais. */
    private void carregarAlunos() {
        listaCompleta = alunoDAO.listarAlunosComMatriculas(null);
        aplicarFiltrosEBusca();
    }

    /** Recarrega os alunos e suas matrículas ao retornar para a aba. */
    public void refresh() {
        carregarAlunos();
    }

    /**
     * Ação do botão "Salvar".
     * Realiza inserção ou atualização dependendo de {@link #idEmEdicao}.
     */
    @FXML
    private void onSalvarClicado() {
        if (!podeAlterar) return;
        if (!validarCampos())
            return;

        Aluno aluno = montarObjetoAluno();

        if (alunoDAO.cpfJaCadastrado(aluno.getCpf(), idEmEdicao)) {
            exibirStatus("⚠ Já existe um aluno cadastrado com este CPF.");
            return;
        }

        if (idEmEdicao == 0) {
            // ── Novo cadastro ────────────────────────────────────────────
            if (alunoDAO.inserir(aluno)) {
                exibirStatus("✔ Aluno '" + aluno.getNome() + "' cadastrado com sucesso!");
                limparFormulario();
                carregarAlunos();
            } else {
                exibirStatus("✗ Não foi possível cadastrar o aluno. Consulte o erro no terminal.");
            }
        } else {
            // ── Atualização ──────────────────────────────────────────────
            aluno.setId(idEmEdicao);
            if (alunoDAO.atualizar(aluno)) {
                exibirStatus("✔ Aluno atualizado com sucesso!");
                limparFormulario();
                carregarAlunos();
            } else {
                exibirStatus("✗ Erro ao atualizar aluno.");
            }
        }
    }

    /**
     * Ação do botão "Excluir".
     * Exclui o aluno selecionado após confirmação.
     */
    @FXML
    private void onExcluirClicado() {
        if (!podeAlterar) return;
        AlunoMatriculaDTO selecionado = tabelaAlunos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            exibirStatus("Selecione um aluno para excluir.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja excluir o aluno '" + selecionado.getNome() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirmacao.setTitle("Confirmar exclusão");
        confirmacao.setHeaderText(null);

        confirmacao.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                if (alunoDAO.excluir(selecionado.getAlunoId())) {
                    exibirStatus("✔ Aluno excluído.");
                    limparFormulario();
                    carregarAlunos();
                } else {
                    exibirStatus("✗ Não foi possível excluir. Verifique dependências.");
                }
            }
        });
    }

    /** Ação do botão "Novo" — limpa o formulário para um novo cadastro. */
    @FXML
    private void onNovoClicado() {
        if (!podeAlterar) return;
        limparFormulario();
        tabelaAlunos.getSelectionModel().clearSelection();
    }

    /** Ação de cancelar a matrícula selecionada diretamente na tabela. */
    @FXML
    private void onCancelarMatriculaClicado() {
        if (!podeAlterar) return;
        AlunoMatriculaDTO selecionado = tabelaAlunos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            exibirStatus("⚠ Selecione um registro na tabela para cancelar a matrícula.");
            return;
        }
        if (selecionado.getMatriculaId() == 0 || "CANCELADO".equals(selecionado.getSituacao())) {
            exibirStatus("⚠ O aluno selecionado não possui matrícula ativa para cancelar.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja cancelar a matrícula atual de " + selecionado.getNome() + "?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cancelar Matrícula");
        alert.setHeaderText(null);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                if (matriculaDAO.cancelar(selecionado.getMatriculaId())) {
                    exibirStatus("✔ Matrícula cancelada com sucesso!");
                    carregarAlunos();
                } else {
                    exibirStatus("✗ Erro ao cancelar a matrícula.");
                }
            }
        });
    }

    // ── Lógica de Filtros e Busca ─────────────────────────────────────────

    @FXML
    private void onFiltroTodos() {
        filtroAtual = "TODOS";
        atualizarEstilosBotoesFiltro(btnTodos);
        aplicarFiltrosEBusca();
    }

    @FXML
    private void onFiltroAtivos() {
        filtroAtual = "ATIVOS";
        atualizarEstilosBotoesFiltro(btnAtivos);
        aplicarFiltrosEBusca();
    }

    @FXML
    private void onFiltroCancelados() {
        filtroAtual = "CANCELADOS";
        atualizarEstilosBotoesFiltro(btnCancelados);
        aplicarFiltrosEBusca();
    }

    private void atualizarEstilosBotoesFiltro(Button botaoAtivo) {
        String estiloInativo = "-fx-background-color: transparent; -fx-text-fill: #8b949e; " +
                "-fx-border-color: #30363d; -fx-border-radius: 20; -fx-background-radius: 20; " +
                "-fx-padding: 4 16 4 16; -fx-cursor: hand; -fx-font-size: 12;";
        String estiloAtivo = "-fx-background-color: #238636; -fx-text-fill: white; " +
                "-fx-border-radius: 20; -fx-background-radius: 20; " +
                "-fx-padding: 4 16 4 16; -fx-cursor: hand; -fx-font-size: 12;";

        btnTodos.setStyle(estiloInativo);
        btnTodos.setText("Todos");
        btnAtivos.setStyle(estiloInativo);
        btnAtivos.setText("Ativos");
        btnCancelados.setStyle(estiloInativo);
        btnCancelados.setText("Cancelados");

        botaoAtivo.setStyle(estiloAtivo);
        botaoAtivo.setText("● " + botaoAtivo.getText());
    }

    private void aplicarFiltrosEBusca() {
        if (listaCompleta == null)
            return;

        String termo = campPesquisa.getText().toLowerCase().trim();

        List<AlunoMatriculaDTO> filtrados = listaCompleta.stream()
                .filter(a -> {
                    boolean statusOk = switch (filtroAtual) {
                        case "ATIVOS" -> "ATIVO".equals(a.getSituacao());
                        case "CANCELADOS" -> "CANCELADO".equals(a.getSituacao());
                        default -> true;
                    };
                    boolean nomeOk = termo.isEmpty() || a.getNome().toLowerCase().contains(termo)
                            || (a.getNomePlano() != null && a.getNomePlano().toLowerCase().contains(termo));
                    return statusOk && nomeOk;
                })
                .collect(Collectors.toList());

        tabelaAlunos.setItems(FXCollections.observableArrayList(filtrados));
        labelContador.setText(filtrados.size() + " registros");
    }

    // ── Métodos auxiliares ────────────────────────────────────────────────

    /** Valida os dados do formulário. */
    private boolean validarCampos() {
        String erro = AlunoValidator.validar(
                campNome.getText(), campCpf.getText(), campEmail.getText(), campTelefone.getText());
        if (erro == null) return true;
        exibirStatus("⚠ " + erro);
        return false;
    }

    /** Constrói um objeto {@link Aluno} a partir dos campos do formulário. */
    private Aluno montarObjetoAluno() {
        Aluno aluno = new Aluno();
        aluno.setNome(campNome.getText().trim());
        aluno.setCpf(campCpf.getText().trim());
        aluno.setEmail(campEmail.getText().trim());
        aluno.setTelefone(campTelefone.getText().trim());
        aluno.setEndereco(campEndereco.getText().trim()); // Adicionado: preenche o endereço
        aluno.setDataNascimento(
                campDataNascimento.getValue() != null
                        ? campDataNascimento.getValue().toString()
                        : null);
        aluno.setObservacoes(campObservacoes.getText().trim());
        return aluno;
    }

    /** Preenche o formulário com os dados do aluno selecionado. */
    private void preencherFormulario(AlunoMatriculaDTO aluno) {
        idEmEdicao = aluno.getAlunoId();
        campNome.setText(aluno.getNome());
        campCpf.setText(aluno.getCpf());
        campEmail.setText(aluno.getEmail());
        campTelefone.setText(aluno.getTelefone());
        
        if (aluno.getDataNascimento() != null && !aluno.getDataNascimento().isEmpty()) {
            campDataNascimento.setValue(java.time.LocalDate.parse(aluno.getDataNascimento()));
        }

        // Pega as informações completas do banco (incluindo observações e endereço caso o DTO não traga)
        Aluno aDb = alunoDAO.buscarPorId(aluno.getAlunoId());
        if (aDb != null) {
            campObservacoes.setText(aDb.getObservacoes());
            campEndereco.setText(aDb.getEndereco() != null ? aDb.getEndereco() : "");
        } else {
            campObservacoes.setText("");
            campEndereco.setText("");
        }
    }

    /**
     * Limpa todos os campos do formulário e redefine o modo para "novo cadastro".
     */
    private void limparFormulario() {
        idEmEdicao = 0;
        campNome.clear();
        campCpf.clear();
        campEmail.clear();
        campTelefone.clear();
        campEndereco.clear(); // Adicionado: limpa o endereço
        campDataNascimento.setValue(null);
        campObservacoes.clear();
        labelStatus.setText("");
    }

    /** Exibe uma mensagem de status abaixo do formulário. */
    private void exibirStatus(String msg) {
        labelStatus.setText(msg);
    }

    /**
     * Configura as máscaras para os campos de CPF, Telefone e Data de Nascimento
     */
    private void aplicarMascaras() {
        configurarMascara(campCpf, "###.###.###-##");
        configurarMascara(campDataNascimento.getEditor(), "##/##/####");
        configurarMascaraTelefone(campTelefone);
    }

    private void configurarMascara(TextField textField, String mascara) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty())
                return;

            if (oldValue != null && oldValue.length() > newValue.length()) {
                String apenasNumerosOld = oldValue.replaceAll("[^\\d]", "");
                String apenasNumerosNew = newValue.replaceAll("[^\\d]", "");
                if (apenasNumerosOld.equals(apenasNumerosNew) && !apenasNumerosNew.isEmpty()) {
                    apenasNumerosNew = apenasNumerosNew.substring(0, apenasNumerosNew.length() - 1);
                    newValue = apenasNumerosNew;
                }
            }

            String apenasNumeros = newValue.replaceAll("[^\\d]", "");
            int maxNumeros = mascara.replaceAll("[^#]", "").length();
            if (apenasNumeros.length() > maxNumeros) {
                textField.setText(oldValue);
                return;
            }

            StringBuilder formatado = new StringBuilder();
            int i = 0;
            for (char m : mascara.toCharArray()) {
                if (m == '#') {
                    if (i < apenasNumeros.length()) {
                        formatado.append(apenasNumeros.charAt(i));
                        i++;
                    } else {
                        break;
                    }
                } else {
                    if (i < apenasNumeros.length()) {
                        formatado.append(m);
                    } else {
                        break;
                    }
                }
            }

            String finalString = formatado.toString();
            if (!newValue.equals(finalString)) {
                textField.setText(finalString);
            }
        });
    }

    private void configurarMascaraTelefone(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty())
                return;

            if (oldValue != null && oldValue.length() > newValue.length()) {
                String apenasNumerosOld = oldValue.replaceAll("[^\\d]", "");
                String apenasNumerosNew = newValue.replaceAll("[^\\d]", "");
                if (apenasNumerosOld.equals(apenasNumerosNew) && !apenasNumerosNew.isEmpty()) {
                    apenasNumerosNew = apenasNumerosNew.substring(0, apenasNumerosNew.length() - 1);
                    newValue = apenasNumerosNew;
                }
            }

            String apenasNumeros = newValue.replaceAll("[^\\d]", "");
            if (apenasNumeros.length() > 11) {
                textField.setText(oldValue);
                return;
            }

            String mascara = apenasNumeros.length() <= 10 ? "(##) ####-####" : "(##) # ####-####";
            StringBuilder formatado = new StringBuilder();
            int i = 0;
            for (char m : mascara.toCharArray()) {
                if (m == '#') {
                    if (i < apenasNumeros.length()) {
                        formatado.append(apenasNumeros.charAt(i));
                        i++;
                    } else {
                        break;
                    }
                } else {
                    if (i < apenasNumeros.length()) {
                        formatado.append(m);
                    } else {
                        break;
                    }
                }
            }

            String finalString = formatado.toString();
            if (!newValue.equals(finalString)) {
                textField.setText(finalString);
            }
        });
    }
}
