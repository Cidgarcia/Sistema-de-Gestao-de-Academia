package com.academia.util;

import com.academia.model.Aluno;
import com.academia.model.Exercicio;
import com.academia.model.TreinoDivisao;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PdfGenerator {

    // Azul usado para identidade visual do documento
    private static final Color BLUE_COLOR = new Color(25, 118, 210); // #1976D2
    private static final Color LIGHT_BLUE = new Color(227, 242, 253);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLUE_COLOR);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLUE_COLOR);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

    public static void gerarFichaTreino(Aluno aluno, List<TreinoDivisao> divisoes, File destino) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(destino));

        document.open();

        adicionarCabecalho(document, aluno);
        document.add(new Paragraph(" "));

        for (int i = 0; i < divisoes.size(); i++) {
            TreinoDivisao div = divisoes.get(i);
            adicionarTabelaTreino(document, div);
            
            // Adiciona uma linha em branco ou nova pagina entre treinos
            if (i < divisoes.size() - 1) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph(" "));
            }
        }

        document.close();
    }

    private static void adicionarCabecalho(Document document, Aluno aluno) throws DocumentException {
        Paragraph title = new Paragraph("FICHA DE TREINO PERSONALIZADA", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});

        PdfPCell nomeCell = new PdfPCell(new Phrase("NOME DO ALUNO:\n" + aluno.getNome(), BOLD_FONT));
        nomeCell.setPadding(10);
        nomeCell.setBorderColor(BLUE_COLOR);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        PdfPCell dataCell = new PdfPCell(new Phrase("DATA DE INÍCIO:\n" + sdf.format(new Date()), BOLD_FONT));
        dataCell.setPadding(10);
        dataCell.setBorderColor(BLUE_COLOR);

        infoTable.addCell(nomeCell);
        infoTable.addCell(dataCell);

        document.add(infoTable);
    }

    private static void adicionarTabelaTreino(Document document, TreinoDivisao div) throws DocumentException {
        // Subtitulo do dia de treino
        Paragraph subtitle = new Paragraph("TREINO " + div.getNomeDivisao().toUpperCase(), SUBTITLE_FONT);
        subtitle.setSpacingAfter(5);
        document.add(subtitle);

        // Criar tabela (6 colunas)
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.5f, 1f, 1.5f, 1.5f, 1.5f, 0.8f}); // Proporcoes das colunas

        // Cabecalhos da tabela
        String[] headers = {"Exercício", "Séries", "Repetições", "Carga", "Descanso", "✓"};
        for (String h : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(h, HEADER_FONT));
            headerCell.setBackgroundColor(BLUE_COLOR);
            headerCell.setPadding(6);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(headerCell);
        }

        // Linhas com os exercicios
        boolean alternate = false;
        for (Exercicio ex : div.getExercicios()) {
            Color rowColor = alternate ? LIGHT_BLUE : Color.WHITE;

            table.addCell(criarCelula(ex.getNome(), rowColor, Element.ALIGN_LEFT));
            table.addCell(criarCelula(String.valueOf(ex.getSeries()), rowColor, Element.ALIGN_CENTER));
            table.addCell(criarCelula(ex.getRepeticoes(), rowColor, Element.ALIGN_CENTER));
            
            // Carga: mostra o valor se existir, senão mostra o espaço para preencher
            String cargaText = (ex.getCarga() != null && !ex.getCarga().trim().isEmpty()) ? ex.getCarga() + " kg" : "____kg";
            table.addCell(criarCelula(cargaText, rowColor, Element.ALIGN_CENTER));
            
            table.addCell(criarCelula(ex.getDescanso() + "s", rowColor, Element.ALIGN_CENTER));
            
            // Celula de checkbox
            PdfPCell checkCell = new PdfPCell(new Phrase("O", NORMAL_FONT)); // Simula um checkbox usando a letra O ou um quadro branco
            checkCell.setBackgroundColor(rowColor);
            checkCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            checkCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            checkCell.setPadding(6);
            table.addCell(checkCell);

            alternate = !alternate;
        }

        document.add(table);
    }

    private static PdfPCell criarCelula(String texto, Color bg, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", NORMAL_FONT));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        return cell;
    }
}
