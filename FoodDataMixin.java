package com.taboochallenge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.food.FoodData;

import com.taboochallenge.HealGuard;

/**
 * 바닐라 자연회복은 heal() 직후 허기를 소모한다. 회복이 막혔는데 허기만 줄어드는 일이 없도록 그 1회를 취소한다.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@Inject(method = "addExhaustion(F)V", at = @At("HEAD"), cancellable = true)
	private void tabooRoulette$skipBlockedRegenCost(CallbackInfo ci) {
		if (HealGuard.consume()) {
			ci.cancel();
		}
	}
}
