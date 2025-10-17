package de.liforra.namehistory.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileHistory(
    val query: String,
    val uuid: String,
    @SerialName("last_seen_at") val lastSeenAt: String,
    val history: List<NameEntry>
)

@Serializable
data class NameEntry(
    val id: Int,
    val name: String,
    @SerialName("changed_at") val changedAt: String?,
    @SerialName("observed_at") val observedAt: String,
    val censored: Boolean
)

@Serializable
data class ApiError(
    val code: Int,
    val name: String,
    val description: String
)

@Serializable
data class UpdateRequest(
    val username: String? = null,
    val uuid: String? = null,
    val usernames: List<String>? = null,
    val uuids: List<String>? = null
)

@Serializable
data class UpdateResult(
    val updated: List<ProfileHistory> = emptyList(),
    val errors: List<UpdateError> = emptyList()
)

@Serializable
data class UpdateError(
    val username: String? = null,
    val uuid: String? = null,
    val error: String
)


