package de.liforra.namehistory.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ModConfig(
    val baseUrl: String = "https://liforra.de/api/namehistory",
    val requestTimeoutMs: Long = 8000,
    val cacheTtlMinutes: Long = 10,
    val apiKey: String = "",
    val apiKeyHeader: String = "X-API-Key",
    // UI/Chat color configuration (hex strings, e.g. #RRGGBB)
    val primaryColor: String = "#9b59d0",
    val secondaryColor: String = "#b388ff",
    val specialColor: String = "#ce93d8",
    val errorColor: String = "#ff6090",
    val disabledColor: String = "#9e9e9e"
)

object ConfigManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun configFile(runDir: File): File {
        val cfgDir = File(runDir, "config")
        if (!cfgDir.exists()) cfgDir.mkdirs()
        return File(cfgDir, "namehistory-client.json")
    }

    fun loadOrDefault(runDir: File): ModConfig {
        val file = configFile(runDir)
        if (!file.exists()) return ModConfig().also { save(runDir, it) }
        return try {
            json.decodeFromString(ModConfig.serializer(), file.readText())
        } catch (_: Exception) {
            ModConfig().also { save(runDir, it) }
        }
    }

    fun save(runDir: File, config: ModConfig) {
        val file = configFile(runDir)
        file.writeText(json.encodeToString(ModConfig.serializer(), config))
    }
}


