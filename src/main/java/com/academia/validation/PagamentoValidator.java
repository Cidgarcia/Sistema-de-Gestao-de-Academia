package com.academia.validation;

public final class PagamentoValidator {

    private PagamentoValidator() {}

    public static String validar(double valor, String tipo, Integer pendenciaId) {
        if (valor <= 0) return "O valor deve ser maior que zero.";
        if (tipo == null || tipo.isBlank()) return "Selecione o tipo de pagamento.";
        if ("MENSALIDADE".equals(tipo) && pendenciaId == null) {
            return "Selecione uma mensalidade pendente.";
        }
        return null;
    }
}
