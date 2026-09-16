package com.academia.model;

/**
 * Model: Avaliação Física de um aluno.
 *
 * <p>Registra medidas corporais e progresso do aluno, podendo ser acessado
 * apenas por usuários com perfil INSTRUTOR (UC 05).</p>
 */
public class AvaliacaoFisica {

    private int    id;
    private int    alunoId;
    private int    instrutorId;
    private String dataAvaliacao;   // YYYY-MM-DD
    private Double pesoKg;
    private Double alturaCm;
    private Double imc;             // calculado: peso / (altura/100)²
    private Double gorduraPerc;
    private Double massaMuscular;
    private String observacoes;

    // Campos auxiliares (join)
    private String nomeAluno;
    private String nomeInstrutor;

    public AvaliacaoFisica() {}

    // ── Getters & Setters ────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getAlunoId()                         { return alunoId; }
    public void setAlunoId(int alunoId)             { this.alunoId = alunoId; }

    public int getInstrutorId()                     { return instrutorId; }
    public void setInstrutorId(int instrutorId)     { this.instrutorId = instrutorId; }

    public String getDataAvaliacao()                        { return dataAvaliacao; }
    public void setDataAvaliacao(String dataAvaliacao)      { this.dataAvaliacao = dataAvaliacao; }

    public Double getPesoKg()                       { return pesoKg; }
    public void setPesoKg(Double pesoKg)            { this.pesoKg = pesoKg; }

    public Double getAlturaCm()                     { return alturaCm; }
    public void setAlturaCm(Double alturaCm)        { this.alturaCm = alturaCm; }

    public Double getImc()                          { return imc; }
    public void setImc(Double imc)                  { this.imc = imc; }

    public Double getGorduraPerc()                  { return gorduraPerc; }
    public void setGorduraPerc(Double gorduraPerc)  { this.gorduraPerc = gorduraPerc; }

    public Double getMassaMuscular()                        { return massaMuscular; }
    public void setMassaMuscular(Double massaMuscular)      { this.massaMuscular = massaMuscular; }

    public String getObservacoes()                  { return observacoes; }
    public void setObservacoes(String observacoes)  { this.observacoes = observacoes; }

    public String getNomeAluno()                    { return nomeAluno; }
    public void setNomeAluno(String nomeAluno)      { this.nomeAluno = nomeAluno; }

    public String getNomeInstrutor()                        { return nomeInstrutor; }
    public void setNomeInstrutor(String nomeInstrutor)      { this.nomeInstrutor = nomeInstrutor; }

    /**
     * Calcula o IMC automaticamente a partir do peso e altura informados.
     * Deve ser chamado antes de salvar o objeto.
     */
    public void calcularImc() {
        if (pesoKg != null && alturaCm != null && alturaCm > 0) {
            double alturaM = alturaCm / 100.0;
            this.imc = pesoKg / (alturaM * alturaM);
        }
    }

    @Override
    public String toString() {
        return "Avaliação #" + id + " — " + nomeAluno + " em " + dataAvaliacao;
    }
}

