package com.academia.controller;

import com.academia.dao.MatriculaDAO;
import com.academia.dao.PagamentoDAO;
import com.academia.dao.AlunoDAO;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.Pagamento;
import com.academia.model.PendenciaFinanceira;
import com.academia.observer.Observer;
import com.academia.observer.Subject;
import com.academia.validation.PagamentoValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller: Controlar Pagamentos — UC 04.
 *
 * <p>Implementa a interface {@link Subject} do Padrão Observer.
 * Ao registrar um pagamento, notifica os observers (PainelPrincipal)
 * para que o "Total Recebido no Dia" seja atualizado em tempo real.</p>
 *
 * <p>Também exibe a lista de alunos com mensalidades atrasadas.</p>
 */
public class PagamentoController implements Subject {

    // ── Lista de observers registrados (Padrão Observer) ──────────────────
    private final List<Observer> observers = new ArrayList<>();

    // ── Formulário de registro de pagamento ──────────────────────────────

    @FXML private TextField              campoBuscaAluno;
    @FXML private ListView<Matricula>     listaResultadosAluno;
    @FXML private TextField              campValor;
    @FXML private ComboBox<String>       comboTipoPagamento;
    @FXML private ComboBox<String>       comboFormaPagamento;
    @FXML private DatePicker             campDataPagamento;
    @FXML private TextArea               campObservacoes;

    // ── Tabela de pagamentos ──────────────────────────────────────────────

    @FXML private TableView<Pagamento>         tabelaPagamentos;
    @FXML private TableColumn<Pagamento, Integer> colId;
    @FXML private TableColumn<Pagamento, String>  colAluno;
    @FXML private TableColumn<Pagamento, Double>  colValor;
    @FXML private TableColumn<Pagamento, String>  colData;
    @FXML private TableColumn<Pagamento, String>  colTipo;
    @FXML private TableColumn<Pagamento, String>  colForma;
    @FXML private TableColumn<Pagamento, String>  colObs;

    // ── Aba de atrasados ─────────────────────────────────────────────────

    @FXML private TableView<PendenciaFinanceira> tabelaAtrasados;
    @FXML private TableColumn<PendenciaFinanceira, String> colAlunoAt;
    @FXML private TableColumn<PendenciaFinanceira, String> colTipoAt;
    @FXML private TableColumn<PendenciaFinanceira, Double> colValorAt;
    @FXML private TableColumn<PendenciaFinanceira, String> colVencimentoAt;
    @FXML private TableColumn<PendenciaFinanceira, String> colSituacaoAt;

    @FXML private Label labelStatus;

    private final PagamentoDAO  pagamentoDAO  = new PagamentoDAO();
    private final MatriculaDAO  matriculaDAO  = new MatriculaDAO();
    private final AlunoDAO      alunoDAO      = new AlunoDAO();
    private List<Matricula> matriculasAtivas = new ArrayList<>();
    private Map<Integer, String> cpfPorAluno = Map.of();
    private Matricula matriculaSelecionada;
    private boolean atualizandoBusca;
    private Integer pendenciaSelecionadaId;

    /**
     * Inicializa o controller: popula combos e tabelas.
     */
    @FXML
    public void initialize() {
        // Popula o combo de matrículas com as ativas
        carregarMatriculasAtivas();

        // Configura exibição do combo de matrículas
        cpfPorAluno = alunoDAO.listarTodos().stream()
                .collect(Collectors.toMap(Aluno::getId, Aluno::getCpf, (a, b) -> a));
        campoBuscaAluno.textProperty().addListener((obs, anterior, texto) -> filtrarAlunos(texto));
        listaResultadosAluno.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Matricula matricula, boolean empty) {
                super.updateItem(matricula, empty);
                setText(empty || matricula == null ? null : String.format(
                        "%s  ·  Matrícula #%d  ·  CPF %s", matricula.getNomeAluno(),
                        matricula.getId(), cpfPorAluno.getOrDefault(matricula.getAlunoId(), "não informado")));
            }
        });
        listaResultadosAluno.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, matricula) -> {
                    if (matricula != null) selecionarMatricula(matricula);
                });

        // Formas de pagamento disponíveis
        comboFormaPagamento.getItems().addAll("DINHEIRO", "CARTAO", "PIX", "BOLETO");
        comboFormaPagamento.getSelectionModel().selectFirst();
        comboTipoPagamento.getItems().addAll("MENSALIDADE", "OUTRO");
        comboTipoPagamento.getSelectionModel().selectFirst();

        // Data de pagamento padrão: hoje
        campDataPagamento.setValue(LocalDate.now());

        configurarColunasPagamentos();
        configurarColunasAtrasados();
        carregarPagamentos();
        carregarAtrasados();

        tabelaAtrasados.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, pendencia) -> { if (pendencia != null) preencherComPendencia(pendencia); });
    }

    /**
     * Registra o observer do painel principal (chamado pelo PainelPrincipalController).
     *
     * @param painel Observer a ser registrado.
     */
    public void setPainelObserver(Observer painel) {
        adicionarObserver(painel);
    }

    // ── Implementação do Subject (Padrão Observer) ────────────────────────

    @Override
    public void adicionarObserver(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removerObserver(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Notifica todos os observers registrados sobre um novo pagamento.
     *
     * @param evento Tipo do evento (ex.: "PAGAMENTO_REGISTRADO").
     * @param dado   Valor do pagamento registrado.
     */
    @Override
    public void notificarObservers(String evento, Object dado) {
        for (Observer obs : observers) {
            obs.atualizar(evento, dado);
        }
    }

    // ── Ações da interface ────────────────────────────────────────────────

    /** Ação do botão "Registrar Pagamento". */
    @FXML
    private void onRegistrarPagamentoClicado() {
        if (!validarCampos()) return;

        Pagamento pagamento = montarObjetoPagamento();

        Integer pendenciaId = "MENSALIDADE".equals(pagamento.getTipo()) ? pendenciaSelecionadaId : null;
        PagamentoDAO.ResultadoPagamento resultado = pagamentoDAO.registrarPagamento(pagamento, pendenciaId);

        if (resultado == PagamentoDAO.ResultadoPagamento.SUCESSO) {
            exibirStatus("✔ Pagamento de R$ " +
                    String.format("%.2f", pagamento.getValorPago()) + " registrado!");

            // ── Notifica os observers — Padrão Observer ────────────────────
            notificarObservers("PAGAMENTO_REGISTRADO", pagamento.getValorPago());

            limparFormulario();
            carregarPagamentos();
            carregarAtrasados();
        } else if (resultado == PagamentoDAO.ResultadoPagamento.PENDENCIA_INEXISTENTE) {
            exibirStatus("⚠ A pendência selecionada não existe para esta matrícula.");
        } else if (resultado == PagamentoDAO.ResultadoPagamento.PENDENCIA_JA_PAGA) {
            exibirStatus("⚠ Esta pendência já foi paga.");
        } else {
            exibirStatus("✗ Erro ao registrar pagamento.");
        }
    }

    /** Ação do botão "Atualizar" (recarrega as tabelas). */
    @FXML
    private void onAtualizarClicado() {
        carregarMatriculasAtivas();
        carregarPagamentos();
        carregarAtrasados();
        exibirStatus("Dados atualizados.");
    }

    /**
     * Recarrega todos os dados da aba.
     * Chamado pelo PainelPrincipalController quando a aba Pagamentos é selecionada.
     */
    public void refresh() {
        carregarMatriculasAtivas();
        carregarPagamentos();
        carregarAtrasados();
    }

    // ── Métodos auxiliares ────────────────────────────────────────────────

    private void carregarMatriculasAtivas() {
        List<Matricula> todas = matriculaDAO.listarTodas();
        matriculasAtivas = todas.stream().filter(Matricula::isAtiva).toList();
    }

    private void carregarPagamentos() {
        tabelaPagamentos.setItems(
                FXCollections.observableArrayList(pagamentoDAO.listarTodos())
        );
    }

    private void carregarAtrasados() {
        carregarPendencias(matriculaSelecionada == null ? null : matriculaSelecionada.getId());
    }

    private void filtrarAlunos(String texto) {
        if (atualizandoBusca) return;
        matriculaSelecionada = null;
        pendenciaSelecionadaId = null;
        tabelaAtrasados.getSelectionModel().clearSelection();
        carregarPendencias(null);

        String consulta = texto == null ? "" : texto.trim().toLowerCase();
        if (consulta.isEmpty()) {
            listaResultadosAluno.getItems().clear();
            listaResultadosAluno.setVisible(false);
            listaResultadosAluno.setManaged(false);
            return;
        }
        String digitos = consulta.replaceAll("\\D", "");
        List<Matricula> encontrados = matriculasAtivas.stream().filter(m -> {
            String nome = m.getNomeAluno() == null ? "" : m.getNomeAluno().toLowerCase();
            String id = Integer.toString(m.getId());
            String cpf = cpfPorAluno.getOrDefault(m.getAlunoId(), "");
            String cpfDigitos = cpf.replaceAll("\\D", "");
            return nome.contains(consulta) || id.contains(consulta)
                    || (!digitos.isEmpty() && cpfDigitos.contains(digitos));
        }).limit(8).toList();
        listaResultadosAluno.setItems(FXCollections.observableArrayList(encontrados));
        boolean mostrar = !encontrados.isEmpty();
        listaResultadosAluno.setVisible(mostrar);
        listaResultadosAluno.setManaged(mostrar);
        if (!mostrar) exibirStatus("Nenhum aluno com matrícula ativa corresponde à busca.");
    }

    private void selecionarMatricula(Matricula matricula) {
        boolean mudouMatricula = matriculaSelecionada == null
                || matriculaSelecionada.getId() != matricula.getId();
        matriculaSelecionada = matricula;
        atualizandoBusca = true;
        campoBuscaAluno.setText(String.format("%s · Matrícula #%d",
                matricula.getNomeAluno(), matricula.getId()));
        atualizandoBusca = false;
        listaResultadosAluno.setVisible(false);
        listaResultadosAluno.setManaged(false);
        if (mudouMatricula) {
            pendenciaSelecionadaId = null;
            carregarPendencias(matricula.getId());
        }
    }

    private void carregarPendencias(Integer matriculaId) {
        if (matriculaId == null) {
            tabelaAtrasados.setItems(FXCollections.observableArrayList());
            return;
        }
        List<PendenciaFinanceira> pendencias = pagamentoDAO.listarPendencias(matriculaId);
        tabelaAtrasados.setItems(FXCollections.observableArrayList(pendencias));
        if (matriculaId != null && pendencias.stream().noneMatch(p -> !"PAGA".equals(p.getSituacao()))) {
            exibirStatus("ℹ Este aluno não possui pagamentos pendentes.");
        }
    }

    private void configurarColunasPagamentos() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAluno.setCellValueFactory(new PropertyValueFactory<>("nomeAluno"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorPago"));
        colData.setCellValueFactory(new PropertyValueFactory<>("dataPagamento"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colForma.setCellValueFactory(new PropertyValueFactory<>("formaPagamento"));
        colObs.setCellValueFactory(new PropertyValueFactory<>("observacoes"));

        // Formata coluna de valor como moeda
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("R$ %.2f", item));
            }
        });
    }

    private void configurarColunasAtrasados() {
        colAlunoAt.setCellValueFactory(new PropertyValueFactory<>("nomeAluno"));
        colTipoAt.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colValorAt.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colVencimentoAt.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        colSituacaoAt.setCellValueFactory(new PropertyValueFactory<>("situacao"));
        colValorAt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("R$ %.2f", item));
            }
        });

        // Pendências não pagas ficam destacadas em vermelho; a seleção tem
        // um estado visual próprio para continuar evidente ao preparar a cobrança.
        PseudoClass pendente = PseudoClass.getPseudoClass("pendente");
        tabelaAtrasados.setRowFactory(tabela -> new TableRow<>() {
            private boolean emAberto;

            {
                selectedProperty().addListener((obs, antes, selecionada) -> atualizarEstilo());
            }

            @Override
            protected void updateItem(PendenciaFinanceira item, boolean empty) {
                super.updateItem(item, empty);
                emAberto = !empty && item != null
                        && !"PAGA".equalsIgnoreCase(item.getSituacao());
                pseudoClassStateChanged(pendente, emAberto);
                atualizarEstilo();
            }

            private void atualizarEstilo() {
                if (isSelected()) {
                    setStyle("-fx-background-color: #0F766E; -fx-border-color: #45D6A4; -fx-border-width: 0 0 2 0;");
                } else if (emAberto) {
                    setStyle("-fx-background-color: #351F2A; -fx-border-color: transparent transparent #59303A transparent; -fx-border-width: 0 0 1 0;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private boolean validarCampos() {
        if (matriculaSelecionada == null) {
            exibirStatus("⚠ Selecione uma matrícula.");
            return false;
        }
        try {
            double valor = Double.parseDouble(campValor.getText().replace(",", "."));
            String erro = PagamentoValidator.validar(valor, comboTipoPagamento.getValue(),
                    pendenciaSelecionadaId);
            if (erro != null) {
                exibirStatus("⚠ " + erro);
                return false;
            }
        } catch (NumberFormatException e) {
            exibirStatus("⚠ Valor inválido.");
            return false;
        }
        return true;
    }

    private Pagamento montarObjetoPagamento() {
        Pagamento p = new Pagamento();
        p.setMatriculaId(matriculaSelecionada.getId());
        p.setValorPago(Double.parseDouble(campValor.getText().replace(",", ".")));
        p.setDataPagamento(
                campDataPagamento.getValue() != null
                        ? campDataPagamento.getValue().toString()
                        : LocalDate.now().toString()
        );
        p.setTipo(comboTipoPagamento.getValue());
        p.setFormaPagamento(comboFormaPagamento.getValue());
        p.setObservacoes(campObservacoes.getText().trim());
        return p;
    }

    private void limparFormulario() {
        matriculaSelecionada = null;
        campoBuscaAluno.clear();
        campValor.clear();
        comboFormaPagamento.getSelectionModel().selectFirst();
        comboTipoPagamento.getSelectionModel().selectFirst();
        campDataPagamento.setValue(LocalDate.now());
        campObservacoes.clear();
        tabelaAtrasados.getSelectionModel().clearSelection();
        pendenciaSelecionadaId = null;
    }

    private void preencherComPendencia(PendenciaFinanceira pendencia) {
        matriculasAtivas.stream()
                .filter(m -> m.getId() == pendencia.getMatriculaId())
                .findFirst().ifPresent(this::selecionarMatricula);
        pendenciaSelecionadaId = pendencia.getId();
        comboTipoPagamento.setValue("MENSALIDADE");
        campValor.setText(String.format("%.2f", pendencia.getValor()));
        campObservacoes.setText(pendencia.getDescricao());
    }

    private void exibirStatus(String msg) { labelStatus.setText(msg); }
}
