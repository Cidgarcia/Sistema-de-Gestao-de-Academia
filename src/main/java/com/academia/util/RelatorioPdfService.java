package com.academia.util;

import com.academia.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Serviço gerador de relatórios em PDF com OpenPDF.
 */
public class RelatorioPdfService {

    private static final Color COR_AZUL = new Color(0, 168, 107); // GymCore Green #00A86B
    private static final Color COR_AZUL_ESCURO = new Color(11, 45, 34); // #0B2D22
    private static final Color COR_LINHA_PAR = new Color(242, 249, 245);
    private static final Color COR_CINZA_BORDA = new Color(215, 230, 222);
    private static final Color COR_CINZA_TEXTO = new Color(100, 116, 139);

    private static final Font FONTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COR_AZUL_ESCURO);
    private static final Font FONTE_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 9, COR_CINZA_TEXTO);
    private static final Font FONTE_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONTE_DADOS = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONTE_DADOS_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private static final Font FONTE_KPI_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COR_CINZA_TEXTO);
    private static final Font FONTE_KPI_VALOR = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COR_AZUL);

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // ── 1. Relatório de Alunos Ativos ─────────────────────────────────────────

    public static void gerarPdfAlunosAtivos(List<RelatorioAlunoAtivoDTO> lista, String filtros, File destino)
            throws IOException, DocumentException {

        Document doc = new Document(PageSize.A4, 25, 25, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        adicionarCabecalho(doc, "RELATÓRIO DE ALUNOS ATIVOS", filtros);

        // Resumo KPI
        adicionarQuadroResumo(doc, new String[][]{
                {"TOTAL DE ALUNOS ATIVOS", String.valueOf(lista.size())}
        });

        // Tabela
        PdfPTable tabela = new PdfPTable(7);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{1.0f, 3.2f, 2.0f, 2.0f, 2.2f, 1.8f, 1.6f});

        String[] headers = {"ID", "Aluno", "CPF", "Telefone", "Plano", "Vigência Até", "Dias Rest."};
        adicionarCabecalhoTabela(tabela, headers);

        boolean par = false;
        for (RelatorioAlunoAtivoDTO item : lista) {
            Color bg = par ? COR_LINHA_PAR : Color.WHITE;
            tabela.addCell(celula(String.valueOf(item.getAlunoId()), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getCpf(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getTelefone(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getPlanoNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getDataFimFormatada(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getDiasRestantesTexto(), bg, Element.ALIGN_CENTER));
            par = !par;
        }

        doc.add(tabela);
        adicionarRodape(doc, lista.size());
        doc.close();
    }

    // ── 2. Relatório de Matrículas ─────────────────────────────────────────────

    public static void gerarPdfMatriculas(List<RelatorioMatriculaDTO> lista, String filtros, File destino)
            throws IOException, DocumentException {

        Document doc = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        adicionarCabecalho(doc, "RELATÓRIO DE MATRÍCULAS", filtros);

        long ativas = lista.stream().filter(m -> "Ativa".equalsIgnoreCase(m.getSituacao())).count();
        long vencidas = lista.stream().filter(m -> "Vencida".equalsIgnoreCase(m.getSituacao())).count();
        long canceladas = lista.stream().filter(m -> "Cancelada".equalsIgnoreCase(m.getSituacao())).count();
        double valorTotal = lista.stream().mapToDouble(RelatorioMatriculaDTO::getValor).sum();

        adicionarQuadroResumo(doc, new String[][]{
                {"TOTAL MATRÍCULAS", String.valueOf(lista.size())},
                {"ATIVAS", String.valueOf(ativas)},
                {"VENCIDAS", String.valueOf(vencidas)},
                {"CANCELADAS", String.valueOf(canceladas)},
                {"VALOR CONTRATADO", MOEDA.format(valorTotal)}
        });

        PdfPTable tabela = new PdfPTable(8);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{1.0f, 3.2f, 2.0f, 2.2f, 1.8f, 1.8f, 1.8f, 1.6f});

        String[] headers = {"Matrícula", "Aluno", "CPF", "Plano", "Valor", "Data Início", "Data Fim", "Situação"};
        adicionarCabecalhoTabela(tabela, headers);

        boolean par = false;
        for (RelatorioMatriculaDTO item : lista) {
            Color bg = par ? COR_LINHA_PAR : Color.WHITE;
            tabela.addCell(celula("#" + item.getMatriculaId(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getAlunoNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getCpf(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getPlanoNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getValorFormatado(), bg, Element.ALIGN_RIGHT));
            tabela.addCell(celula(item.getDataInicioFormatada(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getDataFimFormatada(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getSituacao(), bg, Element.ALIGN_CENTER));
            par = !par;
        }

        doc.add(tabela);
        adicionarRodape(doc, lista.size());
        doc.close();
    }

    // ── 3. Relatório de Pagamentos ─────────────────────────────────────────────

    public static void gerarPdfPagamentos(List<RelatorioPagamentoDTO> lista, String filtros, File destino)
            throws IOException, DocumentException {

        Document doc = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        adicionarCabecalho(doc, "RELATÓRIO DE PAGAMENTOS", filtros);

        double totalArrecadado = lista.stream().mapToDouble(RelatorioPagamentoDTO::getValorPago).sum();
        double totalDinheiro = lista.stream()
                .filter(p -> "DINHEIRO".equalsIgnoreCase(p.getFormaPagamento()))
                .mapToDouble(RelatorioPagamentoDTO::getValorPago).sum();
        double totalCartao = lista.stream()
                .filter(p -> "CARTAO".equalsIgnoreCase(p.getFormaPagamento()) || "CARTÃO".equalsIgnoreCase(p.getFormaPagamento()))
                .mapToDouble(RelatorioPagamentoDTO::getValorPago).sum();
        double totalPix = lista.stream()
                .filter(p -> "PIX".equalsIgnoreCase(p.getFormaPagamento()))
                .mapToDouble(RelatorioPagamentoDTO::getValorPago).sum();

        adicionarQuadroResumo(doc, new String[][]{
                {"TOTAL ARRECADADO", MOEDA.format(totalArrecadado)},
                {"TOTAL TRANSAÇÕES", String.valueOf(lista.size())},
                {"DINHEIRO", MOEDA.format(totalDinheiro)},
                {"CARTÃO", MOEDA.format(totalCartao)},
                {"PIX", MOEDA.format(totalPix)}
        });

        PdfPTable tabela = new PdfPTable(8);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{1.0f, 1.2f, 3.2f, 1.8f, 2.0f, 1.8f, 1.8f, 2.4f});

        String[] headers = {"ID", "Matrícula", "Aluno", "Data", "Tipo", "Forma", "Valor", "Obs."};
        adicionarCabecalhoTabela(tabela, headers);

        boolean par = false;
        for (RelatorioPagamentoDTO item : lista) {
            Color bg = par ? COR_LINHA_PAR : Color.WHITE;
            tabela.addCell(celula(String.valueOf(item.getPagamentoId()), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula("#" + item.getMatriculaId(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getAlunoNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getDataPagamentoFormatada(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getTipo(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getFormaPagamento(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getValorFormatado(), bg, Element.ALIGN_RIGHT));
            tabela.addCell(celula(item.getObservacoes() != null ? item.getObservacoes() : "-", bg, Element.ALIGN_LEFT));
            par = !par;
        }

        doc.add(tabela);
        adicionarRodape(doc, lista.size());
        doc.close();
    }

    // ── 4. Relatório de Frequência ─────────────────────────────────────────────

    public static void gerarPdfFrequencia(List<RelatorioFrequenciaDTO> lista, String filtros, File destino)
            throws IOException, DocumentException {

        Document doc = new Document(PageSize.A4, 25, 25, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        adicionarCabecalho(doc, "RELATÓRIO DE FREQUÊNCIA E ACESSOS", filtros);

        long alunosUnicos = lista.stream().map(RelatorioFrequenciaDTO::getAlunoNome).distinct().count();

        adicionarQuadroResumo(doc, new String[][]{
                {"TOTAL DE ACESSOS", String.valueOf(lista.size())},
                {"ALUNOS PRESENTES (ÚNICOS)", String.valueOf(alunosUnicos)}
        });

        PdfPTable tabela = new PdfPTable(6);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{1.0f, 2.5f, 3.5f, 2.0f, 2.5f, 1.5f});

        String[] headers = {"ID", "Data/Hora Entrada", "Aluno", "CPF", "Matrícula / Plano", "Status"};
        adicionarCabecalhoTabela(tabela, headers);

        boolean par = false;
        for (RelatorioFrequenciaDTO item : lista) {
            Color bg = par ? COR_LINHA_PAR : Color.WHITE;
            tabela.addCell(celula(String.valueOf(item.getFrequenciaId()), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getDataHoraEntradaFormatada(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getAlunoNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getCpf(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getMatriculaPlano(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getStatusAcesso(), bg, Element.ALIGN_CENTER));
            par = !par;
        }

        doc.add(tabela);
        adicionarRodape(doc, lista.size());
        doc.close();
    }

    // ── 5. Relatório de Avaliações Físicas ─────────────────────────────────────

    public static void gerarPdfAvaliacoes(List<RelatorioAvaliacaoDTO> lista, String filtros, File destino)
            throws IOException, DocumentException {

        Document doc = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        adicionarCabecalho(doc, "RELATÓRIO DE AVALIAÇÕES FÍSICAS", filtros);

        double mediaImc = lista.stream()
                .filter(a -> a.getImc() != null && a.getImc() > 0)
                .mapToDouble(RelatorioAvaliacaoDTO::getImc).average().orElse(0.0);
        double mediaPeso = lista.stream()
                .filter(a -> a.getPesoKg() != null && a.getPesoKg() > 0)
                .mapToDouble(RelatorioAvaliacaoDTO::getPesoKg).average().orElse(0.0);

        adicionarQuadroResumo(doc, new String[][]{
                {"TOTAL AVALIAÇÕES", String.valueOf(lista.size())},
                {"PESO MÉDIO", mediaPeso > 0 ? String.format("%.1f kg", mediaPeso) : "-"},
                {"IMC MÉDIO", mediaImc > 0 ? String.format("%.2f", mediaImc) : "-"}
        });

        PdfPTable tabela = new PdfPTable(10);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{1.0f, 1.8f, 3.0f, 2.2f, 1.4f, 1.4f, 1.4f, 2.0f, 1.4f, 1.5f});

        String[] headers = {"ID", "Data", "Aluno", "Instrutor", "Peso", "Alt.", "IMC", "Classificação", "% Gord.", "Massa M."};
        adicionarCabecalhoTabela(tabela, headers);

        boolean par = false;
        for (RelatorioAvaliacaoDTO item : lista) {
            Color bg = par ? COR_LINHA_PAR : Color.WHITE;
            tabela.addCell(celula(String.valueOf(item.getAvaliacaoId()), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getDataAvaliacaoFormatada(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getAlunoNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getInstrutorNome(), bg, Element.ALIGN_LEFT));
            tabela.addCell(celula(item.getPesoFormatado(), bg, Element.ALIGN_RIGHT));
            tabela.addCell(celula(item.getAlturaFormatada(), bg, Element.ALIGN_RIGHT));
            tabela.addCell(celula(item.getImcFormatado(), bg, Element.ALIGN_RIGHT));
            tabela.addCell(celula(item.getClassificacaoImc(), bg, Element.ALIGN_CENTER));
            tabela.addCell(celula(item.getGorduraFormatada(), bg, Element.ALIGN_RIGHT));
            tabela.addCell(celula(item.getMassaFormatada(), bg, Element.ALIGN_RIGHT));
            par = !par;
        }

        doc.add(tabela);
        adicionarRodape(doc, lista.size());
        doc.close();
    }

    // ── Métodos Auxiliares ────────────────────────────────────────────────────

    private static void adicionarCabecalho(Document doc, String titulo, String filtros) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3.5f, 1.5f});
        header.setSpacingAfter(8);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("GYMCORE — MANAGEMENT SYSTEM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COR_AZUL)));
        leftCell.addElement(new Paragraph(titulo, FONTE_TITULO));
        if (filtros != null && !filtros.isBlank()) {
            leftCell.addElement(new Paragraph("Filtros aplicados: " + filtros, FONTE_SUBTITULO));
        }

        String dataHoraAtual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pData = new Paragraph("Emitido em:\n" + dataHoraAtual, FONTE_SUBTITULO);
        pData.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(pData);

        header.addCell(leftCell);
        header.addCell(rightCell);
        doc.add(header);

        // Linha divisória
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingAfter(10);
        PdfPCell line = new PdfPCell(new Phrase(" "));
        line.setFixedHeight(2f);
        line.setBackgroundColor(COR_AZUL);
        line.setBorder(Rectangle.NO_BORDER);
        divider.addCell(line);
        doc.add(divider);
    }

    private static void adicionarQuadroResumo(Document doc, String[][] kpis) throws DocumentException {
        if (kpis == null || kpis.length == 0) return;

        PdfPTable quadro = new PdfPTable(kpis.length);
        quadro.setWidthPercentage(100);
        quadro.setSpacingAfter(12);

        for (String[] kpi : kpis) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(COR_LINHA_PAR);
            cell.setBorderColor(COR_CINZA_BORDA);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pTitulo = new Paragraph(kpi[0], FONTE_KPI_TITULO);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            Paragraph pValor = new Paragraph(kpi[1], FONTE_KPI_VALOR);
            pValor.setAlignment(Element.ALIGN_CENTER);

            cell.addElement(pTitulo);
            cell.addElement(pValor);
            quadro.addCell(cell);
        }

        doc.add(quadro);
    }

    private static void adicionarCabecalhoTabela(PdfPTable tabela, String[] headers) {
        tabela.setHeaderRows(1);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONTE_CABECALHO_TABELA));
            cell.setBackgroundColor(COR_AZUL);
            cell.setBorderColor(COR_AZUL_ESCURO);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabela.addCell(cell);
        }
    }

    private static PdfPCell celula(String texto, Color bg, int alinhamento) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "-", FONTE_DADOS));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COR_CINZA_BORDA);
        cell.setHorizontalAlignment(alinhamento);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    private static void adicionarRodape(Document doc, int totalRegistros) throws DocumentException {
        Paragraph p = new Paragraph("Total de registros listados: " + totalRegistros +
                "  |  Sistema de Gestão de Academia — Documento Confidencial", FONTE_SUBTITULO);
        p.setSpacingBefore(10);
        p.setAlignment(Element.ALIGN_RIGHT);
        doc.add(p);
    }
}
