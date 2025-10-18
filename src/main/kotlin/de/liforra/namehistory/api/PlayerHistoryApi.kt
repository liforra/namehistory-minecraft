package de.liforra.namehistory.api

import de.liforra.namehistory.config.ModConfig
import de.liforra.namehistory.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class PlayerHistoryApi(private val config: ModConfig) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun Request.Builder.applyAuth(): Request.Builder {
        val key = config.apiKey
        if (key.isNotEmpty()) {
            header(config.apiKeyHeader, key)
        }
        return this
    }

    suspend fun getByUsername(username: String, retryOnEmpty: Boolean = true): Result<ProfileHistory> {
        val params = listOf(
            "username=${username}",
            "source=Minecraft",
            "req_name=${username}",
            "version=${config.version}"
        ).joinToString("&")
        return getWithRetry("/api/namehistory?$params", retryOnEmpty)
    }

    suspend fun getByUuid(uuid: String, retryOnEmpty: Boolean = true): Result<ProfileHistory> =
        getWithRetry("/api/namehistory/uuid/${uuid}", retryOnEmpty)

    suspend fun delete(username: String? = null, uuid: String? = null): Result<Unit> {
        val baseParams = listOf(
            "source=Minecraft",
            "version=${config.version}"
        )

        val query = when {
            username != null -> {
                val allParams = baseParams + listOf("username=${username}", "req_name=${username}")
                "?${allParams.joinToString("&")}"
            }
            uuid != null -> {
                val allParams = baseParams + "uuid=${uuid}"
                "?${allParams.joinToString("&")}"
            }
            else -> return Result.failure(IllegalArgumentException("username or uuid required"))
        }
        val url = config.baseUrl.trimEnd('/') + "/api/namehistory" + query
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(url).delete().applyAuth().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) return@runCatching Unit
                    val body = resp.body?.string().orEmpty()
                    throw HttpException(resp.code, body)
                }
            }
        }
    }

    suspend fun update(payload: UpdateRequest): Result<UpdateResult> {
        val params = listOf(
            "source=Minecraft",
            "version=${config.version}"
        ).joinToString("&")
        return post("/api/namehistory/update?$params", json.encodeToString(UpdateRequest.serializer(), payload))
    }

    private suspend inline fun <reified T> get(path: String): Result<T> {
        val url = config.baseUrl.trimEnd('/') + path
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(url).get().applyAuth().build()
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) return@runCatching json.decodeFromString<T>(text)
                    throw HttpException(resp.code, text)
                }
            }
        }
    }

    private suspend inline fun <reified T> getWithRetry(path: String, retryOnEmpty: Boolean): Result<T> {
        val first = get<T>(path)
        if (!retryOnEmpty) return first
        return first.fold(
            onSuccess = { value ->
                val empty = when (value) {
                    is ProfileHistory -> value.history.isEmpty()
                    else -> false
                }
                if (empty) get<T>(path) else Result.success(value)
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend inline fun <reified T> post(path: String, body: String): Result<T> {
        val url = config.baseUrl.trimEnd('/') + path
        val requestBody: RequestBody = body.toRequestBody(jsonMedia)
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(url).post(requestBody).applyAuth().build()
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) return@runCatching json.decodeFromString<T>(text)
                    throw HttpException(resp.code, text)
                }
            }
        }
    }
}

class HttpException(val status: Int, val body: String) : RuntimeException("HTTP $status: $body")


