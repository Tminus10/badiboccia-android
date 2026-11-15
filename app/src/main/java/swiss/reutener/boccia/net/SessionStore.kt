package swiss.reutener.boccia.net

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import swiss.reutener.boccia.model.Team
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** A logged-in team: the API token plus the team it belongs to. */
@Serializable
data class TeamSession(val token: String, val team: Team)

/**
 * Stores the team session, one per server, encrypted with a key that never leaves the device's
 * Android Keystore (the counterpart of the iOS Keychain). A copy restored onto another device
 * can't be decrypted there and simply counts as logged out.
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun load(server: ServerEnvironment): TeamSession? {
        val stored = prefs.getString(server.key, null) ?: return null
        return runCatching {
            val (iv, data) = stored.split(":").map { Base64.decode(it, Base64.NO_WRAP) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            ApiClient.json.decodeFromString<TeamSession>(String(cipher.doFinal(data)))
        }.getOrNull()
    }

    fun save(session: TeamSession?, server: ServerEnvironment) {
        if (session == null) {
            prefs.edit { remove(server.key) }
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.doFinal(ApiClient.json.encodeToString(session).toByteArray())
        val encoded = listOf(cipher.iv, data).joinToString(":") { Base64.encodeToString(it, Base64.NO_WRAP) }
        prefs.edit { putString(server.key, encoded) }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "boccia-session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
