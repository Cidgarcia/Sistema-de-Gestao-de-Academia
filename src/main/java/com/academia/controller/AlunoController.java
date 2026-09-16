package com.academia.controller;

import com.academia.dao.AlunoDAO;
import com.academia.model.Aluno;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller: Cadastro de Alunos — UC 01.
 *
 * <p>Permite cadastrar, editar e listar alunos da academia.
 * Os dados incluem: nome, CPF, email, telefone, data de nascimento e observações.</p>
 */
public class AlunoController {

    // ── Campos do formulário ──────────────────────────────────────────────

    @FXML private TextField      campNome;
    @FXML private TextField      campCpf;
    @FXML private TextField      campEmail;
    @FXML private TextField      campTelefone;
    @FXML private DatePicker     campDataNascimento;
    @FXML private TextArea       campObservacoes;

    // ── Tabela de listagem ────────────────────────────────────────────────

    @FXML private TableView<Aluno>        tabelaAlunos;
    @FXML private TableColumn<Aluno, Integer> colId;
    @FXML private TableColumn<Aluno, String>  colNome;
    @FXML private TableColumn<Aluno, String>  colCpf;
    @FXML private TableColumn<Aluno, String>  colEmail;
    @FXML private TableColumn<Aluno, String>  colTelefone;
    @FXML private TableColumn<Aluno, String>  colDataNasc;

    @FXML private TextField campPesquisa;
    @FXML private Label     labelStatus;

    /** DAO para operações no banco. */
    private final AlunoDAO alunoDAO = new AlunoDAO();

    /** ID do aluno selecionado para edição (0 = nenhum). */
    private int idEmEdicao = 0;

    /**
     * Inicializa o controller: configura as colunas da tabela e carrega os dados.
     */
    @FXML
    public void initialize() {
        configurarColunas();
        carregarAlunos();

        // Ao clicar numa linha da tabela, preenche o formulário para edição
        tabelaAlunos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, selecionado) -> {
                    if (selecionado != null) preencherFormulario(selecionado);
                }
        );
    }

    /** Configura o mapeamento das colunas da tabela. */
    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colDataNasc.setCellValueFactory(new PropertyValueFactory<>("dataNascimento"));
    }

    /** Busca todos os alunos no banco e atualiza a tabela. */
    private void carregarAlunos() {
        ObservableList<Aluno> lista = FXCollections.observableArrayList(alunoDAO.listarTodos());
        tabelaAlunos.setItems(lista);
    }

    /**
     * Ação do botão "Salvar".
     * Realiza inserção ou atualização dependendo de {@link #idEmEdicao}.
     */
    @FXML
    private void onSalvarClicado() {
        if (!validarCampos()) return;

        Aluno aluno = montarObjetoAluno();

        if (idEmEdicao == 0) {
            // ── Novo cadastro ────────────────────────────────────────────
            if (alunoDAO.inserir(aluno)) {
                exibirStatus("✔ Aluno '" + aluno.getNome() + "' cadastrado com sucesso!");
                limparFormulario();
                carregarAlunos();
            } else {
                exibirStatus("✗ Erro ao cadastrar aluno. Verifique o CPF.");
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
        Aluno selecionado = tabelaAlunos.getSelectionModel().getSelectedItem();
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
                if (alunoDAO.excluir(selecionado.getId())) {
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
        limparFormulario();
        tabelaAlunos.getSelectionModel().clearSelection();
    }

    /** Ação do botão "Pesquisar" — filtra alunos pelo nome. */
    @FXML
    private void onPesquisarClicado() {
        String termo = campPesquisa.getText().trim();
        ObservableList<Aluno> lista = termo.isEmpty()
                ? FXCollections.observableArrayList(alunoDAO.listarTodos())
                : FXCollections.observableArrayList(alunoDAO.buscarPorNome(termo));
        tabelaAlunos.setItems(lista);
    }

    // ── Métodos auxiliares ────────────────────────────────────────────────

    /** Valida os campos obrigatórios do formulário. */
    private boolean validarCampos() {
        if (campNome.getText().trim().isEmpty()) {
            exibirStatus("⚠ O campo 'Nome' é obrigatório.");
            return false;
        }
        return true;
    }

    /** Constrói um objeto {@link Aluno} a partir dos campos do formulário. */
    private Aluno montarObjetoAluno() {
        Aluno aluno = new Aluno();
        aluno.setNome(campNome.getText().trim());
        aluno.setCpf(campCpf.getText().trim());
        aluno.setEmail(campEmail.getText().trim());
        aluno.setTelefone(campTelefone.getText().trim());
        aluno.setDataNascimento(
                campDataNascimento.getValue() != null
                        ? campDataNascimento.getValue().toString()
                        : null
        );
        aluno.setObservacoes(campObservacoes.getText().trim());
        return aluno;
    }

    /** Preenche o formulário com os dados do aluno selecionado. */
    private void preencherFormulario(Aluno aluno) {
        idEmEdicao = aluno.getId();
        campNome.setText(aluno.getNome());
        campCpf.setText(aluno.getCpf());
        campEmail.setText(aluno.getEmail());
        campTelefone.setText(aluno.getTelefone());
        if (aluno.getDataNascimento() != null && !aluno.getDataNascimento().isEmpty()) {
            campDataNascimento.setValue(java.time.LocalDate.parse(aluno.getDataNascimento()));
        }
        campObservacoes.setText(aluno.getObservacoes());
    }

    /** Limpa todos os campos do formulário e redefine o modo para "novo cadastro". */
    private void limparFormulario() {
        idEmEdicao = 0;
        campNome.clear();
        campCpf.clear();
        campEmail.clear();
        campTelefone.clear();
        campDataNascimento.setValue(null);
        campObservacoes.clear();
        labelStatus.setText("");
    }

    /** Exibe uma mensagem de status abaixo do formulário. */
    private void exibirStatus(String msg) {
        labelStatus.setText(msg);
    }
}

