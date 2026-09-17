package com.academia.model;

import javafx.beans.property.*;

/**
 * Modelo para Exercício, utilizando JavaFX Properties para permitir
 * a edição bidirecional na TableView.
 */
public class Exercicio {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty fichaId = new SimpleIntegerProperty();
    private final IntegerProperty ordem = new SimpleIntegerProperty();
    private final StringProperty grupoMuscular = new SimpleStringProperty("");
    private final StringProperty nome = new SimpleStringProperty("");
    private final IntegerProperty series = new SimpleIntegerProperty(0);
    private final StringProperty repeticoes = new SimpleStringProperty("");
    private final StringProperty carga = new SimpleStringProperty("");
    private final IntegerProperty descanso = new SimpleIntegerProperty(0);
    private final StringProperty observacoes = new SimpleStringProperty("");

    public Exercicio() {
    }

    public Exercicio(int ordem) {
        this.ordem.set(ordem);
    }

    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getFichaId() { return fichaId.get(); }
    public void setFichaId(int value) { fichaId.set(value); }
    public IntegerProperty fichaIdProperty() { return fichaId; }

    public int getOrdem() { return ordem.get(); }
    public void setOrdem(int value) { ordem.set(value); }
    public IntegerProperty ordemProperty() { return ordem; }

    public String getGrupoMuscular() { return grupoMuscular.get(); }
    public void setGrupoMuscular(String value) { grupoMuscular.set(value); }
    public StringProperty grupoMuscularProperty() { return grupoMuscular; }

    public String getNome() { return nome.get(); }
    public void setNome(String value) { nome.set(value); }
    public StringProperty nomeProperty() { return nome; }

    public int getSeries() { return series.get(); }
    public void setSeries(int value) { series.set(value); }
    public IntegerProperty seriesProperty() { return series; }

    public String getRepeticoes() { return repeticoes.get(); }
    public void setRepeticoes(String value) { repeticoes.set(value); }
    public StringProperty repeticoesProperty() { return repeticoes; }

    public String getCarga() { return carga.get(); }
    public void setCarga(String value) { carga.set(value); }
    public StringProperty cargaProperty() { return carga; }

    public int getDescanso() { return descanso.get(); }
    public void setDescanso(int value) { descanso.set(value); }
    public IntegerProperty descansoProperty() { return descanso; }

    public String getObservacoes() { return observacoes.get(); }
    public void setObservacoes(String value) { observacoes.set(value); }
    public StringProperty observacoesProperty() { return observacoes; }
}
