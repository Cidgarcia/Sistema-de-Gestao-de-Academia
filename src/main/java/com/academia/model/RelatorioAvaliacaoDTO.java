package com.academia.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DTO para o Relatório de Avaliações Físicas.
 */
public class RelatorioAvaliacaoDTO {

    private int avaliacaoId;
    private String dataAvaliacao;
    private String alunoNome;
    private String instrutorNome;
    private Double pesoKg;
    private Double alturaCm;
    private Double imc;
    private Double gorduraPerc;
    private Double massaMuscular;
    private String observacoes;

    public RelatorioAvaliacaoDTO() {}

    public RelatorioAvaliacaoDTO(int avaliacaoId, String dataAvaliacao, String alunoNome, String instrutorNome,
                                 Double pesoKg, Double alturaCm, Double imc, Double gorduraPerc,
                                 Double massaMuscular, String observacoes) {
        this.avaliacaoId = avaliacaoId;
        this.dataAvaliacao = dataAvaliacao;
        this.alunoNome = alunoNome;
        this.instrutorNome = instrutorNome;
        this.pesoKg = pesoKg;
        this.alturaCm = alturaCm;
        this.imc = imc;
        this.gorduraPerc = gorduraPerc;
        this.massaMuscular = massaMuscular;
        this.observacoes = observacoes;
    }

    public int getAvaliacaoId() { return avaliacaoId; }
    public void setAvaliacaoId(int avaliacaoId) { this.avaliacaoId = avaliacaoId; }

    public String getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(String dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }

    public String getAlunoNome() { return alunoNome; }
    public void setAlunoNome(String alunoNome) { this.alunoNome = alunoNome; }

    public String getInstrutorNome() { return instrutorNome; }
    public void setInstrutorNome(String instrutorNome) { this.instrutorNome = instrutorNome; }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public Double getAlturaCm() { return alturaCm; }
    public void setAlturaCm(Double alturaCm) { this.alturaCm = alturaCm; }

    public Double getImc() { return imc; }
    public void setImc(Double imc) { this.imc = imc; }

    public Double getGorduraPerc() { return gorduraPerc; }
    public void setGorduraPerc(Double gorduraPerc) { this.gorduraPerc = gorduraPerc; }

    public Double getMassaMuscular() { return massaMuscular; }
    public void setMassaMuscular(Double massaMuscular) { this.massaMuscular = massaMuscular; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getClassificacaoImc() {
        if (imc == null || imc <= 0) return "-";
        if (imc < 18.5) return "Abaixo do peso";
        if (imc < 25.0) return "Peso normal";
        if (imc < 30.0) return "Sobrepeso";
        if (imc < 35.0) return "Obesidade I";
        if (imc < 40.0) return "Obesidade II";
        return "Obesidade III";
    }

    public String getDataAvaliacaoFormatada() {
        if (dataAvaliacao == null || dataAvaliacao.isBlank()) return "-";
        try {
            LocalDate d = LocalDate.parse(dataAvaliacao.substring(0, 10));
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dataAvaliacao;
        }
    }

    public String getPesoFormatado() {
        return pesoKg != null ? String.format("%.1f kg", pesoKg) : "-";
    }

    public String getAlturaFormatada() {
        return alturaCm != null ? String.format("%.0f cm", alturaCm) : "-";
    }

    public String getImcFormatado() {
        return imc != null ? String.format("%.2f", imc) : "-";
    }

    public String getGorduraFormatada() {
        return gorduraPerc != null ? String.format("%.1f%%", gorduraPerc) : "-";
    }

    public String getMassaFormatada() {
        return massaMuscular != null ? String.format("%.1f kg", massaMuscular) : "-";
    }
}
