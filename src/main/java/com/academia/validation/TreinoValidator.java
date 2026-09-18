package com.academia.validation;

import com.academia.model.Exercicio;
import com.academia.model.TreinoDivisao;
import com.academia.model.Usuario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TreinoValidator {

    private TreinoValidator() {}

    public static boolean podeEditar(Usuario usuario) {
        return usuario != null && usuario.isInstrutor();
    }

    public static String validar(List<TreinoDivisao> divisoes) {
        if (divisoes.isEmpty()) return "Adicione pelo menos uma divisão de treino.";

        for (TreinoDivisao divisao : divisoes) {
            if (divisao.getNomeDivisao() == null || divisao.getNomeDivisao().isBlank()) {
                return "Informe o nome de todas as divisões.";
            }
            if (divisao.getExercicios().isEmpty()) {
                return "A divisão '" + divisao.getNomeDivisao() + "' não possui exercícios.";
            }

            Set<Integer> ordens = new HashSet<>();
            for (Exercicio exercicio : divisao.getExercicios()) {
                if (exercicio.getNome() == null || exercicio.getNome().isBlank()
                        || exercicio.getGrupoMuscular() == null || exercicio.getGrupoMuscular().isBlank()
                        || exercicio.getRepeticoes() == null || exercicio.getRepeticoes().isBlank()) {
                    return "Preencha exercício, grupo muscular e repetições em '" + divisao.getNomeDivisao() + "'.";
                }
                if (exercicio.getSeries() <= 0) return "As séries devem ser maiores que zero.";
                if (exercicio.getOrdem() <= 0 || !ordens.add(exercicio.getOrdem())) {
                    return "A ordem dos exercícios deve ser positiva e não pode se repetir na divisão.";
                }
            }
        }
        return null;
    }
}
