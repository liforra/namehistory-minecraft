package de.liforra.namehistory.config

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class ConfigScreen(private val parent: Screen?) : Screen(Text.literal("Name History Configuration")) {
    private lateinit var baseUrlField: TextFieldWidget
    private lateinit var timeoutField: TextFieldWidget
    private lateinit var cacheTtlField: TextFieldWidget
    private lateinit var apiKeyField: TextFieldWidget
    private lateinit var apiKeyHeaderField: TextFieldWidget
    private lateinit var primaryColorField: TextFieldWidget
    private lateinit var secondaryColorField: TextFieldWidget
    private lateinit var specialColorField: TextFieldWidget
    private lateinit var errorColorField: TextFieldWidget
    private lateinit var disabledColorField: TextFieldWidget

    private lateinit var saveButton: ButtonWidget
    private lateinit var resetButton: ButtonWidget
    private lateinit var cancelButton: ButtonWidget

    private var config: ModConfig = ModConfig()
    private var scrollOffset = 0
    
    // Preset color palette
    private val colorPresets = listOf(
        "#9b59d0", "#b388ff", "#ce93d8", "#7b1fa2", "#4a148c",
        "#ff6090", "#f48fb1", "#e91e63", "#c2185b", "#880e4f",
        "#64b5f6", "#42a5f5", "#2196f3", "#1976d2", "#0d47a1",
        "#4db6ac", "#26a69a", "#009688", "#00796b", "#004d40",
        "#aed581", "#9ccc65", "#8bc34a", "#689f38", "#33691e",
        "#ffb74d", "#ffa726", "#ff9800", "#f57c00", "#e65100",
        "#e0e0e0", "#bdbdbd", "#9e9e9e", "#757575", "#424242",
        "#ffffff", "#f5f5f5", "#eeeeee", "#e0e0e0", "#000000"
    )

    override fun init() {
        val client = MinecraftClient.getInstance()
        config = ConfigManager.loadOrDefault(client.runDirectory)

        val fieldWidth = 300
        val fieldX = (width - fieldWidth) / 2

        var y = 50

        // API Configuration Section
        baseUrlField = TextFieldWidget(textRenderer, fieldX, y, fieldWidth, 20, Text.literal("API Base URL"))
        baseUrlField.text = config.baseUrl
        addSelectableChild(baseUrlField)
        y += 40

        timeoutField = TextFieldWidget(textRenderer, fieldX, y, 80, 20, Text.literal("Timeout"))
        timeoutField.text = config.requestTimeoutMs.toString()
        addSelectableChild(timeoutField)
        y += 40

        cacheTtlField = TextFieldWidget(textRenderer, fieldX, y, 80, 20, Text.literal("Cache TTL"))
        cacheTtlField.text = config.cacheTtlMinutes.toString()
        addSelectableChild(cacheTtlField)
        y += 50

        // Authentication Section
        apiKeyField = TextFieldWidget(textRenderer, fieldX, y, fieldWidth, 20, Text.literal("API Key"))
        apiKeyField.text = config.apiKey
        addSelectableChild(apiKeyField)
        y += 40

        apiKeyHeaderField = TextFieldWidget(textRenderer, fieldX, y, fieldWidth, 20, Text.literal("API Key Header"))
        apiKeyHeaderField.text = config.apiKeyHeader
        addSelectableChild(apiKeyHeaderField)
        y += 55

        // Color Theme Section - add space for headers (17px)
        y += 17

        // Color Theme Section - 2 column grid layout
        val col1X = fieldX + 20
        val col2X = fieldX + 160
        val rowHeight = 38
        
        // Row 1: Primary | Secondary
        primaryColorField = TextFieldWidget(textRenderer, col1X, y, 70, 20, Text.literal("Primary"))
        primaryColorField.text = config.primaryColor
        addSelectableChild(primaryColorField)
        
        secondaryColorField = TextFieldWidget(textRenderer, col2X, y, 70, 20, Text.literal("Secondary"))
        secondaryColorField.text = config.secondaryColor
        addSelectableChild(secondaryColorField)
        y += rowHeight

        // Row 2: Special | Error
        specialColorField = TextFieldWidget(textRenderer, col1X, y, 70, 20, Text.literal("Special"))
        specialColorField.text = config.specialColor
        addSelectableChild(specialColorField)
        
        errorColorField = TextFieldWidget(textRenderer, col2X, y, 70, 20, Text.literal("Error"))
        errorColorField.text = config.errorColor
        addSelectableChild(errorColorField)
        y += rowHeight

        // Row 3: Disabled (centered or left)
        disabledColorField = TextFieldWidget(textRenderer, col1X, y, 70, 20, Text.literal("Disabled"))
        disabledColorField.text = config.disabledColor
        addSelectableChild(disabledColorField)

        // Buttons at bottom
        val buttonY = height - 35
        saveButton = ButtonWidget.builder(Text.literal("Save")) {
            saveConfig()
            client?.setScreen(parent)
        }.dimensions(width / 2 - 155, buttonY, 70, 20).build()

        resetButton = ButtonWidget.builder(Text.literal("Reset")) {
            resetToDefaults()
        }.dimensions(width / 2 - 75, buttonY, 70, 20).build()

        cancelButton = ButtonWidget.builder(Text.literal("Cancel")) {
            client?.setScreen(parent)
        }.dimensions(width / 2 + 5, buttonY, 70, 20).build()

        addDrawableChild(saveButton)
        addDrawableChild(resetButton)
        addDrawableChild(cancelButton)
    }

    private fun saveConfig() {
        val client = MinecraftClient.getInstance()
        val newConfig = ModConfig(
            baseUrl = baseUrlField.text,
            requestTimeoutMs = timeoutField.text.toLongOrNull() ?: config.requestTimeoutMs,
            cacheTtlMinutes = cacheTtlField.text.toLongOrNull() ?: config.cacheTtlMinutes,
            apiKey = apiKeyField.text,
            apiKeyHeader = apiKeyHeaderField.text,
            primaryColor = primaryColorField.text,
            secondaryColor = secondaryColorField.text,
            specialColor = specialColorField.text,
            errorColor = errorColorField.text,
            disabledColor = disabledColorField.text
        )
        ConfigManager.save(client.runDirectory, newConfig)
    }

    private fun resetToDefaults() {
        val defaults = ModConfig()
        baseUrlField.text = defaults.baseUrl
        timeoutField.text = defaults.requestTimeoutMs.toString()
        cacheTtlField.text = defaults.cacheTtlMinutes.toString()
        apiKeyField.text = defaults.apiKey
        apiKeyHeaderField.text = defaults.apiKeyHeader
        primaryColorField.text = defaults.primaryColor
        secondaryColorField.text = defaults.secondaryColor
        specialColorField.text = defaults.specialColor
        errorColorField.text = defaults.errorColor
        disabledColorField.text = defaults.disabledColor
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Simple background fill to avoid blur conflicts with other mods
        context.fill(0, 0, width, height, 0xAA000000.toInt())
        super.render(context, mouseX, mouseY, delta)

        val fieldWidth = 300
        val fieldX = (width - fieldWidth) / 2
        
        // Use config colors for UI
        val headerColor = parseColor(config.secondaryColor)
        val descColor = parseColor(config.disabledColor)
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, parseColor(config.primaryColor))

        var y = 50

        // API Configuration Section Header
        context.drawText(textRenderer, "API Configuration", fieldX, y - 15, headerColor, false)
        baseUrlField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "Base URL of your name history API server", fieldX, y + 24, descColor, false)
        y += 40

        timeoutField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "How long to wait for API responses (ms)", fieldX + 90, y + 5, descColor, false)
        y += 40

        cacheTtlField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "How long to cache results (minutes)", fieldX + 90, y + 5, descColor, false)
        y += 50

        // Authentication Section Header
        context.drawText(textRenderer, "Authentication (Optional)", fieldX, y - 15, headerColor, false)
        apiKeyField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "Leave empty if API doesn't require auth", fieldX, y + 24, descColor, false)
        y += 40

        apiKeyHeaderField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "HTTP header name for the API key", fieldX, y + 24, descColor, false)
        y += 55

        // Color Theme Section Header
        context.drawText(textRenderer, "Color Theme (Hex colors, e.g. #9b59d0)", fieldX, y - 15, headerColor, false)
        context.drawText(textRenderer, "Click color box to cycle through presets", fieldX, y, descColor, false)
        y += 17
        
        // Grid layout - 2 columns
        val col1X = fieldX
        val col2X = fieldX + 140
        val startY = y
        
        // Row 1: Primary | Secondary
        drawColorPreview(context, col1X, startY + 1, primaryColorField.text)
        primaryColorField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "Status text & UI", col1X, startY + 24, descColor, false)
        
        drawColorPreview(context, col2X, startY + 1, secondaryColorField.text)
        secondaryColorField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "Headers & names", col2X, startY + 24, descColor, false)
        
        // Row 2: Special | Error
        val row2Y = startY + 38
        drawColorPreview(context, col1X, row2Y + 1, specialColorField.text)
        specialColorField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "Name history list", col1X, row2Y + 24, descColor, false)
        
        drawColorPreview(context, col2X, row2Y + 1, errorColorField.text)
        errorColorField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "Error messages", col2X, row2Y + 24, descColor, false)
        
        // Row 3: Disabled
        val row3Y = startY + 76
        drawColorPreview(context, col1X, row3Y + 1, disabledColorField.text)
        disabledColorField.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, "Disabled elements", col1X, row3Y + 24, descColor, false)
    }

    private fun parseColor(hex: String): Int {
        return try {
            val cleaned = hex.trim().removePrefix("#")
            (0xFF000000 or cleaned.toLong(16)).toInt()
        } catch (e: Exception) {
            0xFFFFFFFF.toInt()
        }
    }

    override fun mouseClicked(click: net.minecraft.client.gui.Click, doubled: Boolean): Boolean {
        val mouseX = click.x()
        val mouseY = click.y()
        
        val fieldWidth = 300
        val fieldX = (width - fieldWidth) / 2
        val startY = 50 + 40 + 40 + 50 + 40 + 40 + 55 + 17  // Position of first color field
        
        val col1X = fieldX
        val col2X = fieldX + 140
        
        // Check if clicking on color preview boxes - grid layout
        val colorFields = listOf(
            // Row 1
            Pair(primaryColorField, Pair(col1X, startY)),
            Pair(secondaryColorField, Pair(col2X, startY)),
            // Row 2
            Pair(specialColorField, Pair(col1X, startY + 38)),
            Pair(errorColorField, Pair(col2X, startY + 38)),
            // Row 3
            Pair(disabledColorField, Pair(col1X, startY + 76))
        )
        
        for ((field, pos) in colorFields) {
            val (boxX, boxY) = pos
            if (mouseX >= boxX && mouseX <= boxX + 16 && mouseY >= boxY + 1 && mouseY <= boxY + 17) {
                cycleColor(field)
                return true
            }
        }
        
        return super.mouseClicked(click, doubled)
    }
    
    private fun cycleColor(field: TextFieldWidget) {
        val currentColor = field.text.lowercase()
        val currentIndex = colorPresets.indexOfFirst { it.lowercase() == currentColor }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % colorPresets.size else 0
        field.text = colorPresets[nextIndex]
    }

    private fun drawColorPreview(context: DrawContext, x: Int, y: Int, colorHex: String) {
        val color = parseColor(colorHex)
        context.fill(x, y, x + 16, y + 16, color)
        context.drawStrokedRectangle(x, y, 16, 16, 0xFFFFFFFF.toInt())
    }

    override fun close() {
        client?.setScreen(parent)
    }
}
