package com.academia.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Data Transfer Object (DTO) que unifica os dados de Aluno e sua respectiva Matrícula.
 * Utilizado para popular a listagem principal da aba "Alunos".
 */
public class AlunoMatriculaDTO {

    // Dados do Aluno
    private int alunoId;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private String endereco;       // Adicionado: endereço do aluno
    private String dataNascimento;

    // Dados da Matrícula (podem ser nulos se o aluno não tiver matrícula)
    private Integer matriculaId;
    private String nomePlano;
    private String dataInicio;
    private String dataFim;
    private Boolean ativa;

    // Propriedade calculada para exibição na tabela ("ATIVO", "VENCIDO", "CANCELADO", "SEM MATRÍCULA")
    private String situacao;

    public AlunoMatriculaDTO() {}

    public int getAlunoId() { return alunoId; }
    public void setAlunoId(int alunoId) { this.alunoId = alunoId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(String dataNascimento) { this.dataNascimento = dataNascimento; }

    public Integer getMatriculaId() { return matriculaId; }
    public void setMatriculaId(Integer matriculaId) { this.matriculaId = matriculaId; }

    public String getNomePlano() { return nomePlano; }
    public void setNomePlano(String nomePlano) { this.nomePlano = nomePlano; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public Boolean getAtiva() { return ativa; }
    public void setAtiva(Boolean ativa) { this.ativa = ativa; }

    public String getSituacao() {
        if (matriculaId == null || matriculaId == 0) return "SEM MATRÍCULA";
        if (Boolean.FALSE.equals(ativa)) return "CANCELADO";

        if (dataFim != null && !dataFim.isEmpty()) {
            try {
                LocalDate fim = LocalDate.parse(dataFim);
                if (fim.isBefore(LocalDate.now())) {
                    return "VENCIDO";
                }
            } catch (DateTimeParseException e) {
                // Ignorar erro de parse e seguir padrão
            }
        }
        return "ATIVO";
    }
}