package com.academia.util;

import com.academia.model.Aluno;
import com.academia.dao.MatriculaDAO;
import com.academia.model.Matricula;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Consumer;

/** Configura busca incremental de aluno por nome, CPF ou código do aluno. */
public final class AlunoSearchSupport {
    private final TextField campo;
    private final ListView<Aluno> resultados;
    private final Consumer<Aluno> aoSelecionar;
    private List<Aluno> alunos = List.of();
    private Map<Integer, List<Integer>> matriculasPorAluno = Map.of();
    private boolean atualizandoCampo;
    private Aluno selecionado;

    public AlunoSearchSupport(TextField campo, ListView<Aluno> resultados,
                              Consumer<Aluno> aoSelecionar) {
        this.campo = campo;
        this.resultados = resultados;
        this.aoSelecionar = aoSelecionar;
        resultados.setVisible(false);
        resultados.setManaged(false);
        resultados.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Aluno aluno, boolean vazio) {
                super.updateItem(aluno, vazio);
                setText(vazio || aluno == null ? null : String.format("%s  ·  CPF %s  ·  Matrículas %s",
                        aluno.getNome(), aluno.getCpf(),
                        matriculasPorAluno.getOrDefault(aluno.getId(), List.of(aluno.getId()))));
            }
        });
        campo.textProperty().addListener((obs, anterior, texto) -> pesquisar(texto));
        resultados.getSelectionModel().selectedItemProperty().addListener((obs, anterior, aluno) -> {
            if (aluno != null) selecionar(aluno);
        });
    }

    public void setAlunos(Collection<Aluno> alunos) {
        this.alunos = List.copyOf(alunos);
        matriculasPorAluno = new MatriculaDAO().listarTodas().stream().collect(
                Collectors.groupingBy(Matricula::getAlunoId,
                        Collectors.mapping(Matricula::getId, Collectors.toList())));
    }

    public Aluno getSelecionado() {
        return selecionado;
    }

    public void limpar() {
        campo.clear();
    }

    private void pesquisar(String texto) {
        if (atualizandoCampo) return;
        selecionado = null;
        aoSelecionar.accept(null);
        String consulta = texto == null ? "" : texto.trim().toLowerCase();
        if (consulta.isEmpty()) {
            ocultarResultados();
            return;
        }
        String digitos = consulta.replaceAll("\\D", "");
        List<Aluno> encontrados = alunos.stream().filter(aluno -> {
            String nome = aluno.getNome() == null ? "" : aluno.getNome().toLowerCase();
            String cpf = aluno.getCpf() == null ? "" : aluno.getCpf();
            String cpfDigitos = cpf.replaceAll("\\D", "");
            return nome.contains(consulta)
                    || (!digitos.isEmpty() && (cpfDigitos.contains(digitos)
                    || Integer.toString(aluno.getId()).contains(digitos)
                    || matriculasPorAluno.getOrDefault(aluno.getId(), List.of()).stream()
                    .anyMatch(id -> Integer.toString(id).contains(digitos))));
        }).limit(8).toList();
        resultados.setItems(FXCollections.observableArrayList(encontrados));
        boolean mostrar = !encontrados.isEmpty();
        resultados.setVisible(mostrar);
        resultados.setManaged(mostrar);
    }

    private void selecionar(Aluno aluno) {
        selecionado = aluno;
        aoSelecionar.accept(aluno);
        atualizandoCampo = true;
        campo.setText(aluno.getNome() + " · #" + aluno.getId());
        atualizandoCampo = false;
        ocultarResultados();
    }

    private void ocultarResultados() {
        resultados.getItems().clear();
        resultados.setVisible(false);
        resultados.setManaged(false);
    }
}
