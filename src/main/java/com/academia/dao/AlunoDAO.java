package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Aluno;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Acesso aos dados da tabela Alunos (UC 01).
 *
 * <p>Fornece operações CRUD completas para o cadastro de alunos.</p>
 */
public class AlunoDAO {

    /**
     * Insere um novo aluno no banco de dados.
     *
     * @param aluno Objeto {@link Aluno} com os dados a serem salvos.
     * @return {@code true} se inserido com sucesso.
     */
    public boolean inserir(Aluno aluno) {
        String sql = """
                INSERT INTO Alunos (nome, cpf, email, telefone, endereco, data_nascimento, observacoes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, aluno.getNome());
            ps.setString(2, aluno.getCpf());
            ps.setString(3, aluno.getEmail());
            ps.setString(4, aluno.getTelefone());
            ps.setString(5, aluno.getEndereco());
            ps.setString(6, aluno.getDataNascimento());
            ps.setString(7, aluno.getObservacoes());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        aluno.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.inserir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Atualiza os dados de um aluno existente.
     *
     * @param aluno Objeto {@link Aluno} com os dados atualizados.
     * @return {@code true} se atualizado com sucesso.
     */
    public boolean atualizar(Aluno aluno) {
        String sql = """
                UPDATE Alunos SET nome = ?, cpf = ?, email = ?, telefone = ?,
                endereco = ?, data_nascimento = ?, observacoes = ?
                WHERE id = ?
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, aluno.getNome());
            ps.setString(2, aluno.getCpf());
            ps.setString(3, aluno.getEmail());
            ps.setString(4, aluno.getTelefone());
            ps.setString(5, aluno.getEndereco());
            ps.setString(6, aluno.getDataNascimento());
            ps.setString(7, aluno.getObservacoes());
            ps.setInt(8, aluno.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.atualizar: " + e.getMessage());
        }
        return false;
    }

    /**
     * Exclui um aluno pelo seu ID.
     *
     * @param id ID do aluno a ser excluído.
     * @return {@code true} se excluído com sucesso.
     */
    public boolean excluir(int id) {
        String sql = "DELETE FROM Alunos WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.excluir: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lista todos os alunos cadastrados.
     *
     * @return Lista de objetos {@link Aluno}.
     */
    public List<Aluno> listarTodos() {
        String sql = "SELECT * FROM Alunos ORDER BY nome";
        List<Aluno> alunos = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                alunos.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.listarTodos: " + e.getMessage());
        }
        return alunos;
    }

    /**
     * Busca alunos pelo nome (pesquisa parcial, case-insensitive).
     *
     * @param nome Parte do nome a ser buscada.
     * @return Lista de alunos correspondentes.
     */
    public List<Aluno> buscarPorNome(String nome) {
        String sql = "SELECT * FROM Alunos WHERE nome LIKE ? ORDER BY nome";
        List<Aluno> alunos = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nome + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alunos.add(mapearResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.buscarPorNome: " + e.getMessage());
        }
        return alunos;
    }

    /**
     * Busca aluno pelo seu ID.
     *
     * @param id ID do aluno.
     * @return Objeto Aluno correspondente ou null se não encontrado.
     */
    public Aluno buscarPorId(int id) {
        String sql = "SELECT * FROM Alunos WHERE id = ?";
        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.buscarPorId: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retorna a lista unificada de Aluno + Matrícula via LEFT JOIN.
     * Traz todos os alunos, e se tiverem matrícula, as informações vêm junto.
     * Caso o aluno tenha mais de uma, pode retornar várias linhas.
     * 
     * @param filtroNome Parte do nome do aluno (opcional).
     * @return Lista de {@link com.academia.model.AlunoMatriculaDTO}.
     */
    public List<com.academia.model.AlunoMatriculaDTO> listarAlunosComMatriculas(String filtroNome) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id as aluno_id, a.nome as aluno_nome, a.cpf, a.email, a.telefone, a.endereco, a.data_nascimento,
                       m.id as matricula_id, p.nome as plano_nome, m.data_inicio, m.data_fim, m.ativa
                FROM Alunos a
                LEFT JOIN Matriculas m ON a.id = m.aluno_id
                LEFT JOIN Planos p ON m.plano_id = p.id
                """);

        if (filtroNome != null && !filtroNome.trim().isEmpty()) {
            sql.append(" WHERE a.nome LIKE ?");
        }
        sql.append(" ORDER BY a.nome, m.data_inicio DESC");

        List<com.academia.model.AlunoMatriculaDTO> lista = new ArrayList<>();

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (filtroNome != null && !filtroNome.trim().isEmpty()) {
                ps.setString(1, "%" + filtroNome + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.academia.model.AlunoMatriculaDTO dto = new com.academia.model.AlunoMatriculaDTO();
                    
                    dto.setAlunoId(rs.getInt("aluno_id"));
                    dto.setNome(rs.getString("aluno_nome"));
                    dto.setCpf(rs.getString("cpf"));
                    dto.setEmail(rs.getString("email"));
                    dto.setTelefone(rs.getString("telefone"));
                    dto.setEndereco(rs.getString("endereco"));
                    dto.setDataNascimento(rs.getString("data_nascimento"));
                    
                    int matId = rs.getInt("matricula_id");
                    if (!rs.wasNull()) {
                        dto.setMatriculaId(matId);
                        dto.setNomePlano(rs.getString("plano_nome"));
                        dto.setDataInicio(rs.getString("data_inicio"));
                        dto.setDataFim(rs.getString("data_fim"));
                        dto.setAtiva(rs.getInt("ativa") == 1);
                    }
                    
                    lista.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.listarAlunosComMatriculas: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Busca o aluno pelo CPF ou ID e retorna seu status de acesso atual.
     * Retorna um DTO com os dados do aluno e se ele está Liberado ou Bloqueado.
     *
     * @param termo CPF ou Matrícula (ID) digitado.
     * @return DTO preenchido se encontrar, null caso contrário.
     */
    public com.academia.model.FrequenciaDTO buscarStatusAcesso(String termo) {
        String sql = """
                SELECT a.id, a.nome, a.cpf, p.nome AS plano_nome,
                       (CASE WHEN m.id IS NOT NULL AND m.ativa = 1 AND m.data_fim >= date('now') THEN 1 ELSE 0 END) AS liberado
                FROM Alunos a
                LEFT JOIN Matriculas m ON a.id = m.aluno_id AND m.ativa = 1 AND m.data_fim >= date('now')
                LEFT JOIN Planos p ON m.plano_id = p.id
                WHERE a.cpf = ? OR a.id = ?
                ORDER BY m.id DESC LIMIT 1
                """;

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, termo);
            
            // Tenta converter o termo para int (caso seja ID numérico)
            int idBusca = -1;
            try {
                idBusca = Integer.parseInt(termo);
            } catch (NumberFormatException e) {
                // Ignore se não for número
            }
            ps.setInt(2, idBusca);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString("nome");
                    String cpf = rs.getString("cpf");
                    String id = String.valueOf(rs.getInt("id"));
                    
                    String matriculaCpf = "CPF: " + (cpf != null && !cpf.isBlank() ? cpf : "N/A") + " | ID: #" + id;
                    
                    boolean liberado = rs.getInt("liberado") == 1;
                    String status = liberado ? "Liberado" : "Bloqueado";
                    
                    String plano = rs.getString("plano_nome");
                    String detalhe = liberado ? ("Plano " + (plano != null ? plano : "Ativo")) : "Mensalidade Atrasada / Vencida / Sem Plano";

                    // O campo data_hora_entrada não se aplica aqui, será preenchido depois
                    return new com.academia.model.FrequenciaDTO(null, nome, matriculaCpf, status, detalhe);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] AlunoDAO.buscarStatusAcesso: " + e.getMessage());
        }
        return null;
    }

   /** Mapeia uma linha do ResultSet para um objeto {@link Aluno}. */
    private Aluno mapearResultSet(ResultSet rs) throws SQLException {
        return new Aluno(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cpf"),
                rs.getString("email"),
                rs.getString("telefone"),
                rs.getString("endereco"),
                rs.getString("data_nascimento"),
                rs.getString("observacoes"),
                rs.getString("data_cadastro")
        );
    }
}