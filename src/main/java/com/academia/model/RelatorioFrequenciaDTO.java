package com.academia.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO para o Relatório de Frequência.
 */
public class RelatorioFrequenciaDTO {

    private int frequenciaId;
    private String dataHoraEntrada;
    private String alunoNome;
    private String cpf;
    private String matriculaPlano;
    private String statusAcesso;

    public RelatorioFrequenciaDTO() {}

    public RelatorioFrequenciaDTO(int frequenciaId, String dataHoraEntrada, String alunoNome,
                                  String cpf, String matriculaPlano, String statusAcesso) {
        this.frequenciaId = frequenciaId;
        this.dataHoraEntrada = dataHoraEntrada;
        this.alunoNome = alunoNome;
        this.cpf = cpf;
        this.matriculaPlano = matriculaPlano;
        this.statusAcesso = statusAcesso;
    }

    public int getFrequenciaId() { return frequenciaId; }
    public void setFrequenciaId(int frequenciaId) { this.frequenciaId = frequenciaId; }

    public String getDataHoraEntrada() { return dataHoraEntrada; }
    public void setDataHoraEntrada(String dataHoraEntrada) { this.dataHoraEntrada = dataHoraEntrada; }

    public String getAlunoNome() { return alunoNome; }
    public void setAlunoNome(String alunoNome) { this.alunoNome = alunoNome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getMatriculaPlano() { return matriculaPlano; }
    public void setMatriculaPlano(String matriculaPlano) { this.matriculaPlano = matriculaPlano; }

    public String getStatusAcesso() { return statusAcesso; }
    public void setStatusAcesso(String statusAcesso) { this.statusAcesso = statusAcesso; }

    public String getDataHoraEntradaFormatada() {
        if (dataHoraEntrada == null || dataHoraEntrada.isBlank()) return "-";
        try {
            // Usually formatted like "2026-09-24 13:00:00" or ISO "2026-09-24T13:00:00"
            String limpa = dataHoraEntrada.replace("T", " ");
            if (limpa.length() >= 19) {
                DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime dt = LocalDateTime.parse(limpa.substring(0, 19), inFmt);
                return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            } else if (limpa.length() >= 10) {
                return limpa.substring(8, 10) + "/" + limpa.substring(5, 7) + "/" + limpa.substring(0, 4);
            }
            return dataHoraEntrada;
        } catch (Exception e) {
            return dataHoraEntrada;
        }
    }
}
