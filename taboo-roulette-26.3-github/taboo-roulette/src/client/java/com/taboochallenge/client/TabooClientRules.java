package com.taboochallenge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;

import com.taboochallenge.Taboo;
import com.taboochallenge.TabooRules;
import com.taboochallenge.TabooState;

/**
 * 클라이언트에서만 가능한 차단: 키 입력, 휘두르기 모션.
 */
public final class TabooClientRules {
	private TabooClientRules() {
	}

	static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return;

		TabooRules.applyPlayerRules(player);
	}

	/**
	 * 공격 키를 누르고 있는 동안 매 틱 불린다. true 를 돌려주면 휘두르기 모션까지 통째로 취소되고,
	 * 캐던 중이던 블록도 Fabric 이 즉시 멈춘다(채굴 금지가 걸리는 순간 캐던 블록 포함).
	 */
	static boolean preAttack(Minecraft client, LocalPlayer player, int clickCount) {
		HitResult hit = client.hitResult;
		if (hit == null) return false;

		return switch (hit.getType()) {
			case ENTITY -> TabooState.isLocked(Taboo.ATTACK);
			case BLOCK -> TabooState.isLocked(Taboo.MINING);
			default -> false;
		};
	}

	/** 3. 거동 금지(WASD·달리기), 6. 웅크리기 금지(Shift): 해당 키가 항상 '안 눌림'으로 읽히게 한다. */
	public static boolean isKeyBlocked(KeyMapping key) {
		if (!TabooState.anyLocked()) return false;

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) return false;

		Options options = client.options;
		if (options == null) return false;

		if (TabooState.isLocked(Taboo.MOVE)
				&& (key == options.keyUp || key == options.keyDown || key == options.keyLeft
				|| key == options.keyRight || key == options.keySprint)) {
			return true;
		}

		return TabooState.isLocked(Taboo.SNEAK) && key == options.keyShift;
	}
}
