package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Usuario;

import java.sql.*;

/**
 * DAO: Acesso aos dados da tabela Usuarios.
 *
 * <p>Fornece operações de autenticação para o login do sistema.</p>
 */
public class UsuarioDAO {

    /**
     * Autentica um usuário por login e senha, identificando seu perfil automaticamente.
     *
     * @param login Login do usuário.
     * @param senha Senha informada pelo usuário.
     * @return O objeto {@link Usuario} com o perfil identificado se autenticado, ou {@code null} se inválido.
     */
    public Usuario autenticar(String login, String senha) {
        if (login == null || senha == null) return null;
        String sql = "SELECT * FROM Usuarios WHERE login = ?";

        try {
            Connection conn = ConexaoSQLite.getConexao();
            Usuario usuario;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, login);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !SenhaHasher.verificar(senha, rs.getString("senha"))) return null;
                    usuario = mapearResultSet(rs);
                }
            }
            if (SenhaHasher.isLegado(usuario.getSenha())) {
                String novoHash = SenhaHasher.criar(senha);
                if (!atualizarHash(conn, usuario.getId(), usuario.getSenha(), novoHash)) return null;
                usuario.setSenha(novoHash);
            }
            return usuario;
        } catch (SQLException e) {
            System.err.println("[ERRO] Falha na autenticação.");
            return null;
        }
    }

    /**
     * Autentica um usuário e atualiza hashes SHA-256 antigos após login válido.
     *
     * @param login  Login do usuário.
     * @param senha  Senha informada pelo usuário.
     * @param perfil Perfil desejado: "FUNCIONARIO" ou "INSTRUTOR".
     * @return O objeto {@link Usuario} se autenticado, ou {@code null} se inválido.
     */
    public Usuario autenticar(String login, String senha, String perfil) {
        if (login == null || senha == null || perfil == null) return null;
        String sql = "SELECT * FROM Usuarios WHERE login = ? AND perfil = ?";

        try {
            Connection conn = ConexaoSQLite.getConexao();
            Usuario usuario;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, login);
                ps.setString(2, perfil.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !SenhaHasher.verificar(senha, rs.getString("senha"))) return null;
                    usuario = mapearResultSet(rs);
                }
            }
            if (SenhaHasher.isLegado(usuario.getSenha())) {
                String novoHash = SenhaHasher.criar(senha);
                if (!atualizarHash(conn, usuario.getId(), usuario.getSenha(), novoHash)) return null;
                usuario.setSenha(novoHash);
            }
            return usuario;
        } catch (SQLException e) {
            System.err.println("[ERRO] Falha na autenticação.");
            return null;
        }
    }

    /** Troca a senha após verificar a atual; a política mínima também vale fora da interface. */
    public boolean alterarSenha(int usuarioId, String senhaAtual, String senhaNova) {
        if (senhaAtual == null || senhaNova == null || senhaNova.length() < 8) return false;
        try {
            Connection conn = ConexaoSQLite.getConexao();
            String hashAtual;
            try (PreparedStatement ps = conn.prepareStatement("SELECT senha FROM Usuarios WHERE id = ?")) {
                ps.setInt(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    hashAtual = rs.getString("senha");
                }
            }
            if (!SenhaHasher.verificar(senhaAtual, hashAtual)) return false;
            return atualizarHash(conn, usuarioId, hashAtual, SenhaHasher.criar(senhaNova));
        } catch (SQLException e) {
            System.err.println("[ERRO] Falha ao alterar senha.");
            return false;
        }
    }

    private boolean atualizarHash(Connection conn, int id, String hashAtual, String novoHash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Usuarios SET senha = ? WHERE id = ? AND senha = ?")) {
            ps.setString(1, novoHash);
            ps.setInt(2, id);
            ps.setString(3, hashAtual);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Busca um usuário pelo seu ID.
     *
     * @param id ID do usuário.
     * @return O objeto {@link Usuario} encontrado, ou {@code null}.
     */
    public Usuario buscarPorId(int id) {
        String sql = "SELECT * FROM Usuarios WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] buscarPorId: " + e.getMessage());
        }
        return null;
    }

    // ── Métodos auxiliares privados ───────────────────────────────

    /** Mapeia uma linha do ResultSet para um objeto {@link Usuario}. */
    private Usuario mapearResultSet(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("login"),
                rs.getString("senha"),
                rs.getString("perfil")
        );
    }

}
