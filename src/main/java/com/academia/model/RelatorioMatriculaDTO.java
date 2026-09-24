package com.academia.model;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * DTO para o Relatório de Matrículas.
 */
public class RelatorioMatriculaDTO {

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private int matriculaId;
    private String alunoNome;
    private String cpf;
    private String planoNome;
    private double valor;
    private String dataInicio;
    private String dataFim;
    private String situacao;

    public RelatorioMatriculaDTO() {}

    public RelatorioMatriculaDTO(int matriculaId, String alunoNome, String cpf, String planoNome,
                                 double valor, String dataInicio, String dataFim, String situacao) {
        this.matriculaId = matriculaId;
        this.alunoNome = alunoNome;
        this.cpf = cpf;
        this.planoNome = planoNome;
        this.valor = valor;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.situacao = situacao;
    }

    public int getMatriculaId() { return matriculaId; }
    public void setMatriculaId(int matriculaId) { this.matriculaId = matriculaId; }

    public String getAlunoNome() { return alunoNome; }
    public void setAlunoNome(String alunoNome) { this.alunoNome = alunoNome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getPlanoNome() { return planoNome; }
    public void setPlanoNome(String planoNome) { this.planoNome = planoNome; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }

    public String getValorFormatado() {
        return MOEDA.format(valor);
    }

    public String getDataInicioFormatada() {
        return formatarData(dataInicio);
    }

    public String getDataFimFormatada() {
        return formatarData(dataFim);
    }

    private String formatarData(String iso) {
        if (iso == null || iso.isBlank()) return "-";
        try {
            LocalDate d = LocalDate.parse(iso.substring(0, 10));
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }
}
