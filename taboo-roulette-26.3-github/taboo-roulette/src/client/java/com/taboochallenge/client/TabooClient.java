package com.taboochallenge.client;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

import com.taboochallenge.TabooMod;

public class TabooClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 가장 위 레이어에 그린다 (오른쪽 상단 타이머 + 화면 중앙 룰렛)
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(TabooMod.MOD_ID, "hud"), TabooHud::extract);

		ClientTickEvents.END_CLIENT_TICK.register(TabooClientRules::tick);
		ClientPreAttackCallback.EVENT.register(TabooClientRules::preAttack);
	}
}
