package com.academia.model;

public class PendenciaFinanceira {

    private int id;
    private int matriculaId;
    private String tipo;
    private String descricao;
    private double valor;
    private String dataVencimento;
    private String situacao;
    private String nomeAluno;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMatriculaId() { return matriculaId; }
    public void setMatriculaId(int matriculaId) { this.matriculaId = matriculaId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(String dataVencimento) { this.dataVencimento = dataVencimento; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public String getNomeAluno() { return nomeAluno; }
    public void setNomeAluno(String nomeAluno) { this.nomeAluno = nomeAluno; }
}
