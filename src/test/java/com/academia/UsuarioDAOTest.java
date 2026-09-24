package com.academia;

import com.academia.dao.SenhaHasher;
import com.academia.dao.UsuarioDAO;
import com.academia.database.ConexaoSQLite;
import com.academia.model.Usuario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioDAOTest {
    private static Path bancoTeste;
    private static String caminhoAnterior;

    @BeforeAll
    static void prepararBanco() throws IOException {
        bancoTeste = Files.createTempFile("academia-usuarios-", ".sqlite");
        caminhoAnterior = System.getProperty("academia.db.path");
        System.setProperty("academia.db.path", bancoTeste.toString());
    }

    @AfterAll
    static void limparBanco() throws IOException {
        ConexaoSQLite.fecharConexao();
        if (bancoTeste != null) Files.deleteIfExists(bancoTeste);
        if (caminhoAnterior == null) System.clearProperty("academia.db.path");
        else System.setProperty("academia.db.path", caminhoAnterior);
    }

    @Test
    void contasPadraoUsamPbkdf2ComSaltsDistintosEExigemSenhaEPerfilCorretos() throws SQLException {
        UsuarioDAO dao = new UsuarioDAO();
        for (String[] conta : new String[][] {
                {"admin", "FUNCIONARIO", "INSTRUTOR"},
                {"instrutor", "INSTRUTOR", "FUNCIONARIO"}
        }) {
            String hash = hashDe(conta[0]);
            assertTrue(hash.startsWith("pbkdf2-sha256$"), conta[0]);
            assertFalse(SenhaHasher.isLegado(hash), conta[0]);
            assertTrue(SenhaHasher.verificar("admin123", hash), conta[0]);
            assertFalse(SenhaHasher.verificar("senhaErrada", hash), conta[0]);

            Usuario usuario = dao.autenticar(conta[0], "admin123", conta[1]);
            assertNotNull(usuario, conta[0]);
            assertEquals(conta[1], usuario.getPerfil());
            assertNull(dao.autenticar(conta[0], "senhaErrada", conta[1]));
            assertNull(dao.autenticar(conta[0], "admin123", conta[2]));
        }
        assertNotEquals(hashDe("admin").split("\\$")[2],
                hashDe("instrutor").split("\\$")[2]);
    }

    @Test
    void loginLegadoMigraHashEContinuaFuncionando() throws SQLException, NoSuchAlgorithmException {
        String senha = "senhaLegada123";
        String hashLegado = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(senha.getBytes(StandardCharsets.UTF_8)));
        inserirUsuario("legado", hashLegado, "FUNCIONARIO");
        UsuarioDAO dao = new UsuarioDAO();

        assertTrue(SenhaHasher.isLegado(hashDe("legado")));
        assertNull(dao.autenticar("legado", "senhaErrada", "FUNCIONARIO"));
        assertEquals(hashLegado, hashDe("legado"));

        assertNotNull(dao.autenticar("legado", senha, "FUNCIONARIO"));
        String hashMigrado = hashDe("legado");
        assertNotEquals(hashLegado, hashMigrado);
        assertTrue(hashMigrado.startsWith("pbkdf2-sha256$"));
        assertFalse(SenhaHasher.isLegado(hashMigrado));
        assertTrue(SenhaHasher.verificar(senha, hashMigrado));
        assertNotNull(dao.autenticar("legado", senha, "FUNCIONARIO"));
        assertEquals(hashMigrado, hashDe("legado"));
    }

    @Test
    void trocaDeSenhaExigeAtualEComprimentoMinimoEInvalidaSenhaAntiga() throws SQLException {
        inserirUsuario("troca", SenhaHasher.criar("senhaAntiga123"), "INSTRUTOR");
        UsuarioDAO dao = new UsuarioDAO();
        Usuario usuario = dao.autenticar("troca", "senhaAntiga123", "INSTRUTOR");
        assertNotNull(usuario);
        String hashOriginal = hashDe("troca");

        assertFalse(dao.alterarSenha(usuario.getId(), "senhaErrada", "senhaNova123"));
        assertFalse(dao.alterarSenha(usuario.getId(), "senhaAntiga123", "curta"));
        assertEquals(hashOriginal, hashDe("troca"));

        assertTrue(dao.alterarSenha(usuario.getId(), "senhaAntiga123", "senhaNova123"));
        String hashNovo = hashDe("troca");
        assertNotEquals(hashOriginal, hashNovo);
        assertFalse(SenhaHasher.isLegado(hashNovo));
        assertTrue(SenhaHasher.verificar("senhaNova123", hashNovo));
        assertNull(dao.autenticar("troca", "senhaAntiga123", "INSTRUTOR"));
        assertNotNull(dao.autenticar("troca", "senhaNova123", "INSTRUTOR"));
    }

    private static void inserirUsuario(String login, String hash, String perfil) throws SQLException {
        try (PreparedStatement ps = ConexaoSQLite.getConexao().prepareStatement(
                "INSERT INTO Usuarios(nome, login, senha, perfil) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, login);
            ps.setString(2, login);
            ps.setString(3, hash);
            ps.setString(4, perfil);
            assertEquals(1, ps.executeUpdate());
        }
    }

    private static String hashDe(String login) throws SQLException {
        try (PreparedStatement ps = ConexaoSQLite.getConexao().prepareStatement(
                "SELECT senha FROM Usuarios WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), login);
                return rs.getString(1);
            }
        }
    }
}
