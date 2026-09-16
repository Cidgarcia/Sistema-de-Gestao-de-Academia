package com.academia.model;

/**
 * Model: Plano de assinatura da academia.
 *
 * <p>Representa as opções de planos disponíveis (mensal, trimestral, etc.),
 * conforme UC 02.</p>
 */
public class Plano {

    private int    id;
    private String nome;
    private String descricao;
    private double valor;
    private int    duracaoDias;  // ex.: 30, 90, 180, 365

    public Plano() {}

    public Plano(int id, String nome, String descricao, double valor, int duracaoDias) {
        this.id          = id;
        this.nome        = nome;
        this.descricao   = descricao;
        this.valor       = valor;
        this.duracaoDias = duracaoDias;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getNome()                     { return nome; }
    public void setNome(String nome)            { this.nome = nome; }

    public String getDescricao()                { return descricao; }
    public void setDescricao(String descricao)  { this.descricao = descricao; }

    public double getValor()                    { return valor; }
    public void setValor(double valor)          { this.valor = valor; }

    public int getDuracaoDias()                     { return duracaoDias; }
    public void setDuracaoDias(int duracaoDias)     { this.duracaoDias = duracaoDias; }

    @Override
    public String toString() {
        return nome + " — R$ " + String.format("%.2f", valor) +
               " (" + duracaoDias + " dias)";
    }
}

