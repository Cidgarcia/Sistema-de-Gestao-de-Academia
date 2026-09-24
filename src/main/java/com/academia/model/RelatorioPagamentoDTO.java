package com.academia.model;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * DTO para o Relatório de Pagamentos.
 */
public class RelatorioPagamentoDTO {

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private int pagamentoId;
    private int matriculaId;
    private String alunoNome;
    private String dataPagamento;
    private String tipo;
    private String formaPagamento;
    private double valorPago;
    private String observacoes;

    public RelatorioPagamentoDTO() {}

    public RelatorioPagamentoDTO(int pagamentoId, int matriculaId, String alunoNome, String dataPagamento,
                                 String tipo, String formaPagamento, double valorPago, String observacoes) {
        this.pagamentoId = pagamentoId;
        this.matriculaId = matriculaId;
        this.alunoNome = alunoNome;
        this.dataPagamento = dataPagamento;
        this.tipo = tipo;
        this.formaPagamento = formaPagamento;
        this.valorPago = valorPago;
        this.observacoes = observacoes;
    }

    public int getPagamentoId() { return pagamentoId; }
    public void setPagamentoId(int pagamentoId) { this.pagamentoId = pagamentoId; }

    public int getMatriculaId() { return matriculaId; }
    public void setMatriculaId(int matriculaId) { this.matriculaId = matriculaId; }

    public String getAlunoNome() { return alunoNome; }
    public void setAlunoNome(String alunoNome) { this.alunoNome = alunoNome; }

    public String getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(String dataPagamento) { this.dataPagamento = dataPagamento; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public double getValorPago() { return valorPago; }
    public void setValorPago(double valorPago) { this.valorPago = valorPago; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getValorFormatado() {
        return MOEDA.format(valorPago);
    }

    public String getDataPagamentoFormatada() {
        if (dataPagamento == null || dataPagamento.isBlank()) return "-";
        try {
            LocalDate d = LocalDate.parse(dataPagamento.substring(0, 10));
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dataPagamento;
        }
    }
}
