package com.academia.controller;

import com.academia.dao.AlunoDAO;
import com.academia.dao.MatriculaDAO;
import com.academia.dao.PlanoDAO;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.Plano;
import com.academia.validation.MatriculaValidator;
import com.academia.util.AlunoSearchSupport;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller: Realizar Matrícula — UC 03.
 *
 * <p>Exibe os planos disponíveis como cards visuais interativos (tema escuro / neon azul)
 * e permite efetivar a matrícula de um aluno, calculando automaticamente a data de fim.</p>
 */
public class MatriculaController {

    // ── Painel de cards ───────────────────────────────────────────────────

    @FXML private HBox painelCards;

    // ── Formulário ────────────────────────────────────────────────────────

    @FXML private TextField campoBuscaAluno;
    @FXML private ListView<Aluno> listaResultadosAluno;
    @FXML private DatePicker      campDataInicio;
    @FXML private Label           labelDataFim;
    @FXML private Label           labelPlanoSelecionado;
    @FXML private Label           labelStatus;

    // ── Estado ────────────────────────────────────────────────────────────

    /** Plano escolhido pelo usuário via clique no card. */
    private Plano planoSelecionado = null;

    private final MatriculaDAO matriculaDAO = new MatriculaDAO();
    private final AlunoDAO     alunoDAO     = new AlunoDAO();
    private final PlanoDAO     planoDAO     = new PlanoDAO();
    private AlunoSearchSupport buscaAluno;
    private Aluno alunoSelecionado;

    // ── Estilos CSS inline ────────────────────────────────────────────────

    private static final String CARD_BASE =
            "-fx-background-color: #161b22;" +
            "-fx-border-color: #30363d;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-pref-width: 190;" +
            "-fx-pref-height: 205;" +
            "-fx-cursor: hand;";

    private static final String CARD_SELECTED =
            "-fx-background-color: #0d1f38;" +
            "-fx-border-color: #1f6feb;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-pref-width: 190;" +
            "-fx-pref-height: 205;" +
            "-fx-cursor: hand;";

    private static final String BTN_NORMAL =
            "-fx-background-color: transparent;" +
            "-fx-border-color: #1f6feb;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-text-fill: #58a6ff;" +
            "-fx-font-size: 11;" +
            "-fx-pref-width: 140;" +
            "-fx-cursor: hand;";

    private static final String BTN_SELECTED =
            "-fx-background-color: #1f6feb;" +
            "-fx-border-color: #1f6feb;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 11;" +
            "-fx-pref-width: 140;" +
            "-fx-cursor: hand;";

    // ── Inicialização ─────────────────────────────────────────────────────

    /**
     * Inicializa o controller: carrega cards de planos e popula combo de alunos.
     */
    @FXML
    public void initialize() {
        buscaAluno = new AlunoSearchSupport(campoBuscaAluno, listaResultadosAluno,
                aluno -> alunoSelecionado = aluno);
        buscaAluno.setAlunos(alunoDAO.listarTodos());

        campDataInicio.valueProperty().addListener((obs, ant, sel) -> calcularDataFim());
        campDataInicio.setValue(LocalDate.now());

        gerarCards(planoDAO.listarTodos());
    }

    // ── Geração dos Cards ─────────────────────────────────────────────────

    /**
     * Gera dinamicamente um card visual para cada plano disponível no banco.
     * Cada card exibe categoria, período, preço/mês, economia e features.
     *
     * @param planos Lista de planos recuperados do banco de dados.
     */
    private void gerarCards(List<Plano> planos) {
        painelCards.getChildren().clear();

        for (Plano plano : planos) {
            VBox card = criarCard(plano);
            painelCards.getChildren().add(card);
        }
    }

    /**
     * Constrói o VBox de um card para o plano informado.
     *
     * @param plano Plano a ser representado.
     * @return VBox estilizado como card interativo.
     */
    private VBox criarCard(Plano plano) {
        VBox card = new VBox(6);
        card.setStyle(CARD_BASE);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.TOP_LEFT);

        // ── Linha superior: categoria + ícone/badge ──────────────────────
        HBox topoCard = new HBox();
        topoCard.setAlignment(Pos.CENTER_LEFT);

        Label lblCategoria = new Label(resolverCategoria(plano).toUpperCase());
        lblCategoria.setStyle(
                "-fx-text-fill: " + resolverCorCategoria(plano) + ";" +
                "-fx-font-size: 10;" +
                "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblBadge = criarBadge(plano);
        topoCard.getChildren().addAll(lblCategoria, spacer, lblBadge);

        // ── Período ──────────────────────────────────────────────────────
        Label lblPeriodo = new Label(plano.getNome());
        lblPeriodo.setWrapText(true);
        lblPeriodo.setMaxWidth(Double.MAX_VALUE);
        lblPeriodo.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");

        // ── Preço por mês ────────────────────────────────────────────────
        double precoMensal = calcularPrecoMensal(plano);
        HBox hboxPreco = new HBox(2);
        hboxPreco.setAlignment(Pos.BASELINE_LEFT);

        Label lblValor = new Label(String.format("R$ %.0f", precoMensal));
        lblValor.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold;");

        Label lblPorMes = new Label(" /mês");
        lblPorMes.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11;");

        hboxPreco.getChildren().addAll(lblValor, lblPorMes);

        // ── Economia ─────────────────────────────────────────────────────
        Label lblEconomia = criarLabelEconomia(plano, precoMensal);

        // ── Features ─────────────────────────────────────────────────────
        VBox features = criarFeatures(plano);
        // Sem vgrow — deixa o botão sempre visível na base do card

        // ── Botão selecionar ─────────────────────────────────────────────
        Button btnSelecionar = new Button("Selecionar");
        btnSelecionar.setStyle(BTN_NORMAL);
        btnSelecionar.setMaxWidth(Double.MAX_VALUE);
        btnSelecionar.setOnAction(e -> selecionarPlano(plano, card, btnSelecionar));

        card.getChildren().addAll(topoCard, lblPeriodo, hboxPreco, lblEconomia, features, btnSelecionar);
        card.setOnMouseClicked(e -> selecionarPlano(plano, card, btnSelecionar));
        return card;
    }

    /** Constrói o VBox de features (checkmarks) com base na descrição do plano. */
    private VBox criarFeatures(Plano plano) {
        VBox features = new VBox(4);
        features.setPadding(new Insets(4, 0, 4, 0));

        if (plano.getDescricao() != null && !plano.getDescricao().isBlank()) {
            for (String item : plano.getDescricao().split("\\|")) {
                HBox linha = new HBox(5);
                linha.setAlignment(Pos.CENTER_LEFT);

                Label check = new Label("✔");
                check.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 11;");

                Label texto = new Label(item.trim());
                texto.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 11;");
                texto.setWrapText(true);
                texto.setTextAlignment(TextAlignment.LEFT);

                linha.getChildren().addAll(check, texto);
                features.getChildren().add(linha);
            }
        }
        return features;
    }

    /** Cria o badge ou ícone exibido no canto superior direito do card. */
    private Label criarBadge(Plano plano) {
        String periodo = resolverPeriodo(plano);

        if ("Trimestral".equals(periodo)) {
            Label badge = new Label("POPULAR");
            badge.setStyle(
                    "-fx-background-color: #1f6feb;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 10;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 3 8 3 8;" +
                    "-fx-background-radius: 10;"
            );
            return badge;
        } else if ("Anual".equals(periodo)) {
            Label ico = new Label("∞");
            ico.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 18; -fx-font-weight: bold;");
            return ico;
        } else {
            Label ico = new Label("📅");
            ico.setStyle("-fx-font-size: 16;");
            return ico;
        }
    }

    /** Cria label de economia (oculto para planos mensais). */
    private Label criarLabelEconomia(Plano plano, double precoMensal) {
        int meses = (int) Math.round(plano.getDuracaoDias() / 30.0);
        double totalMensal = precoMensal * meses;
        double totalReal   = plano.getValor();
        double economia    = totalMensal - totalReal;

        if (economia > 0.5) {
            Label lbl = new Label(String.format("Economia de R$ %.0f", economia));
            lbl.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 12;");
            return lbl;
        }
        // Sem economia visível: ocupa espaço mas fica vazio
        Label lbl = new Label("");
        lbl.setPrefHeight(16);
        return lbl;
    }

    // ── Seleção de plano ──────────────────────────────────────────────────

    /**
     * Marca o plano clicado como selecionado: atualiza estilos de todos os cards
     * e recalcula a data de término da matrícula.
     *
     * @param plano       Plano clicado.
     * @param cardClicado VBox do card clicado.
     * @param btnClicado  Botão do card clicado.
     */
    private void selecionarPlano(Plano plano, VBox cardClicado, Button btnClicado) {
        planoSelecionado = plano;

        // Reseta todos os cards
        for (javafx.scene.Node node : painelCards.getChildren()) {
            if (node instanceof VBox cardNode) {
                cardNode.setStyle(CARD_BASE);
                // Reseta o botão dentro do card
                for (javafx.scene.Node filho : cardNode.getChildren()) {
                    if (filho instanceof Button btn) {
                        btn.setText("Selecionar");
                        btn.setStyle(BTN_NORMAL);
                    }
                }
            }
        }

        // Destaca o card selecionado
        cardClicado.setStyle(CARD_SELECTED);
        btnClicado.setText("Selecionado");
        btnClicado.setStyle(BTN_SELECTED);

        // Atualiza label de feedback e data de término
        labelPlanoSelecionado.setText("Plano: " + plano.getNome() +
                " — R$ " + String.format("%.2f", plano.getValor()));
        labelPlanoSelecionado.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 12px;" +
                "-fx-padding: 6 10 6 10; -fx-background-color: #0d1f38;" +
                "-fx-background-radius: 6;"
        );
        calcularDataFim();
    }

    // ── Lógica de data ────────────────────────────────────────────────────

    /** Recalcula e exibe a data de término conforme plano e data de início. */
    private void calcularDataFim() {
        LocalDate dataInicio = campDataInicio.getValue();

        if (planoSelecionado != null && dataInicio != null) {
            LocalDate dataFim = dataInicio.plusDays(planoSelecionado.getDuracaoDias());
            labelDataFim.setText("Data de Término: " + dataFim);
        } else {
            labelDataFim.setText("Data de Término: —");
        }
    }

    // ── Ações dos botões ──────────────────────────────────────────────────

    /** Ação do botão "Matricular". */
    @FXML
    private void onMatricularClicado() {
        Aluno aluno     = alunoSelecionado;
        LocalDate dataInicio = campDataInicio.getValue();

        if (aluno == null || planoSelecionado == null || dataInicio == null) {
            exibirStatus("⚠ Selecione aluno, plano (card) e data de início.");
            return;
        }

        LocalDate dataFim = dataInicio.plusDays(planoSelecionado.getDuracaoDias());

        String erroData = MatriculaValidator.validarDatas(dataInicio, dataFim);
        if (erroData != null) {
            exibirStatus("⚠ " + erroData);
            return;
        }

        if (matriculaDAO.existeAtiva(aluno.getId(), planoSelecionado.getId())) {
            exibirStatus("⚠ Este aluno já possui matrícula ativa neste plano.");
            return;
        }

        Matricula matricula = new Matricula();
        matricula.setAlunoId(aluno.getId());
        matricula.setPlanoId(planoSelecionado.getId());
        matricula.setDataInicio(dataInicio.toString());
        matricula.setDataFim(dataFim.toString());
        matricula.setAtiva(true);

        if (matriculaDAO.inserir(matricula)) {
            exibirStatus("✔ Matrícula de '" + aluno.getNome() + "' efetuada até " + dataFim + "!");
            limparFormulario();
        } else {
            exibirStatus("✗ Erro ao realizar matrícula.");
        }
    }

    /** Ação do botão "Cancelar Matrícula" — navega o usuário para a aba Alunos Matriculados. */
    @FXML
    private void onCancelarMatriculaClicado() {
        // Limpa o formulário (ação de cancelar preenchimento)
        limparFormulario();
        exibirStatus("");
    }

    // ── Refresh (chamado pelo PainelPrincipalController) ──────────────────

    /**
     * Recarrega alunos e planos (cards).
     * Chamado sempre que a aba Matrículas for selecionada.
     */
    public void refresh() {
        buscaAluno.setAlunos(alunoDAO.listarTodos());
        gerarCards(planoDAO.listarTodos());
    }

    // ── Auxiliares ────────────────────────────────────────────────────────



    private void limparFormulario() {
        alunoSelecionado = null;
        buscaAluno.limpar();
        campDataInicio.setValue(LocalDate.now());
        labelDataFim.setText("Data de Término: —");
        labelStatus.setText("");
        planoSelecionado = null;
        labelPlanoSelecionado.setText("Plano: nenhum selecionado");
        labelPlanoSelecionado.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;" +
                "-fx-padding: 6 10 6 10; -fx-background-color: #161b22;" +
                "-fx-background-radius: 6;"
        );
        // Reseta todos os cards para o estado normal
        for (javafx.scene.Node node : painelCards.getChildren()) {
            if (node instanceof VBox cardNode) {
                cardNode.setStyle(CARD_BASE);
                for (javafx.scene.Node filho : cardNode.getChildren()) {
                    if (filho instanceof Button btn) {
                        btn.setText("Selecionar");
                        btn.setStyle(BTN_NORMAL);
                    }
                }
            }
        }
    }

    private void exibirStatus(String msg) { labelStatus.setText(msg); }

    // ── Helpers de mapeamento plano → metadados visuais ───────────────────

    /** Determina a categoria textual do card com base na duração do plano. */
    private String resolverCategoria(Plano plano) {
        int dias = plano.getDuracaoDias();
        if (dias <= 31)       return "Básico";
        if (dias <= 95)       return "Recomendado";
        return "Premium";
    }

    /** Cor da label de categoria. */
    private String resolverCorCategoria(Plano plano) {
        int dias = plano.getDuracaoDias();
        if (dias <= 31)  return "#58a6ff";
        if (dias <= 95)  return "#58a6ff";
        return "#f78166";
    }

    /** Período legível (Mensal / Trimestral / Anual). */
    private String resolverPeriodo(Plano plano) {
        int dias = plano.getDuracaoDias();
        if (dias <= 31)  return "Mensal";
        if (dias <= 95)  return "Trimestral";
        return "Anual";
    }

    /** Calcula o preço mensal equivalente do plano. */
    private double calcularPrecoMensal(Plano plano) {
        double meses = plano.getDuracaoDias() / 30.0;
        return meses >= 1 ? plano.getValor() / meses : plano.getValor();
    }
}
