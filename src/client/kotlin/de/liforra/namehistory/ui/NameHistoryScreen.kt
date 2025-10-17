package de.liforra.namehistory.ui

import com.mojang.blaze3d.systems.RenderSystem
import de.liforra.namehistory.api.PlayerHistoryApi
import de.liforra.namehistory.config.ModConfig
import de.liforra.namehistory.model.ProfileHistory
import de.liforra.namehistory.cache.SimpleCache
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NameHistoryScreen(
    private val api: PlayerHistoryApi,
    private val config: ModConfig,
    title: Text = Text.literal("Name History")
) : Screen(title) {
    private lateinit var input: TextFieldWidget
    private lateinit var queryBtn: ButtonWidget
    private lateinit var updateBtn: ButtonWidget
    private lateinit var deleteBtn: ButtonWidget

    private var statusLine: String = ""
    private var result: ProfileHistory? = null
    private val cache = SimpleCache<ProfileHistory>(ttlMillis = 10 * 60 * 1000L)

    private val uiScope = CoroutineScope(Dispatchers.Main + Job())

    override fun init() {
        val w = 200
        input = TextFieldWidget(textRenderer, (width - w) / 2, height / 3, w, 20, Text.literal("username or uuid"))
        addSelectableChild(input)

        queryBtn = ButtonWidget.builder(Text.literal("Query")) {
            val q = input.text.trim()
            runQuery(q)
        }.dimensions((width - w) / 2, input.y + 30, 60, 20).build()

        updateBtn = ButtonWidget.builder(Text.literal("Update")) {
            val q = input.text.trim()
            runUpdate(q)
        }.dimensions(queryBtn.x + 70, queryBtn.y, 60, 20).build()

        deleteBtn = ButtonWidget.builder(Text.literal("Delete")) {
            val q = input.text.trim()
            runDelete(q)
        }.dimensions(updateBtn.x + 70, queryBtn.y, 60, 20).build()

        addDrawableChild(queryBtn)
        addDrawableChild(updateBtn)
        addDrawableChild(deleteBtn)
    }

    private fun runQuery(q: String) {
        if (q.isEmpty()) return
        cache.get(q)?.let {
            result = it
            statusLine = "Cached"
            return
        }
        statusLine = "Querying..."
        result = null
        uiScope.launch {
            val res = if (q.contains('-') || q.length >= 32) api.getByUuid(q) else api.getByUsername(q)
            res.onSuccess { ph ->
                cache.put(q, ph)
                result = ph
                statusLine = "OK"
            }.onFailure { e ->
                statusLine = when (e) {
                    is de.liforra.namehistory.api.HttpException -> {
                        when (e.status) {
                            429 -> "Too many requests. Please wait and try again."
                            502 -> "API unavailable: Bad Gateway (502)"
                            else -> "HTTP ${e.status}: ${e.message ?: e.toString()}"
                        }
                    }
                    else -> e.message ?: e.toString()
                }
            }
        }
    }

    private fun runUpdate(q: String) {
        if (q.isEmpty()) return
        statusLine = "Updating..."
        uiScope.launch {
            val body = de.liforra.namehistory.model.UpdateRequest(
                username = if (q.contains('-') || q.length >= 32) null else q,
                uuid = if (q.contains('-') || q.length >= 32) q else null
            )
            val res = api.update(body)
            res.onSuccess { statusLine = "Updated ${it.updated.size}, errors ${it.errors.size}" }
                .onFailure { e ->
                    statusLine = when (e) {
                        is de.liforra.namehistory.api.HttpException -> {
                            when (e.status) {
                                429 -> "Too many requests. Please wait and try again."
                                502 -> "API unavailable: Bad Gateway (502)"
                                else -> "HTTP ${e.status}: ${e.message ?: e.toString()}"
                            }
                        }
                        else -> e.message ?: e.toString()
                    }
                }
        }
    }

    private fun runDelete(q: String) {
        if (q.isEmpty()) return
        statusLine = "Deleting..."
        uiScope.launch {
            val res = if (q.contains('-') || q.length >= 32) api.delete(uuid = q) else api.delete(username = q)
            res.onSuccess { statusLine = "Deleted" }
                .onFailure { e ->
                    statusLine = when (e) {
                        is de.liforra.namehistory.api.HttpException -> {
                            when (e.status) {
                                429 -> "Too many requests. Please wait and try again."
                                502 -> "API unavailable: Bad Gateway (502)"
                                else -> "HTTP ${e.status}: ${e.message ?: e.toString()}"
                            }
                        }
                        else -> e.message ?: e.toString()
                    }
                }
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Draw a simple dim background to avoid blur issues with other mods
        context.fill(0, 0, width, height, 0xAA000000.toInt())
        input.render(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        val y = input.y + 60
        context.drawText(textRenderer, statusLine, (width - textRenderer.getWidth(statusLine)) / 2, y, parseColor(config.primaryColor), false)

        val r = result
        if (r != null) {
            var lineY = y + 15
            val header = "${r.query} (${r.uuid})"
            context.drawText(textRenderer, header, (width - textRenderer.getWidth(header)) / 2, lineY, parseColor(config.secondaryColor), false)
            lineY += 12
            if (r.history.isEmpty()) {
                context.drawText(textRenderer, "No entries (retried once).", (width - 200) / 2, lineY, parseColor(config.disabledColor), false)
            } else {
                for (e in r.history) {
                    val s = "#${e.id} ${e.name}  changed=${e.changedAt ?: "original"} observed=${e.observedAt}${if (e.censored) "  [censored]" else ""}"
                    context.drawText(textRenderer, s, (width - 340) / 2, lineY, parseColor(config.specialColor), false)
                    lineY += 10
                    if (lineY > height - 20) break
                }
            }
        }
    }

    private fun parseColor(hex: String): Int {
        return try {
            val cleaned = hex.trim().removePrefix("#")
            (0xFF000000 or cleaned.toLong(16)).toInt()
        } catch (e: Exception) {
            0xFFFFFFFF.toInt() // fallback to white
        }
    }
}


