package com.academia.validation;

import java.util.regex.Pattern;

public final class AlunoValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private AlunoValidator() {}

    public static String validar(String nome, String cpf, String email, String telefone) {
        if (nome == null || nome.isBlank()) return "O campo 'Nome' é obrigatório.";
        if (!cpfValido(cpf)) return "Informe um CPF válido.";
        if (email == null || !EMAIL.matcher(email.trim()).matches()) return "Informe um e-mail válido.";

        String digitosTelefone = somenteDigitos(telefone);
        if (digitosTelefone.length() != 10 && digitosTelefone.length() != 11) {
            return "Informe um telefone válido com DDD.";
        }
        return null;
    }

    public static boolean cpfValido(String cpf) {
        String digitos = somenteDigitos(cpf);
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) return false;

        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (digitos.charAt(i) - '0') * (tamanho + 1 - i);
            int verificador = 11 - soma % 11;
            if (verificador >= 10) verificador = 0;
            if (verificador != digitos.charAt(tamanho) - '0') return false;
        }
        return true;
    }

    private static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
