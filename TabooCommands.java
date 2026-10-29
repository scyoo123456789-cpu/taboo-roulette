package com.taboochallenge;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * /taboo start | pause | resume | spin | reset | status
 * 싱글플레이에서 치트 허용 여부와 상관없이 쓸 수 있도록 권한 제한을 두지 않았다.
 */
public final class TabooCommands {
	private TabooCommands() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
				Commands.literal("taboo")
						.executes(context -> reply(context, TabooManager.status()))
						.then(Commands.literal("start").executes(context -> reply(context, TabooManager.start())))
						.then(Commands.literal("pause").executes(context -> reply(context, TabooManager.pause())))
						.then(Commands.literal("resume").executes(context -> reply(context, TabooManager.resume())))
						.then(Commands.literal("spin").executes(context -> reply(context, TabooManager.spin(context.getSource().getServer()))))
						.then(Commands.literal("reset").executes(context -> reply(context, TabooManager.reset())))
						.then(Commands.literal("status").executes(context -> reply(context, TabooManager.status())))
		));
	}

	private static int reply(CommandContext<CommandSourceStack> context, String message) {
		context.getSource().sendSuccess(() -> Component.literal(message), false);
		return 1;
	}
}
