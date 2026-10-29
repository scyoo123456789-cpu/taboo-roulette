package com.taboochallenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class TabooMod implements ModInitializer {
	public static final String MOD_ID = "taboo_roulette";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		TabooRules.register();
		TabooCommands.register();

		ServerLifecycleEvents.SERVER_STARTED.register(TabooManager::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(TabooManager::onServerStopping);
		ServerLifecycleEvents.SERVER_STOPPED.register(TabooManager::onServerStopped);
		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> TabooManager.onAfterSave(server));
		ServerTickEvents.END_SERVER_TICK.register(TabooManager::tick);

		LOGGER.info("금기 룰렛 로드 완료");
	}
}
