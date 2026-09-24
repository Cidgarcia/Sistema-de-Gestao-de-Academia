package com.academia.model;

/**
 * Enum que representa os tipos de relatórios disponíveis no sistema.
 */
public enum TipoRelatorio {
    ALUNOS_ATIVOS("👥 Alunos Ativos", "Listagem de alunos com matrícula ativa no momento"),
    MATRICULAS("📝 Matrículas", "Histórico de matrículas, períodos de vigência e status"),
    PAGAMENTOS("💳 Pagamentos", "Controle de receitas, formas de pagamento e valores recebidos"),
    FREQUENCIA("⏰ Frequência", "Registros de acessos e presença dos alunos na academia"),
    AVALIACOES_FISICAS("📊 Avaliações Físicas", "Histórico de medidas, IMC e evolução física");

    private final String titulo;
    private final String descricao;

    TipoRelatorio(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return titulo;
    }
}
