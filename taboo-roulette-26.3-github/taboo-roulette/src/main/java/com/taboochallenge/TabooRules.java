package com.taboochallenge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

/**
 * 금기별 차단 규칙. 여기 등록하는 이벤트는 클라이언트와 서버 양쪽에서 모두 불리므로,
 * 화면상 예측(모션)과 실제 결과가 동시에 막힌다.
 *
 * <ul>
 *   <li>거동·웅크리기: 클라이언트 키 입력 자체를 막음 (client/mixin/KeyMappingMixin)</li>
 *   <li>피 회복: LivingEntity#heal 차단 (mixin/LivingEntityMixin)</li>
 * </ul>
 */
public final class TabooRules {
	private TabooRules() {
	}

	static void register() {
		// 1. 채굴 금지: 블록을 때리기 시작하는 것 자체와, 실제로 부서지는 것을 둘 다 막는다.
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				TabooState.isLocked(Taboo.MINING) ? InteractionResult.FAIL : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				!TabooState.isLocked(Taboo.MINING));

		// 2. 공격 금지
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
				TabooState.isLocked(Taboo.ATTACK) ? InteractionResult.FAIL : InteractionResult.PASS);

		// 5·7·11·12. 방패 / 음식 / 설치(양동이·보트 등) / 투사체: 아이템 사용 시작 자체를 막는다.
		UseItemCallback.EVENT.register((player, level, hand) ->
				isUseBlocked(player.getItemInHand(hand)) ? InteractionResult.FAIL : InteractionResult.PASS);

		// 11·12. 블록에 대고 쓰는 아이템: 블록 설치, 엔드 수정, 불 붙이기, 폭죽 발사 등
		ItemEvents.USE_ON.register(context -> {
			if (TabooState.isLocked(Taboo.PLACE) && TabooItems.placesOnUseOn(context)) return InteractionResult.PASS;
			if (TabooState.isLocked(Taboo.PROJECTILE) && TabooItems.isProjectile(context.getItemInHand())) return InteractionResult.PASS;
			return null;
		});

		// 8. 블록간 상호작용 금지 (+ 음식 금지일 때 케이크 먹기)
		//    블록 쪽 기능만 건너뛰고 손에 든 아이템 사용(블록 설치 등)은 그대로 진행된다.
		BlockEvents.USE_ITEM_ON.register((stack, state, level, pos, player, hand, hitResult) -> blockUseResult(state));
		BlockEvents.USE_WITHOUT_ITEM.register((state, level, pos, player, hitResult) -> blockUseResult(state));

		// 9. 부활 금지: 죽은 뒤 리스폰하면 관전 모드로 전환
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive && TabooState.isLocked(Taboo.RESPAWN)) {
				TabooManager.onForbiddenRespawn(newPlayer);
			}
		});
	}

	private static InteractionResult blockUseResult(BlockState state) {
		if (TabooState.isLocked(Taboo.BLOCK_INTERACT)) return InteractionResult.PASS;
		if (TabooState.isLocked(Taboo.FOOD) && TabooItems.isCake(state)) return InteractionResult.PASS;
		return null;
	}

	public static boolean isUseBlocked(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (TabooState.isLocked(Taboo.FOOD) && TabooItems.isFood(stack)) return true;
		if (TabooState.isLocked(Taboo.SHIELD) && TabooItems.isShield(stack)) return true;
		if (TabooState.isLocked(Taboo.PROJECTILE) && TabooItems.isProjectile(stack)) return true;
		return TabooState.isLocked(Taboo.PLACE) && TabooItems.placesOnUse(stack);
	}

	/** 금기가 걸리는 순간 이미 먹는 중/방패를 든 중/활을 당기는 중이었다면 강제로 내려놓게 한다. */
	public static boolean shouldStopUsing(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (TabooState.isLocked(Taboo.FOOD) && TabooItems.isFood(stack)) return true;
		if (TabooState.isLocked(Taboo.SHIELD) && TabooItems.isShield(stack)) return true;
		return TabooState.isLocked(Taboo.PROJECTILE) && TabooItems.isProjectile(stack);
	}

	/** 매 틱 플레이어에게 적용 (서버·클라이언트 공용). */
	public static void applyPlayerRules(Player player) {
		applyJump(player);

		if (player.isUsingItem() && shouldStopUsing(player.getUseItem())) {
			player.stopUsingItem();
		}

		if (TabooState.isLocked(Taboo.SNEAK) && player.isShiftKeyDown()) {
			player.setShiftKeyDown(false);
		}
	}

	/** 4. 점프 금지: 점프력을 0으로. 물속에서 위로 헤엄치는 것은 점프가 아니므로 그대로 가능. */
	private static void applyJump(Player player) {
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump == null) return;

		if (TabooState.isLocked(Taboo.JUMP)) {
			if (jump.getBaseValue() != 0.0) jump.setBaseValue(0.0);
		} else if (jump.getBaseValue() == 0.0) {
			jump.setBaseValue(Attributes.JUMP_STRENGTH.value().getDefaultValue());
		}
	}

	static void serverTick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			applyPlayerRules(player);
		}
	}
}
