package com.academia.validation;

import java.time.LocalDate;

public final class MatriculaValidator {

    private MatriculaValidator() {}

    public static String validarDatas(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) return "Informe as datas de início e término.";
        if (inicio.isBefore(LocalDate.now())) return "A data inicial não pode estar no passado.";
        if (!fim.isAfter(inicio)) return "A data final deve ser posterior à data inicial.";
        return null;
    }
}
