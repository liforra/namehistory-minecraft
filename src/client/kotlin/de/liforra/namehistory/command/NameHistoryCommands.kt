package de.liforra.namehistory.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.suggestion.SuggestionProvider
import de.liforra.namehistory.api.PlayerHistoryApi
import de.liforra.namehistory.config.ConfigManager
import de.liforra.namehistory.model.UpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object NameHistoryCommands {
    private val playerSuggestions = SuggestionProvider<FabricClientCommandSource> { ctx, builder ->
        CommandSource.suggestMatching(
            ctx.source.playerNames.map { it.toString() },
            builder
        )
    }

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommandManager.literal("namehistory")
                .then(
                    ClientCommandManager.argument("query", string())
                        .suggests(playerSuggestions)
                        .executes { ctx ->
                        val src = ctx.source
                        val q = getString(ctx, "query").trim()
                        runClient(src) { client ->
                            val cfg = ConfigManager.loadOrDefault(client.runDirectory)
                            val api = PlayerHistoryApi(cfg)
                            val res = if (q.contains('-') || q.length >= 32) api.getByUuid(q) else api.getByUsername(q)
                            res.onSuccess { ph ->
                                // Header
                                src.sendFeedback(
                                    Text.literal("[NameHistory] ").styled { it.withColor(parseColor(cfg.primaryColor)) }
                                        .append(Text.literal(ph.query).styled { it.withColor(parseColor(cfg.secondaryColor)) })
                                        .append(Text.literal("  "))
                                        .append(Text.literal("(${ph.uuid})").styled { it.withColor(parseColor(cfg.disabledColor)) })
                                        .append(Text.literal("  entries: ").styled { it.withColor(parseColor(cfg.disabledColor)) })
                                        .append(Text.literal(ph.history.size.toString()).styled { it.withColor(parseColor(cfg.specialColor)) })
                                )
                                // Divider
                                src.sendFeedback(Text.literal("——————————————").styled { it.withColor(parseColor(cfg.disabledColor)) })

                                if (ph.history.isEmpty()) {
                                    src.sendFeedback(Text.literal("No entries (retried once).").styled { it.withColor(parseColor(cfg.disabledColor)) })
                                } else {
                                    val maxName = ph.history.maxOf { it.name.length }
                                    ph.history.forEach { e ->
                                        val timeStr = formatTimestamp(e.changedAt)
                                        val padSpaces = (maxName - e.name.length).coerceAtLeast(0)
                                        val spaces = " ".repeat(padSpaces + 1)
                                        val line = Text.literal("${e.id}). ").styled { it.withColor(parseColor(cfg.disabledColor)) }
                                            .append(Text.literal(e.name).styled { it.withColor(parseColor(cfg.secondaryColor)) })
                                            .append(Text.literal(spaces + "|").styled { it.withColor(parseColor(cfg.disabledColor)) })
                                            .append(Text.literal("  ").styled { it.withColor(parseColor(cfg.disabledColor)) })
                                            .append(Text.literal(timeStr).styled { it.withColor(parseColor(cfg.specialColor)) })
                                            .append(if (e.censored) Text.literal("  [censored]").styled { it.withColor(parseColor(cfg.errorColor)) } else Text.literal(""))
                                        src.sendFeedback(line)
                                    }
                                }
                            }.onFailure { e ->
                                handleError(src, client, e, q)
                            }
                        }
                        1
                    }
                )
                .then(
                    ClientCommandManager.literal("update").then(
                        ClientCommandManager.argument("query", string()).executes { ctx ->
                            val src = ctx.source
                            val q = getString(ctx, "query").trim()
                            runClient(src) { client ->
                                val cfg = ConfigManager.loadOrDefault(client.runDirectory)
                                val api = PlayerHistoryApi(cfg)
                                val body = UpdateRequest(
                                    username = if (q.contains('-') || q.length >= 32) null else q,
                                    uuid = if (q.contains('-') || q.length >= 32) q else null
                                )
                                val res = api.update(body)
                                res.onSuccess { ur -> src.sendFeedback(Text.literal("updated=${ur.updated.size} errors=${ur.errors.size}")) }
                                    .onFailure { e -> handleError(src, client, e) }
                            }
                            1
                        }
                    )
                )
                .then(
                    ClientCommandManager.literal("delete").then(
                        ClientCommandManager.argument("query", string()).executes { ctx ->
                            val src = ctx.source
                            val q = getString(ctx, "query").trim()
                            runClient(src) { client ->
                                val cfg = ConfigManager.loadOrDefault(client.runDirectory)
                                val api = PlayerHistoryApi(cfg)
                                val res = if (q.contains('-') || q.length >= 32) api.delete(uuid = q) else api.delete(username = q)
                                res.onSuccess { src.sendFeedback(Text.literal("deleted")) }
                                    .onFailure { e -> handleError(src, client, e) }
                            }
                            1
                        }
                    )
                )
        )
    }

    private fun runClient(src: FabricClientCommandSource, block: suspend (net.minecraft.client.MinecraftClient) -> Unit) {
        val client = src.client
        CoroutineScope(Dispatchers.Default).launch { block(client) }
    }

    private fun handleError(src: FabricClientCommandSource, client: net.minecraft.client.MinecraftClient, e: Throwable, query: String? = null) {
        val msg = e.message ?: e.toString()
        val cfg = ConfigManager.loadOrDefault(client.runDirectory)
        when (e) {
            is de.liforra.namehistory.api.HttpException -> {
                when (e.status) {
                    404 -> src.sendError(Text.literal(if (query != null) "User \"$query\" does not seem to have a valid Minecraft account." else "Not found").formatted(Formatting.RED))
                    429 -> src.sendError(Text.literal("Too many requests. Please wait a moment and try again.").formatted(Formatting.RED))
                    502 -> src.sendError(Text.literal("Bad Gateway: The API is currently not available. Please try again later.").formatted(Formatting.RED))
                    else -> src.sendError(Text.literal("HTTP ${e.status}").formatted(Formatting.RED))
                }
                System.err.println("[NameHistory] Request failed: ${cfg.baseUrl} details=${msg}")
            }
            else -> {
                if (msg.contains("timeout", ignoreCase = true)) {
                    src.sendError(Text.literal("Timeout contacting ${cfg.baseUrl}. Check your internet connection.").formatted(Formatting.RED))
                } else {
                    src.sendError(Text.literal("Error: ${msg}").formatted(Formatting.RED))
                }
                System.err.println("[NameHistory] Error contacting ${cfg.baseUrl}: ${msg}")
            }
        }
    }

    private fun parseColor(hex: String): Int {
        return try {
            val cleaned = hex.trim().removePrefix("#")
            cleaned.toInt(16)
        } catch (e: Exception) {
            0xFFFFFF
        }
    }

    private fun formatTimestamp(iso: String?): String {
        if (iso == null) return "original"
        return try {
            val instant = java.time.Instant.parse(iso)
            val ldt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            val dd = ldt.dayOfMonth.toString().padStart(2, '0')
            val mm = ldt.monthValue.toString().padStart(2, '0')
            val yy = (ldt.year % 100).toString().padStart(2, '0')
            val hh = ldt.hour.toString().padStart(2, '0')
            val min = ldt.minute.toString().padStart(2, '0')
            "$dd.$mm.$yy $hh:$min"
        } catch (_: Exception) {
            iso
        }
    }
}


