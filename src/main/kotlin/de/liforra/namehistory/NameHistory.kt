package de.liforra.namehistory

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object NameHistory : ModInitializer {
    private val logger = LoggerFactory.getLogger("name-history")

	override fun onInitialize() {
		logger.info("Name History mod initialized")
	}
}