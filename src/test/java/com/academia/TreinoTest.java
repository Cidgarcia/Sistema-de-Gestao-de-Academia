package com.academia;

import com.academia.model.Exercicio;
import com.academia.model.TreinoDivisao;
import com.academia.model.Usuario;
import com.academia.validation.TreinoValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreinoTest {

    @Test
    void permiteEdicaoSomenteParaInstrutor() {
        Usuario funcionario = new Usuario(1, "Funcionário", "func", "", "FUNCIONARIO");
        Usuario instrutor = new Usuario(2, "Instrutor", "inst", "", "INSTRUTOR");

        assertFalse(TreinoValidator.podeEditar(funcionario));
        assertTrue(TreinoValidator.podeEditar(instrutor));
        assertFalse(TreinoValidator.podeEditar(null));
    }

    @Test
    void validaCamposSeriesEOrdemDosExercicios() {
        TreinoDivisao divisao = new TreinoDivisao("Treino A");
        assertTrue(TreinoValidator.validar(List.of(divisao)).contains("não possui exercícios"));

        Exercicio exercicio = exercicioValido(1);
        divisao.getExercicios().add(exercicio);
        assertNull(TreinoValidator.validar(List.of(divisao)));

        exercicio.setNome("");
        assertTrue(TreinoValidator.validar(List.of(divisao)).startsWith("Preencha exercício"));
        exercicio.setNome("Supino");
        exercicio.setSeries(0);
        assertEquals("As séries devem ser maiores que zero.", TreinoValidator.validar(List.of(divisao)));
        exercicio.setSeries(3);
        divisao.getExercicios().add(exercicioValido(1));
        assertTrue(TreinoValidator.validar(List.of(divisao)).startsWith("A ordem"));
    }

    private static Exercicio exercicioValido(int ordem) {
        Exercicio exercicio = new Exercicio(ordem);
        exercicio.setGrupoMuscular("Peito");
        exercicio.setNome("Supino");
        exercicio.setSeries(3);
        exercicio.setRepeticoes("10");
        return exercicio;
    }
}
