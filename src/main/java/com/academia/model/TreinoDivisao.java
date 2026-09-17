package com.academia.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Representa uma aba (Divisão de Treino) da ficha do aluno.
 */
public class TreinoDivisao {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty alunoId = new SimpleIntegerProperty();
    private final StringProperty nomeDivisao = new SimpleStringProperty("");
    private final ObservableList<Exercicio> exercicios = FXCollections.observableArrayList();

    public TreinoDivisao() {
    }

    public TreinoDivisao(String nomeDivisao) {
        this.nomeDivisao.set(nomeDivisao);
    }

    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getAlunoId() { return alunoId.get(); }
    public void setAlunoId(int value) { alunoId.set(value); }
    public IntegerProperty alunoIdProperty() { return alunoId; }

    public String getNomeDivisao() { return nomeDivisao.get(); }
    public void setNomeDivisao(String value) { nomeDivisao.set(value); }
    public StringProperty nomeDivisaoProperty() { return nomeDivisao; }

    public ObservableList<Exercicio> getExercicios() {
        return exercicios;
    }
}
