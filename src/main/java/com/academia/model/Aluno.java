package com.academia.model;

/**
 * Model: Aluno da academia.
 *
 * <p>Contém os dados cadastrais do aluno, incluindo contatos,
 * endereço, data de nascimento e observações gerais (UC 01).</p>
 */
public class Aluno {

    private int     id;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private String endereco;       // Adicionado: endereço resumido
    private String dataNascimento;  // formato: YYYY-MM-DD
    private String observacoes;
    private String dataCadastro;    // formato: YYYY-MM-DD

    public Aluno() {}

    public Aluno(int id, String nome, String cpf, String email,
                 String telefone, String endereco, String dataNascimento,
                 String observacoes, String dataCadastro) {
        this.id              = id;
        this.nome            = nome;
        this.cpf             = cpf;
        this.email           = email;
        this.telefone        = telefone;
        this.endereco        = endereco;
        this.dataNascimento  = dataNascimento;
        this.observacoes     = observacoes;
        this.dataCadastro    = dataCadastro;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getNome()                         { return nome; }
    public void setNome(String nome)                { this.nome = nome; }

    public String getCpf()                          { return cpf; }
    public void setCpf(String cpf)                  { this.cpf = cpf; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    public String getTelefone()                     { return telefone; }
    public void setTelefone(String telefone)        { this.telefone = telefone; }

    public String getEndereco()                     { return endereco; }
    public void setEndereco(String endereco)        { this.endereco = endereco; }

    public String getDataNascimento()                      { return dataNascimento; }
    public void setDataNascimento(String dataNascimento)   { this.dataNascimento = dataNascimento; }

    public String getObservacoes()                  { return observacoes; }
    public void setObservacoes(String observacoes)  { this.observacoes = observacoes; }

    public String getDataCadastro()                 { return dataCadastro; }
    public void setDataCadastro(String dataCadastro){ this.dataCadastro = dataCadastro; }

    @Override
    public String toString() { return nome + " (CPF: " + cpf + ")"; }
}