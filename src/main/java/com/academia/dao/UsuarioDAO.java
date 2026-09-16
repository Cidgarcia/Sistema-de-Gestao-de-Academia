package com.academia.dao;

import com.academia.database.ConexaoSQLite;
import com.academia.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * DAO: Acesso aos dados da tabela Usuarios.
 *
 * <p>Fornece operações de autenticação para o login do sistema.</p>
 */
public class UsuarioDAO {

    /**
     * Autentica um usuário verificando login, senha (hash SHA-256) e perfil.
     *
     * @param login  Login do usuário.
     * @param senha  Senha em texto plano (será convertida em hash).
     * @param perfil Perfil desejado: "FUNCIONARIO" ou "INSTRUTOR".
     * @return O objeto {@link Usuario} se autenticado, ou {@code null} se inválido.
     */
    public Usuario autenticar(String login, String senha, String perfil) {
        String senhaHash = gerarHashSHA256(senha);
        String sql = "SELECT * FROM Usuarios WHERE login = ? AND senha = ? AND perfil = ?";

        try (Connection conn = ConexaoSQLite.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, login);
            ps.setString(2, senhaHash);
            ps.setString(3, perfil.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO] Falha na autenticação: " + e.getMessage());
        }
        return null;
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

    /**
     * Gera o hash SHA-256 da senha em texto plano.
     *
     * @param texto Senha em texto plano.
     * @return Hash hexadecimal da senha.
     */
    public static String gerarHashSHA256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 não disponível.", e);
        }
    }
}

