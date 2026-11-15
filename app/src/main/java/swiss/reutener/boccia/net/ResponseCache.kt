package swiss.reutener.boccia.net

import java.io.File

/** Last-known API responses on disk, so the app opens instantly and still shows something offline. */
class ResponseCache(private val cacheDir: File) {

    fun read(key: String, server: ServerEnvironment): String? =
        file(key, server).takeIf { it.exists() }?.runCatching { readText() }?.getOrNull()

    fun write(body: String, key: String, server: ServerEnvironment) {
        val file = file(key, server)
        runCatching {
            file.parentFile?.mkdirs()
            // Written next to it first, so a crash never leaves half a file behind.
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(body)
            temp.renameTo(file)
        }
    }

    private fun file(key: String, server: ServerEnvironment) = File(cacheDir, "api/${server.key}/$key.json")
}
