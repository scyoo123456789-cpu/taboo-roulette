package com.taboochallenge.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.KeyMapping;

import com.taboochallenge.client.TabooClientRules;

/**
 * 거동·웅크리기 금지: 이동 키와 Shift 키가 눌려 있어도 게임에는 '안 눌림'으로 전달된다.
 * 웅크리기 토글 모드를 켜 둔 상태에서도 동일하게 막힌다.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
	@Inject(method = "isDown()Z", at = @At("HEAD"), cancellable = true)
	private void tabooRoulette$blockLockedKeys(CallbackInfoReturnable<Boolean> cir) {
		if (TabooClientRules.isKeyBlocked((KeyMapping) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
