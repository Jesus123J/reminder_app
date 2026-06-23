package com.reminder.app.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado de notas con contraseña (AES/GCM).
 *
 * La clave AES-256 se deriva de la contraseña con SHA-256. El blob resultante
 * (IV + texto cifrado + tag de autenticacion) se codifica en Base64. GCM ademas
 * autentica: si la contraseña es incorrecta, el descifrado falla con excepcion.
 *
 * @author Jesus Gutierrez
 */
public final class NoteCrypto {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private NoteCrypto() {
    }

    /** SHA-256 de la contraseña en hexadecimal (para verificacion rapida). */
    public static String hash(String password) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String encrypt(String plaintext, String password) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORM);
        cipher.init(Cipher.ENCRYPT_MODE, keyFrom(password), new GCMParameterSpec(TAG_BITS, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ct, 0, out, iv.length, ct.length);
        return Base64.getEncoder().encodeToString(out);
    }

    public static String decrypt(String blob, String password) throws Exception {
        byte[] all = Base64.getDecoder().decode(blob);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(all, 0, iv, 0, IV_LENGTH);
        byte[] ct = new byte[all.length - IV_LENGTH];
        System.arraycopy(all, IV_LENGTH, ct, 0, ct.length);
        Cipher cipher = Cipher.getInstance(TRANSFORM);
        cipher.init(Cipher.DECRYPT_MODE, keyFrom(password), new GCMParameterSpec(TAG_BITS, iv));
        return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    }

    private static SecretKeySpec keyFrom(String password) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
