package com.academia.dao;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Formato versionado: pbkdf2-sha256$iterações$salt$hash (salt e hash em Base64). */
public final class SenhaHasher {
    private static final String ALGORITMO = "pbkdf2-sha256";
    private static final int ITERACOES = 600_000;
    private static final int TAMANHO_SALT = 16;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SenhaHasher() {}

    public static String criar(String senha) {
        byte[] salt = new byte[TAMANHO_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(senha, salt, ITERACOES);
        return ALGORITMO + "$" + ITERACOES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificar(String senha, String armazenada) {
        if (senha == null || armazenada == null) return false;
        if (isLegado(armazenada)) {
            byte[] esperado = HexFormat.of().parseHex(armazenada);
            byte[] atual = sha256(senha);
            return MessageDigest.isEqual(esperado, atual);
        }
        String[] partes = armazenada.split("\\$", -1);
        if (partes.length != 4 || !ALGORITMO.equals(partes[0])) return false;
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < 1 || iteracoes > 1_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (salt.length < TAMANHO_SALT || esperado.length != TAMANHO_HASH_BITS / 8) return false;
            return MessageDigest.isEqual(esperado, derivar(senha, salt, iteracoes));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isLegado(String armazenada) {
        return armazenada != null && armazenada.matches("[0-9a-f]{64}");
    }

    private static byte[] sha256(String senha) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(senha.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static byte[] derivar(String senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 indisponível", e);
        } finally {
            spec.clearPassword();
        }
    }
}
