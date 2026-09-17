package com.academia.model;

/**
 * DTO: Usado para agrupar os dados de Frequência, Aluno e Status
 * para exibição no Histórico Recente da aba Recepção.
 */
public class FrequenciaDTO {

    private String dataHoraEntrada;
    private String nomeAluno;
    private String matriculaCpf; // Pode ser a matrícula (#ID) ou o CPF para exibição
    private String statusAcesso; // "Liberado" ou "Bloqueado"
    private String detalhePlano; // Nome do plano ou motivo do bloqueio

    public FrequenciaDTO() {}

    public FrequenciaDTO(String dataHoraEntrada, String nomeAluno, String matriculaCpf, String statusAcesso, String detalhePlano) {
        this.dataHoraEntrada = dataHoraEntrada;
        this.nomeAluno = nomeAluno;
        this.matriculaCpf = matriculaCpf;
        this.statusAcesso = statusAcesso;
        this.detalhePlano = detalhePlano;
    }

    public String getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public void setDataHoraEntrada(String dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getNomeAluno() {
        return nomeAluno;
    }

    public void setNomeAluno(String nomeAluno) {
        this.nomeAluno = nomeAluno;
    }

    public String getMatriculaCpf() {
        return matriculaCpf;
    }

    public void setMatriculaCpf(String matriculaCpf) {
        this.matriculaCpf = matriculaCpf;
    }

    public String getStatusAcesso() {
        return statusAcesso;
    }

    public void setStatusAcesso(String statusAcesso) {
        this.statusAcesso = statusAcesso;
    }

    public String getDetalhePlano() {
        return detalhePlano;
    }

    public void setDetalhePlano(String detalhePlano) {
        this.detalhePlano = detalhePlano;
    }
}
