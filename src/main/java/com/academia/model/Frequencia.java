package com.academia.model;

/**
 * Model: Representa o registro de entrada (frequência) de um aluno na academia.
 */
public class Frequencia {

    private int id;
    private int alunoId;
    private String dataHoraEntrada; // formato: YYYY-MM-DD HH:MM:SS

    public Frequencia() {}

    public Frequencia(int id, int alunoId, String dataHoraEntrada) {
        this.id = id;
        this.alunoId = alunoId;
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAlunoId() {
        return alunoId;
    }

    public void setAlunoId(int alunoId) {
        this.alunoId = alunoId;
    }

    public String getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public void setDataHoraEntrada(String dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }
}
