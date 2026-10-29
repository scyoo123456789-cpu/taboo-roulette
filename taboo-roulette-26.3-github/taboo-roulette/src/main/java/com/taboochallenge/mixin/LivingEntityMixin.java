package com.taboochallenge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.taboochallenge.HealGuard;
import com.taboochallenge.Taboo;
import com.taboochallenge.TabooState;

/**
 * 10. 피 회복 금지: 자연회복, 재생 효과, 즉시 치유, 황금사과 재생 등 모든 회복은 heal() 을 거치므로 여기서 막는다.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "heal(F)V", at = @At("HEAD"), cancellable = true)
	private void tabooRoulette$blockHeal(CallbackInfo ci) {
		if ((Object) this instanceof Player && TabooState.isLocked(Taboo.HEAL)) {
			HealGuard.markBlocked();
			ci.cancel();
		}
	}
}
