package com.academia.controller;

import com.academia.dao.AlunoDAO;
import com.academia.dao.PlanoDAO;
import com.academia.dao.RelatorioDAO;
import com.academia.model.*;
import com.academia.util.RelatorioPdfService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller responsável pelo módulo de geração e exportação de Relatórios.
 */
public class RelatorioController {

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Controles de Seleção e Ações ───────────────────────────────────────
    @FXML private ComboBox<TipoRelatorio> comboTipoRelatorio;
    @FXML private Button btnFiltrar;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnLimparFiltros;

    // ── Painel de Filtros ──────────────────────────────────────────────────
    @FXML private VBox boxFiltros;
    @FXML private HBox boxPeriodo;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;

    @FXML private HBox boxFiltroPlano;
    @FXML private ComboBox<Plano> comboPlano;

    @FXML private HBox boxFiltroSituacao;
    @FXML private ComboBox<String> comboSituacao;

    @FXML private HBox boxFiltroFormaPagamento;
    @FXML private ComboBox<String> comboFormaPagamento;

    @FXML private HBox boxFiltroTipoPagamento;
    @FXML private ComboBox<String> comboTipoPagamento;

    @FXML private HBox boxFiltroAluno;
    @FXML private ComboBox<Aluno> comboAluno;

    @FXML private TextField campBuscaTexto;

    // ── Cards KPI ─────────────────────────────────────────────────────────
    @FXML private Label labelKpi1Titulo;
    @FXML private Label labelKpi1Valor;
    @FXML private Label labelKpi2Titulo;
    @FXML private Label labelKpi2Valor;
    @FXML private Label labelKpi3Titulo;
    @FXML private Label labelKpi3Valor;
    @FXML private Label labelKpi4Titulo;
    @FXML private Label labelKpi4Valor;

    // ── Tabelas ───────────────────────────────────────────────────────────
    // 1. Alunos Ativos
    @FXML private TableView<RelatorioAlunoAtivoDTO> tabelaAlunosAtivos;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, Integer> colAlunoAtivoId;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoNome;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoCpf;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoTelefone;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoEmail;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoPlano;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoInicio;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoFim;
    @FXML private TableColumn<RelatorioAlunoAtivoDTO, String> colAlunoAtivoDias;

    // 2. Matrículas
    @FXML private TableView<RelatorioMatriculaDTO> tabelaMatriculas;
    @FXML private TableColumn<RelatorioMatriculaDTO, Integer> colMatriculaId;
    @FXML private TableColumn<RelatorioMatriculaDTO, String> colMatriculaAluno;
    @FXML private TableColumn<RelatorioMatriculaDTO, String> colMatriculaCpf;
    @FXML private TableColumn<RelatorioMatriculaDTO, String> colMatriculaPlano;
    @FXML private TableColumn<RelatorioMatriculaDTO, String> colMatriculaValor;
    @FXML private TableColumn<RelatorioMatriculaDTO, String> colMatriculaInicio;
    @FXML private TableColumn<RelatorioMatriculaDTO, String> colMatriculaFim;
    @FXML private TableColumn<RelatorioMatriculaDTO, String> colMatriculaSituacao;

    // 3. Pagamentos
    @FXML private TableView<RelatorioPagamentoDTO> tabelaPagamentos;
    @FXML private TableColumn<RelatorioPagamentoDTO, Integer> colPagamentoId;
    @FXML private TableColumn<RelatorioPagamentoDTO, Integer> colPagamentoMatricula;
    @FXML private TableColumn<RelatorioPagamentoDTO, String> colPagamentoAluno;
    @FXML private TableColumn<RelatorioPagamentoDTO, String> colPagamentoData;
    @FXML private TableColumn<RelatorioPagamentoDTO, String> colPagamentoTipo;
    @FXML private TableColumn<RelatorioPagamentoDTO, String> colPagamentoForma;
    @FXML private TableColumn<RelatorioPagamentoDTO, String> colPagamentoValor;
    @FXML private TableColumn<RelatorioPagamentoDTO, String> colPagamentoObs;

    // 4. Frequência
    @FXML private TableView<RelatorioFrequenciaDTO> tabelaFrequencia;
    @FXML private TableColumn<RelatorioFrequenciaDTO, Integer> colFreqId;
    @FXML private TableColumn<RelatorioFrequenciaDTO, String> colFreqDataHora;
    @FXML private TableColumn<RelatorioFrequenciaDTO, String> colFreqAluno;
    @FXML private TableColumn<RelatorioFrequenciaDTO, String> colFreqCpf;
    @FXML private TableColumn<RelatorioFrequenciaDTO, String> colFreqMatPlano;
    @FXML private TableColumn<RelatorioFrequenciaDTO, String> colFreqStatus;

    // 5. Avaliações
    @FXML private TableView<RelatorioAvaliacaoDTO> tabelaAvaliacoes;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, Integer> colAvId;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvData;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvAluno;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvInstrutor;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvPeso;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvAltura;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvImc;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvClassificacao;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvGordura;
    @FXML private TableColumn<RelatorioAvaliacaoDTO, String> colAvMassa;

    @FXML private Label labelStatus;

    // ── DAOs e Estado ─────────────────────────────────────────────────────
    private final RelatorioDAO relatorioDAO = new RelatorioDAO();
    private final PlanoDAO planoDAO = new PlanoDAO();
    private final AlunoDAO alunoDAO = new AlunoDAO();

    private Usuario usuarioLogado;

    // Cache dos dados atualmente carregados na tela
    private List<RelatorioAlunoAtivoDTO> cacheAlunosAtivos = new ArrayList<>();
    private List<RelatorioMatriculaDTO> cacheMatriculas = new ArrayList<>();
    private List<RelatorioPagamentoDTO> cachePagamentos = new ArrayList<>();
    private List<RelatorioFrequenciaDTO> cacheFrequencias = new ArrayList<>();
    private List<RelatorioAvaliacaoDTO> cacheAvaliacoes = new ArrayList<>();

    private Plano planoTodos;
    private Aluno alunoTodos;

    @FXML
    public void initialize() {
        configurarColunas();
        configurarFiltros();
        configurarEventos();

        // Inicializa combos
        carregarPlanos();
        carregarAlunos();

        // Seleciona o primeiro tipo de relatório por padrão
        comboTipoRelatorio.getSelectionModel().select(TipoRelatorio.ALUNOS_ATIVOS);
    }

    public void setUsuarioLogado(Usuario usuario) {
        this.usuarioLogado = usuario;
        if (usuario != null && usuario.isInstrutor()) {
            // Se for instrutor, podemos sugerir Avaliações Físicas
            if (comboTipoRelatorio.getValue() == null) {
                comboTipoRelatorio.getSelectionModel().select(TipoRelatorio.AVALIACOES_FISICAS);
            }
        }
    }

    public void refresh() {
        carregarPlanos();
        carregarAlunos();
        executarConsulta();
    }

    private void configurarColunas() {
        // 1. Alunos Ativos
        colAlunoAtivoId.setCellValueFactory(new PropertyValueFactory<>("alunoId"));
        colAlunoAtivoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colAlunoAtivoCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colAlunoAtivoTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colAlunoAtivoEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colAlunoAtivoPlano.setCellValueFactory(new PropertyValueFactory<>("planoNome"));
        colAlunoAtivoInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicioFormatada"));
        colAlunoAtivoFim.setCellValueFactory(new PropertyValueFactory<>("dataFimFormatada"));
        colAlunoAtivoDias.setCellValueFactory(new PropertyValueFactory<>("diasRestantesTexto"));

        // 2. Matrículas
        colMatriculaId.setCellValueFactory(new PropertyValueFactory<>("matriculaId"));
        colMatriculaAluno.setCellValueFactory(new PropertyValueFactory<>("alunoNome"));
        colMatriculaCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colMatriculaPlano.setCellValueFactory(new PropertyValueFactory<>("planoNome"));
        colMatriculaValor.setCellValueFactory(new PropertyValueFactory<>("valorFormatado"));
        colMatriculaInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicioFormatada"));
        colMatriculaFim.setCellValueFactory(new PropertyValueFactory<>("dataFimFormatada"));
        colMatriculaSituacao.setCellValueFactory(new PropertyValueFactory<>("situacao"));

        // 3. Pagamentos
        colPagamentoId.setCellValueFactory(new PropertyValueFactory<>("pagamentoId"));
        colPagamentoMatricula.setCellValueFactory(new PropertyValueFactory<>("matriculaId"));
        colPagamentoAluno.setCellValueFactory(new PropertyValueFactory<>("alunoNome"));
        colPagamentoData.setCellValueFactory(new PropertyValueFactory<>("dataPagamentoFormatada"));
        colPagamentoTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colPagamentoForma.setCellValueFactory(new PropertyValueFactory<>("formaPagamento"));
        colPagamentoValor.setCellValueFactory(new PropertyValueFactory<>("valorFormatado"));
        colPagamentoObs.setCellValueFactory(new PropertyValueFactory<>("observacoes"));

        // 4. Frequência
        colFreqId.setCellValueFactory(new PropertyValueFactory<>("frequenciaId"));
        colFreqDataHora.setCellValueFactory(new PropertyValueFactory<>("dataHoraEntradaFormatada"));
        colFreqAluno.setCellValueFactory(new PropertyValueFactory<>("alunoNome"));
        colFreqCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colFreqMatPlano.setCellValueFactory(new PropertyValueFactory<>("matriculaPlano"));
        colFreqStatus.setCellValueFactory(new PropertyValueFactory<>("statusAcesso"));

        // 5. Avaliações
        colAvId.setCellValueFactory(new PropertyValueFactory<>("avaliacaoId"));
        colAvData.setCellValueFactory(new PropertyValueFactory<>("dataAvaliacaoFormatada"));
        colAvAluno.setCellValueFactory(new PropertyValueFactory<>("alunoNome"));
        colAvInstrutor.setCellValueFactory(new PropertyValueFactory<>("instrutorNome"));
        colAvPeso.setCellValueFactory(new PropertyValueFactory<>("pesoFormatado"));
        colAvAltura.setCellValueFactory(new PropertyValueFactory<>("alturaFormatada"));
        colAvImc.setCellValueFactory(new PropertyValueFactory<>("imcFormatado"));
        colAvClassificacao.setCellValueFactory(new PropertyValueFactory<>("classificacaoImc"));
        colAvGordura.setCellValueFactory(new PropertyValueFactory<>("gorduraFormatada"));
        colAvMassa.setCellValueFactory(new PropertyValueFactory<>("massaFormatada"));
    }

    private void configurarFiltros() {
        comboTipoRelatorio.setItems(FXCollections.observableArrayList(TipoRelatorio.values()));

        comboSituacao.setItems(FXCollections.observableArrayList("Todas", "Ativas", "Vencidas", "Canceladas"));
        comboSituacao.getSelectionModel().selectFirst();

        comboFormaPagamento.setItems(FXCollections.observableArrayList("Todas", "DINHEIRO", "CARTAO", "PIX"));
        comboFormaPagamento.getSelectionModel().selectFirst();

        comboTipoPagamento.setItems(FXCollections.observableArrayList("Todos", "MENSALIDADE", "OUTRO"));
        comboTipoPagamento.getSelectionModel().selectFirst();
    }

    private void configurarEventos() {
        // Ao trocar o tipo de relatório
        comboTipoRelatorio.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ajustarLayoutFiltros(newVal);
                alternarTabelaVisivel(newVal);
                executarConsulta();
            }
        });

        // Pressionar ENTER no campo de busca executa filtro
        campBuscaTexto.setOnAction(e -> onFiltrarClicado());
    }

    private void ajustarLayoutFiltros(TipoRelatorio tipo) {
        boolean usaPeriodo = (tipo != TipoRelatorio.ALUNOS_ATIVOS);
        boolean usaPlano = (tipo == TipoRelatorio.ALUNOS_ATIVOS || tipo == TipoRelatorio.MATRICULAS);
        boolean usaSituacao = (tipo == TipoRelatorio.MATRICULAS);
        boolean usaFormaPagto = (tipo == TipoRelatorio.PAGAMENTOS);
        boolean usaTipoPagto = (tipo == TipoRelatorio.PAGAMENTOS);
        boolean usaAluno = (tipo == TipoRelatorio.AVALIACOES_FISICAS);

        boxPeriodo.setVisible(usaPeriodo);
        boxPeriodo.setManaged(usaPeriodo);

        boxFiltroPlano.setVisible(usaPlano);
        boxFiltroPlano.setManaged(usaPlano);

        boxFiltroSituacao.setVisible(usaSituacao);
        boxFiltroSituacao.setManaged(usaSituacao);

        boxFiltroFormaPagamento.setVisible(usaFormaPagto);
        boxFiltroFormaPagamento.setManaged(usaFormaPagto);

        boxFiltroTipoPagamento.setVisible(usaTipoPagto);
        boxFiltroTipoPagamento.setManaged(usaTipoPagto);

        boxFiltroAluno.setVisible(usaAluno);
        boxFiltroAluno.setManaged(usaAluno);

        // Se o tipo usa período e as datas estiverem vazias, define default para Este Mês
        if (usaPeriodo && dpInicio.getValue() == null && dpFim.getValue() == null) {
            LocalDate hoje = LocalDate.now();
            dpInicio.setValue(hoje.withDayOfMonth(1));
            dpFim.setValue(hoje);
        }
    }

    private void alternarTabelaVisivel(TipoRelatorio tipo) {
        tabelaAlunosAtivos.setVisible(tipo == TipoRelatorio.ALUNOS_ATIVOS);
        tabelaMatriculas.setVisible(tipo == TipoRelatorio.MATRICULAS);
        tabelaPagamentos.setVisible(tipo == TipoRelatorio.PAGAMENTOS);
        tabelaFrequencia.setVisible(tipo == TipoRelatorio.FREQUENCIA);
        tabelaAvaliacoes.setVisible(tipo == TipoRelatorio.AVALIACOES_FISICAS);
    }

    private void carregarPlanos() {
        List<Plano> planos = planoDAO.listarTodos();
        planoTodos = new Plano(0, "Todos os Planos", "", "", 0, 0);

        ObservableList<Plano> itens = FXCollections.observableArrayList();
        itens.add(planoTodos);
        itens.addAll(planos);
        comboPlano.setItems(itens);
        comboPlano.getSelectionModel().selectFirst();
    }

    private void carregarAlunos() {
        List<Aluno> alunos = alunoDAO.listarTodos();
        alunoTodos = new Aluno();
        alunoTodos.setId(0);
        alunoTodos.setNome("Todos os Alunos");

        ObservableList<Aluno> itens = FXCollections.observableArrayList();
        itens.add(alunoTodos);
        itens.addAll(alunos);
        comboAluno.setItems(itens);
        comboAluno.getSelectionModel().selectFirst();
    }

    // ── Execução de Consultas ─────────────────────────────────────────────────

    @FXML
    public void onFiltrarClicado() {
        executarConsulta();
    }

    @FXML
    public void onLimparFiltrosClicado() {
        dpInicio.setValue(null);
        dpFim.setValue(null);
        campBuscaTexto.clear();
        comboPlano.getSelectionModel().selectFirst();
        comboSituacao.getSelectionModel().selectFirst();
        comboFormaPagamento.getSelectionModel().selectFirst();
        comboTipoPagamento.getSelectionModel().selectFirst();
        comboAluno.getSelectionModel().selectFirst();

        executarConsulta();
    }

    private void executarConsulta() {
        TipoRelatorio tipo = comboTipoRelatorio.getValue();
        if (tipo == null) return;

        String filtroTexto = campBuscaTexto.getText();
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();

        Plano planoSel = comboPlano.getValue();
        Integer planoId = (planoSel != null && planoSel.getId() > 0) ? planoSel.getId() : null;

        switch (tipo) {
            case ALUNOS_ATIVOS -> {
                cacheAlunosAtivos = relatorioDAO.listarAlunosAtivos(filtroTexto, planoId);
                tabelaAlunosAtivos.setItems(FXCollections.observableArrayList(cacheAlunosAtivos));
                atualizarKpisAlunosAtivos(cacheAlunosAtivos);
                labelStatus.setText(cacheAlunosAtivos.size() + " alunos ativos encontrados.");
            }
            case MATRICULAS -> {
                String situacao = comboSituacao.getValue();
                cacheMatriculas = relatorioDAO.listarMatriculas(inicio, fim, situacao, planoId, filtroTexto);
                tabelaMatriculas.setItems(FXCollections.observableArrayList(cacheMatriculas));
                atualizarKpisMatriculas(cacheMatriculas);
                labelStatus.setText(cacheMatriculas.size() + " matrículas encontradas no período.");
            }
            case PAGAMENTOS -> {
                String forma = comboFormaPagamento.getValue();
                String tipoPagto = comboTipoPagamento.getValue();
                cachePagamentos = relatorioDAO.listarPagamentos(inicio, fim, forma, tipoPagto, filtroTexto);
                tabelaPagamentos.setItems(FXCollections.observableArrayList(cachePagamentos));
                atualizarKpisPagamentos(cachePagamentos);
                labelStatus.setText(cachePagamentos.size() + " pagamentos encontrados no período.");
            }
            case FREQUENCIA -> {
                cacheFrequencias = relatorioDAO.listarFrequencias(inicio, fim, filtroTexto);
                tabelaFrequencia.setItems(FXCollections.observableArrayList(cacheFrequencias));
                atualizarKpisFrequencia(cacheFrequencias);
                labelStatus.setText(cacheFrequencias.size() + " registros de frequência encontrados.");
            }
            case AVALIACOES_FISICAS -> {
                Aluno alunoSel = comboAluno.getValue();
                Integer alunoId = (alunoSel != null && alunoSel.getId() > 0) ? alunoSel.getId() : null;
                cacheAvaliacoes = relatorioDAO.listarAvaliacoes(inicio, fim, alunoId, filtroTexto);
                tabelaAvaliacoes.setItems(FXCollections.observableArrayList(cacheAvaliacoes));
                atualizarKpisAvaliacoes(cacheAvaliacoes);
                labelStatus.setText(cacheAvaliacoes.size() + " avaliações físicas encontradas.");
            }
        }
    }

    // ── Atualização dos Cards KPI ─────────────────────────────────────────────

    private void atualizarKpisAlunosAtivos(List<RelatorioAlunoAtivoDTO> lista) {
        labelKpi1Titulo.setText("TOTAL ALUNOS ATIVOS");
        labelKpi1Valor.setText(String.valueOf(lista.size()));

        // Plano mais comum
        Map<String, Long> planosCount = new HashMap<>();
        for (RelatorioAlunoAtivoDTO a : lista) {
            planosCount.merge(a.getPlanoNome() != null ? a.getPlanoNome() : "Outro", 1L, Long::sum);
        }
        String topPlano = planosCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");
        labelKpi2Titulo.setText("PLANO MAIS POPULAR");
        labelKpi2Valor.setText(topPlano);

        // Vencendo nos próximos 7 dias
        long vencendo7Dias = lista.stream().filter(a -> a.getDiasRestantes() >= 0 && a.getDiasRestantes() <= 7).count();
        labelKpi3Titulo.setText("VENCENDO EM ATÉ 7 DIAS");
        labelKpi3Valor.setText(String.valueOf(vencendo7Dias));

        // Média de dias restantes
        double mediaDias = lista.stream().mapToInt(RelatorioAlunoAtivoDTO::getDiasRestantes).average().orElse(0.0);
        labelKpi4Titulo.setText("MÉDIA DIAS RESTANTES");
        labelKpi4Valor.setText(mediaDias > 0 ? String.format("%.0f dias", mediaDias) : "—");
    }

    private void atualizarKpisMatriculas(List<RelatorioMatriculaDTO> lista) {
        labelKpi1Titulo.setText("TOTAL DE MATRÍCULAS");
        labelKpi1Valor.setText(String.valueOf(lista.size()));

        long ativas = lista.stream().filter(m -> "Ativa".equalsIgnoreCase(m.getSituacao())).count();
        labelKpi2Titulo.setText("MATRÍCULAS ATIVAS");
        labelKpi2Valor.setText(String.valueOf(ativas));

        long encerradas = lista.stream().filter(m -> !"Ativa".equalsIgnoreCase(m.getSituacao())).count();
        labelKpi3Titulo.setText("VENCIDAS / CANCELADAS");
        labelKpi3Valor.setText(String.valueOf(encerradas));

        double totalValor = lista.stream().mapToDouble(RelatorioMatriculaDTO::getValor).sum();
        labelKpi4Titulo.setText("VALOR TOTAL CONTRATADO");
        labelKpi4Valor.setText(MOEDA.format(totalValor));
    }

    private void atualizarKpisPagamentos(List<RelatorioPagamentoDTO> lista) {
        double totalArrecadado = lista.stream().mapToDouble(RelatorioPagamentoDTO::getValorPago).sum();
        labelKpi1Titulo.setText("TOTAL ARRECADADO");
        labelKpi1Valor.setText(MOEDA.format(totalArrecadado));

        labelKpi2Titulo.setText("TOTAL DE TRANSAÇÕES");
        labelKpi2Valor.setText(String.valueOf(lista.size()));

        double totalPix = lista.stream().filter(p -> "PIX".equalsIgnoreCase(p.getFormaPagamento()))
                .mapToDouble(RelatorioPagamentoDTO::getValorPago).sum();
        labelKpi3Titulo.setText("RECEBIDO VIA PIX");
        labelKpi3Valor.setText(MOEDA.format(totalPix));

        double totalCartao = lista.stream().filter(p -> "CARTAO".equalsIgnoreCase(p.getFormaPagamento()) || "CARTÃO".equalsIgnoreCase(p.getFormaPagamento()))
                .mapToDouble(RelatorioPagamentoDTO::getValorPago).sum();
        labelKpi4Titulo.setText("RECEBIDO VIA CARTÃO");
        labelKpi4Valor.setText(MOEDA.format(totalCartao));
    }

    private void atualizarKpisFrequencia(List<RelatorioFrequenciaDTO> lista) {
        labelKpi1Titulo.setText("TOTAL DE ACESSOS");
        labelKpi1Valor.setText(String.valueOf(lista.size()));

        long alunosUnicos = lista.stream().map(RelatorioFrequenciaDTO::getAlunoNome).distinct().count();
        labelKpi2Titulo.setText("ALUNOS ÚNICOS");
        labelKpi2Valor.setText(String.valueOf(alunosUnicos));

        // Dias únicos
        long diasUnicos = lista.stream()
                .map(f -> f.getDataHoraEntrada() != null && f.getDataHoraEntrada().length() >= 10
                        ? f.getDataHoraEntrada().substring(0, 10) : "")
                .filter(d -> !d.isBlank()).distinct().count();
        double mediaDia = diasUnicos > 0 ? (double) lista.size() / diasUnicos : (double) lista.size();

        labelKpi3Titulo.setText("MÉDIA DIÁRIA DE ACESSOS");
        labelKpi3Valor.setText(String.format("%.1f / dia", mediaDia));

        long liberados = lista.stream().filter(f -> "Liberado".equalsIgnoreCase(f.getStatusAcesso())).count();
        labelKpi4Titulo.setText("STATUS LIBERADO");
        labelKpi4Valor.setText(String.valueOf(liberados));
    }

    private void atualizarKpisAvaliacoes(List<RelatorioAvaliacaoDTO> lista) {
        labelKpi1Titulo.setText("TOTAL DE AVALIAÇÕES");
        labelKpi1Valor.setText(String.valueOf(lista.size()));

        double mediaImc = lista.stream()
                .filter(a -> a.getImc() != null && a.getImc() > 0)
                .mapToDouble(RelatorioAvaliacaoDTO::getImc).average().orElse(0.0);
        labelKpi2Titulo.setText("IMC MÉDIO");
        labelKpi2Valor.setText(mediaImc > 0 ? String.format("%.2f", mediaImc) : "—");

        double mediaPeso = lista.stream()
                .filter(a -> a.getPesoKg() != null && a.getPesoKg() > 0)
                .mapToDouble(RelatorioAvaliacaoDTO::getPesoKg).average().orElse(0.0);
        labelKpi3Titulo.setText("PESO MÉDIO");
        labelKpi3Valor.setText(mediaPeso > 0 ? String.format("%.1f kg", mediaPeso) : "—");

        long alunosAvaliados = lista.stream().map(RelatorioAvaliacaoDTO::getAlunoNome).distinct().count();
        labelKpi4Titulo.setText("ALUNOS AVALIADOS");
        labelKpi4Valor.setText(String.valueOf(alunosAvaliados));
    }

    // ── Atalhos de Período ────────────────────────────────────────────────────

    @FXML
    public void onAtalhoHoje() {
        LocalDate hoje = LocalDate.now();
        dpInicio.setValue(hoje);
        dpFim.setValue(hoje);
        executarConsulta();
    }

    @FXML
    public void onAtalho7Dias() {
        LocalDate hoje = LocalDate.now();
        dpInicio.setValue(hoje.minusDays(7));
        dpFim.setValue(hoje);
        executarConsulta();
    }

    @FXML
    public void onAtalhoEsteMes() {
        LocalDate hoje = LocalDate.now();
        dpInicio.setValue(hoje.withDayOfMonth(1));
        dpFim.setValue(hoje);
        executarConsulta();
    }

    // ── Exportação em PDF ─────────────────────────────────────────────────────

    @FXML
    public void onExportarPdfClicado() {
        TipoRelatorio tipo = comboTipoRelatorio.getValue();
        if (tipo == null) return;

        boolean vazio = switch (tipo) {
            case ALUNOS_ATIVOS -> cacheAlunosAtivos.isEmpty();
            case MATRICULAS -> cacheMatriculas.isEmpty();
            case PAGAMENTOS -> cachePagamentos.isEmpty();
            case FREQUENCIA -> cacheFrequencias.isEmpty();
            case AVALIACOES_FISICAS -> cacheAvaliacoes.isEmpty();
        };

        if (vazio) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Não há registros correspondentes aos filtros para exportar.");
            alert.setTitle("Atenção");
            alert.setHeaderText("Nenhum Dado Encontrado");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Relatório em PDF");
        String nomeBase = switch (tipo) {
            case ALUNOS_ATIVOS -> "Relatorio_Alunos_Ativos";
            case MATRICULAS -> "Relatorio_Matriculas";
            case PAGAMENTOS -> "Relatorio_Pagamentos";
            case FREQUENCIA -> "Relatorio_Frequencia";
            case AVALIACOES_FISICAS -> "Relatorio_Avaliacoes_Fisicas";
        };
        fileChooser.setInitialFileName(nomeBase + "_" + LocalDate.now() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos PDF (*.pdf)", "*.pdf"));

        File file = fileChooser.showSaveDialog(btnExportarPdf.getScene().getWindow());
        if (file == null) return;

        String descricaoFiltros = montarDescricaoFiltros(tipo);

        try {
            switch (tipo) {
                case ALUNOS_ATIVOS -> RelatorioPdfService.gerarPdfAlunosAtivos(cacheAlunosAtivos, descricaoFiltros, file);
                case MATRICULAS -> RelatorioPdfService.gerarPdfMatriculas(cacheMatriculas, descricaoFiltros, file);
                case PAGAMENTOS -> RelatorioPdfService.gerarPdfPagamentos(cachePagamentos, descricaoFiltros, file);
                case FREQUENCIA -> RelatorioPdfService.gerarPdfFrequencia(cacheFrequencias, descricaoFiltros, file);
                case AVALIACOES_FISICAS -> RelatorioPdfService.gerarPdfAvaliacoes(cacheAvaliacoes, descricaoFiltros, file);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Relatório gerado com sucesso em:\n" + file.getAbsolutePath());
            alert.setTitle("Exportação Concluída");
            alert.setHeaderText("Sucesso!");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao exportar PDF: " + e.getMessage());
            alert.setTitle("Erro");
            alert.setHeaderText("Falha na geração do relatório");
            alert.showAndWait();
        }
    }

    private String montarDescricaoFiltros(TipoRelatorio tipo) {
        List<String> partes = new ArrayList<>();

        if (dpInicio.getValue() != null && dpFim.getValue() != null) {
            partes.add("Período: " + dpInicio.getValue().format(FMT_DATA) + " a " + dpFim.getValue().format(FMT_DATA));
        } else if (dpInicio.getValue() != null) {
            partes.add("A partir de: " + dpInicio.getValue().format(FMT_DATA));
        } else if (dpFim.getValue() != null) {
            partes.add("Até: " + dpFim.getValue().format(FMT_DATA));
        }

        if (comboPlano.getValue() != null && comboPlano.getValue().getId() > 0) {
            partes.add("Plano: " + comboPlano.getValue().getNome());
        }

        if (tipo == TipoRelatorio.MATRICULAS && comboSituacao.getValue() != null && !comboSituacao.getValue().equalsIgnoreCase("Todas")) {
            partes.add("Situação: " + comboSituacao.getValue());
        }

        if (tipo == TipoRelatorio.PAGAMENTOS) {
            if (comboFormaPagamento.getValue() != null && !comboFormaPagamento.getValue().equalsIgnoreCase("Todas")) {
                partes.add("Forma: " + comboFormaPagamento.getValue());
            }
            if (comboTipoPagamento.getValue() != null && !comboTipoPagamento.getValue().equalsIgnoreCase("Todos")) {
                partes.add("Tipo: " + comboTipoPagamento.getValue());
            }
        }

        if (tipo == TipoRelatorio.AVALIACOES_FISICAS && comboAluno.getValue() != null && comboAluno.getValue().getId() > 0) {
            partes.add("Aluno: " + comboAluno.getValue().getNome());
        }

        if (campBuscaTexto.getText() != null && !campBuscaTexto.getText().isBlank()) {
            partes.add("Busca: \"" + campBuscaTexto.getText().trim() + "\"");
        }

        return partes.isEmpty() ? "Todos os registros (sem restrições)" : String.join(" | ", partes);
    }
}
