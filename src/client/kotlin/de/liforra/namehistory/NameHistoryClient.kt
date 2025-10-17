package de.liforra.namehistory

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import de.liforra.namehistory.command.NameHistoryCommands

object NameHistoryClient : ClientModInitializer {
	override fun onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
			NameHistoryCommands.register(dispatcher)
		})
	}
}