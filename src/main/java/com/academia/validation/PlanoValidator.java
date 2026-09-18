package com.academia.validation;

public final class PlanoValidator {

    private PlanoValidator() {}

    public static String validar(String nome, String descricao, String condicoes, double valor, int duracaoDias) {
        if (nome == null || nome.isBlank()) return "O campo 'Nome' é obrigatório.";
        if (descricao == null || descricao.isBlank()) return "O campo 'Descrição' é obrigatório.";
        if (condicoes == null || condicoes.isBlank()) return "Informe as condições de utilização.";
        if (valor <= 0) return "O valor deve ser maior que zero.";
        if (duracaoDias <= 0) return "A duração deve ser maior que zero.";
        return null;
    }
}
