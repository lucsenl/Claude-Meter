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

/** AES-GCM storage for the Anthropic API key. */
@SuppressLint("ApplySharedPref")
public final class SecureTokenStore {
    private static final String KEY_ALIAS = "claude_meter_api_key_v1";
    private static final String KEY_BLOB = "blob";
    private static final String PREFS = "secure_anthropic_api_key_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final Object LOCK = new Object();

    private SecureTokenStore() {}

    public static void save(Context context, AuthTokens credential) throws Exception {
        if (credential == null || !credential.isUsable()) {
            throw new Exception("Enter a valid Anthropic API key.");
        }
        synchronized (LOCK) {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] ciphertext = cipher.doFinal(
                    credential.toJson().toString().getBytes(StandardCharsets.UTF_8));
            JSONObject envelope = new JSONObject();
            envelope.put("iv", Base64.getEncoder().encodeToString(cipher.getIV()));
            envelope.put("ct", Base64.getEncoder().encodeToString(ciphertext));
            if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_BLOB, envelope.toString()).commit()) {
                throw new Exception("Could not persist the encrypted API key.");
            }
        }
    }

    public static AuthTokens load(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String value = prefs.getString(KEY_BLOB, null);
            if (value == null || value.isEmpty()) return null;
            try {
                JSONObject envelope = new JSONObject(value);
                byte[] iv = Base64.getDecoder().decode(envelope.getString("iv"));
                byte[] ciphertext = Base64.getDecoder().decode(envelope.getString("ct"));
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
                return AuthTokens.fromJson(new JSONObject(new String(
                        cipher.doFinal(ciphertext), StandardCharsets.UTF_8)));
            } catch (Exception exception) {
                prefs.edit().remove(KEY_BLOB).commit();
                return null;
            }
        }
    }

    public static boolean isSignedIn(Context context) {
        AuthTokens value = load(context);
        return value != null && value.isUsable();
    }

    public static void clear(Context context) {
        synchronized (LOCK) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS);
            } catch (Exception ignored) {}
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
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                        | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                        android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
