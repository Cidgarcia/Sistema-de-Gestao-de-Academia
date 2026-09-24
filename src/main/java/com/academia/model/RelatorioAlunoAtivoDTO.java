package com.academia.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DTO para o Relatório de Alunos Ativos.
 */
public class RelatorioAlunoAtivoDTO {

    private int alunoId;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private String planoNome;
    private String dataInicio;
    private String dataFim;
    private int diasRestantes;

    public RelatorioAlunoAtivoDTO() {}

    public RelatorioAlunoAtivoDTO(int alunoId, String nome, String cpf, String telefone, String email,
                                  String planoNome, String dataInicio, String dataFim, int diasRestantes) {
        this.alunoId = alunoId;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.email = email;
        this.planoNome = planoNome;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.diasRestantes = diasRestantes;
    }

    public int getAlunoId() { return alunoId; }
    public void setAlunoId(int alunoId) { this.alunoId = alunoId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPlanoNome() { return planoNome; }
    public void setPlanoNome(String planoNome) { this.planoNome = planoNome; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public int getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(int diasRestantes) { this.diasRestantes = diasRestantes; }

    public String getDataInicioFormatada() {
        return formatarData(dataInicio);
    }

    public String getDataFimFormatada() {
        return formatarData(dataFim);
    }

    public String getDiasRestantesTexto() {
        if (diasRestantes <= 0) return "Vence hoje";
        if (diasRestantes == 1) return "1 dia";
        return diasRestantes + " dias";
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
