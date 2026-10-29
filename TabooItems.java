package com.taboochallenge;

import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 아이템이 어떤 금기에 해당하는지 판별한다.
 * 버전마다 바뀌기 쉬운 아이템 클래스 대신 아이템 ID(minecraft:xxx)로 구분한다.
 */
public final class TabooItems {
	/** 쏘거나 던지는 아이템. */
	private static final Set<String> PROJECTILES = Set.of(
			"bow", "crossbow", "trident",
			"snowball", "egg", "blue_egg", "brown_egg",
			"ender_pearl", "ender_eye",
			"splash_potion", "lingering_potion", "experience_bottle",
			"fishing_rod", "wind_charge", "firework_rocket"
	);

	/** 블록에 대고 우클릭해서 무언가를 월드에 놓는 아이템(블록 아이템 제외). */
	private static final Set<String> USE_ON_PLACERS = Set.of(
			"armor_stand", "end_crystal", "painting", "item_frame", "glow_item_frame",
			"flint_and_steel", "fire_charge"
	);

	private TabooItems() {
	}

	public static String itemPath(ItemStack stack) {
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id == null ? "" : id.getPath();
	}

	public static String blockPath(BlockState state) {
		var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return id == null ? "" : id.getPath();
	}

	public static boolean isFood(ItemStack stack) {
		return stack.has(DataComponents.FOOD);
	}

	public static boolean isShield(ItemStack stack) {
		return itemPath(stack).equals("shield");
	}

	public static boolean isProjectile(ItemStack stack) {
		return PROJECTILES.contains(itemPath(stack));
	}

	public static boolean isCake(BlockState state) {
		String path = blockPath(state);
		return path.equals("cake") || path.endsWith("candle_cake");
	}

	/** 허공/물에 대고 우클릭(use)해서 설치되는 것: 양동이 물·용암, 보트, 수련잎, 스폰알 등. */
	public static boolean placesOnUse(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem) return true;

		String path = itemPath(stack);
		return isFluidBucket(path) || isBoat(path) || path.endsWith("_spawn_egg");
	}

	/** 블록에 대고 우클릭(useOn)해서 설치되는 것: 모든 블록, 갑옷 거치대, 엔드 수정, 그림, 불, 엔더의 눈(차원문 틀) 등. */
	public static boolean placesOnUseOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();

		if (stack.getItem() instanceof BlockItem) return true;

		String path = itemPath(stack);

		if (USE_ON_PLACERS.contains(path) || path.endsWith("minecart") || path.endsWith("_spawn_egg")
				|| isBoat(path) || isFluidBucket(path)) {
			return true;
		}

		if (path.equals("ender_eye")) {
			return blockPath(context.getLevel().getBlockState(context.getClickedPos())).equals("end_portal_frame");
		}

		return false;
	}

	private static boolean isFluidBucket(String path) {
		return path.endsWith("_bucket") && !path.equals("milk_bucket");
	}

	private static boolean isBoat(String path) {
		return path.endsWith("_boat") || path.endsWith("_raft");
	}
}
