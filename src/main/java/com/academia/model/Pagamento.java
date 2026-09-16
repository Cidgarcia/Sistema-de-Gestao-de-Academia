package com.academia.model;

/**
 * Model: Pagamento registrado no balcão.
 *
 * <p>Registra os valores recebidos vinculados a uma {@link Matricula} (UC 04).
 * O {@code PagamentoController} usa esta classe ao notificar os observers.</p>
 */
public class Pagamento {

    private int    id;
    private int    matriculaId;
    private double valorPago;
    private String dataPagamento;   // YYYY-MM-DD
    private String formaPagamento;  // "DINHEIRO", "CARTAO", "PIX"
    private String observacoes;

    // Campos auxiliares (join)
    private String nomeAluno;

    public Pagamento() {}

    public Pagamento(int id, int matriculaId, double valorPago,
                     String dataPagamento, String formaPagamento, String observacoes) {
        this.id              = id;
        this.matriculaId     = matriculaId;
        this.valorPago       = valorPago;
        this.dataPagamento   = dataPagamento;
        this.formaPagamento  = formaPagamento;
        this.observacoes     = observacoes;
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }

    public int getMatriculaId()                  { return matriculaId; }
    public void setMatriculaId(int matriculaId)  { this.matriculaId = matriculaId; }

    public double getValorPago()                 { return valorPago; }
    public void setValorPago(double valorPago)   { this.valorPago = valorPago; }

    public String getDataPagamento()                        { return dataPagamento; }
    public void setDataPagamento(String dataPagamento)      { this.dataPagamento = dataPagamento; }

    public String getFormaPagamento()                       { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento)    { this.formaPagamento = formaPagamento; }

    public String getObservacoes()                  { return observacoes; }
    public void setObservacoes(String observacoes)  { this.observacoes = observacoes; }

    public String getNomeAluno()                    { return nomeAluno; }
    public void setNomeAluno(String nomeAluno)      { this.nomeAluno = nomeAluno; }

    @Override
    public String toString() {
        return "Pagamento #" + id + " — R$ " + String.format("%.2f", valorPago) +
               " (" + formaPagamento + ") em " + dataPagamento;
    }
}

