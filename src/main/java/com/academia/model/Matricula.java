package com.academia.model;

/**
 * Model: Matrícula de um aluno em um plano.
 *
 * <p>Vincula um {@link Aluno} a um {@link Plano}, definindo o período
 * de vigência da matrícula (UC 03).</p>
 */
public class Matricula {

    private int     id;
    private int     alunoId;
    private int     planoId;
    private String  dataInicio;  // YYYY-MM-DD
    private String  dataFim;     // YYYY-MM-DD
    private boolean ativa;

    // Campos auxiliares (join com outras tabelas — não persistidos diretamente)
    private String nomeAluno;
    private String nomePlano;

    public Matricula() {}

    public Matricula(int id, int alunoId, int planoId,
                     String dataInicio, String dataFim, boolean ativa) {
        this.id         = id;
        this.alunoId    = alunoId;
        this.planoId    = planoId;
        this.dataInicio = dataInicio;
        this.dataFim    = dataFim;
        this.ativa      = ativa;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getAlunoId()                     { return alunoId; }
    public void setAlunoId(int alunoId)         { this.alunoId = alunoId; }

    public int getPlanoId()                     { return planoId; }
    public void setPlanoId(int planoId)         { this.planoId = planoId; }

    public String getDataInicio()               { return dataInicio; }
    public void setDataInicio(String dataInicio){ this.dataInicio = dataInicio; }

    public String getDataFim()                  { return dataFim; }
    public void setDataFim(String dataFim)      { this.dataFim = dataFim; }

    public boolean isAtiva()                    { return ativa; }
    public void setAtiva(boolean ativa)         { this.ativa = ativa; }

    public String getNomeAluno()                        { return nomeAluno; }
    public void setNomeAluno(String nomeAluno)          { this.nomeAluno = nomeAluno; }

    public String getNomePlano()                        { return nomePlano; }
    public void setNomePlano(String nomePlano)          { this.nomePlano = nomePlano; }

    @Override
    public String toString() {
        return "Matrícula #" + id + " — " + nomeAluno + " / " + nomePlano;
    }
}

