package dev.lucsenl.claudemeter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Android-Keystore-backed storage for Anthropic API/Admin credentials. */
@SuppressLint("ApplySharedPref")
public final class SecureCredentialStore {
    private static final String KEY_ALIAS = "claude_meter_anthropic_credential_v1";
    private static final String KEY_BLOB = "credential";
    private static final String PREFS = "secure_anthropic_credential_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final Object LOCK = new Object();

    private SecureCredentialStore() {}

    public static void save(Context context, AnthropicCredential credential) throws Exception {
        if (credential == null || !credential.isUsable()) {
            throw new IllegalArgumentException("Anthropic credential is empty.");
        }
        synchronized (LOCK) {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] ciphertext = cipher.doFinal(
                    credential.toJson().toString().getBytes(StandardCharsets.UTF_8));
            JSONObject envelope = new JSONObject();
            envelope.put("iv", Base64.getEncoder().encodeToString(cipher.getIV()));
            envelope.put("ct", Base64.getEncoder().encodeToString(ciphertext));
            if (!context.getSharedPreferences(PREFS, 0).edit()
                    .putString(KEY_BLOB, envelope.toString()).commit()) {
                throw new Exception("Could not persist the encrypted Anthropic credential.");
            }
        }
    }

    public static AnthropicCredential load(Context context) {
        synchronized (LOCK) {
            SharedPreferences preferences = context.getSharedPreferences(PREFS, 0);
            String encoded = preferences.getString(KEY_BLOB, null);
            if (encoded == null || encoded.isEmpty()) return null;
            try {
                JSONObject envelope = new JSONObject(encoded);
                byte[] iv = Base64.getDecoder().decode(envelope.getString("iv"));
                byte[] ciphertext = Base64.getDecoder().decode(envelope.getString("ct"));
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
                return AnthropicCredential.fromJson(new JSONObject(new String(
                        cipher.doFinal(ciphertext), StandardCharsets.UTF_8)));
            } catch (Exception exception) {
                preferences.edit().remove(KEY_BLOB).commit();
                return null;
            }
        }
    }

    public static boolean isConfigured(Context context) {
        return load(context) != null;
    }

    public static void clear(Context context) {
        synchronized (LOCK) {
            context.getSharedPreferences(PREFS, 0).edit().clear().commit();
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS);
            } catch (Exception ignored) {
            }
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyGenParameterSpec.PURPOSE_ENCRYPT | KeyGenParameterSpec.PURPOSE_DECRYPT)
                .setBlockModes("GCM")
                .setEncryptionPaddings("NoPadding")
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
