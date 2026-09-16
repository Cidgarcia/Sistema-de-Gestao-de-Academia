package com.academia.model;

/**
 * Model: Usuário do sistema.
 *
 * <p>Representa um usuário autenticado, que pode ser Funcionário ou Instrutor.
 * O perfil determina quais funcionalidades estão disponíveis na interface.</p>
 */
public class Usuario {

    private int id;
    private String nome;
    private String login;
    private String senha;   // armazenada como hash SHA-256
    private String perfil;  // "FUNCIONARIO" ou "INSTRUTOR"

    public Usuario() {}

    public Usuario(int id, String nome, String login, String senha, String perfil) {
        this.id     = id;
        this.nome   = nome;
        this.login  = login;
        this.senha  = senha;
        this.perfil = perfil;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getId()               { return id; }
    public void setId(int id)        { this.id = id; }

    public String getNome()             { return nome; }
    public void setNome(String nome)    { this.nome = nome; }

    public String getLogin()            { return login; }
    public void setLogin(String login)  { this.login = login; }

    public String getSenha()            { return senha; }
    public void setSenha(String senha)  { this.senha = senha; }

    public String getPerfil()              { return perfil; }
    public void setPerfil(String perfil)   { this.perfil = perfil; }

    /** Verifica se este usuário tem perfil de Instrutor. */
    public boolean isInstrutor() {
        return "INSTRUTOR".equalsIgnoreCase(perfil);
    }

    @Override
    public String toString() {
        return nome + " (" + perfil + ")";
    }
}

