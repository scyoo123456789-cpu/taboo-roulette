package com.taboochallenge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * 서버 쪽 진행 관리: 5분 타이머, 룰렛 추첨, 금기 확정, 저장/불러오기.
 * 모든 메서드는 서버 스레드에서만 호출된다.
 */
public final class TabooManager {
	/** 금기 간격: 5분 */
	public static final int INTERVAL_TICKS = 5 * 60 * 20;

	private static final String FILE_NAME = "taboo_roulette.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Random RANDOM = new Random();

	private static final int COLOR_RED = 0xFF5555;
	private static final int COLOR_GOLD = 0xFFC94A;
	private static final int COLOR_WHITE = 0xFFFFFF;

	private static boolean running;
	private static boolean paused;
	private static boolean failed;
	private static final List<Taboo> locked = new ArrayList<>();
	private static int ticksLeft = INTERVAL_TICKS;

	private static List<Taboo> reel;
	private static Taboo rouletteResult;
	private static int rouletteTick = -1;
	private static int lastRow = Integer.MIN_VALUE;

	private static MinecraftServer currentServer;
	private static Path saveFile;
	private static int ticksSinceSave;

	private TabooManager() {
	}

	// ---------------------------------------------------------------- 수명 주기

	static void onServerStarted(MinecraftServer server) {
		resetFields();
		currentServer = server;
		saveFile = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
		load();
		publish();
	}

	static void onServerStopping(MinecraftServer server) {
		save();
	}

	static void onServerStopped(MinecraftServer server) {
		resetFields();
		currentServer = null;
		saveFile = null;
		TabooState.clear();
	}

	static void onAfterSave(MinecraftServer server) {
		save();
	}

	private static void resetFields() {
		running = false;
		paused = false;
		failed = false;
		locked.clear();
		ticksLeft = INTERVAL_TICKS;
		clearRoulette();
		ticksSinceSave = 0;
	}

	private static void clearRoulette() {
		reel = null;
		rouletteResult = null;
		rouletteTick = -1;
		lastRow = Integer.MIN_VALUE;
	}

	// ---------------------------------------------------------------- 매 틱

	static void tick(MinecraftServer server) {
		if (saveFile == null) return;

		if (!paused) {
			if (rouletteTick >= 0) {
				advanceRoulette(server);
			}

			if (running) {
				if (pendingCount() < Taboo.COUNT) {
					ticksLeft--;

					if (ticksLeft <= 0) {
						if (rouletteTick >= 0) {
							ticksLeft = 1; // 수동 룰렛이 아직 도는 중이면 끝날 때까지 기다렸다가 바로 돌린다
						} else {
							startRoulette(server);
							ticksLeft = INTERVAL_TICKS;
						}
					}
				} else {
					ticksLeft = 0;
				}
			}
		}

		TabooRules.serverTick(server);
		HealGuard.reset();
		publish();

		if (++ticksSinceSave >= 200) {
			save();
		}
	}

	private static int pendingCount() {
		boolean spinning = rouletteTick >= 0 && rouletteResult != null && !locked.contains(rouletteResult);
		return locked.size() + (spinning ? 1 : 0);
	}

	private static boolean startRoulette(MinecraftServer server) {
		if (rouletteTick >= 0) return false;

		List<Taboo> remaining = new ArrayList<>();

		for (Taboo taboo : Taboo.values()) {
			if (!locked.contains(taboo)) remaining.add(taboo);
		}

		if (remaining.isEmpty()) return false;

		Collections.shuffle(remaining, RANDOM);
		Taboo result = remaining.get(RANDOM.nextInt(remaining.size()));

		reel = RouletteAnim.buildReel(remaining, result);
		rouletteResult = result;
		rouletteTick = 0;
		lastRow = Integer.MIN_VALUE;

		playSound(server, SoundEvents.BELL_BLOCK, 1.0f, 1.0f);
		save();
		return true;
	}

	private static void advanceRoulette(MinecraftServer server) {
		rouletteTick++;

		if (rouletteTick < RouletteAnim.SPIN_TICKS) {
			int row = RouletteAnim.centeredRow(rouletteTick);

			if (row != lastRow) {
				lastRow = row;
				float progress = rouletteTick / (float) RouletteAnim.SPIN_TICKS;
				playSound(server, SoundEvents.NOTE_BLOCK_HAT, 0.8f, 0.9f + 0.7f * progress);
			}
		} else if (rouletteTick == RouletteAnim.SPIN_TICKS) {
			lockTaboo(server, rouletteResult);
		} else if (rouletteTick >= RouletteAnim.TOTAL_TICKS) {
			clearRoulette();
		}
	}

	private static void lockTaboo(MinecraftServer server, Taboo taboo) {
		if (taboo == null) return;
		if (!locked.contains(taboo)) locked.add(taboo);

		int count = locked.size();
		broadcast(server, Component.literal("[금기 " + count + "/" + Taboo.COUNT + "] ").withColor(COLOR_RED)
				.append(Component.literal(taboo.title).withColor(COLOR_GOLD))
				.append(Component.literal(" — " + taboo.description).withColor(COLOR_WHITE)));
		playSound(server, SoundEvents.ANVIL_LAND, 0.8f, 0.7f);

		if (count >= Taboo.COUNT) {
			broadcast(server, Component.literal("모든 금기가 활성화되었습니다. 행운을 빕니다!").withColor(COLOR_RED));
			playSound(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
		}

		save();
	}

	// ---------------------------------------------------------------- 부활 금지

	static void onForbiddenRespawn(ServerPlayer player) {
		player.setGameMode(GameType.SPECTATOR);

		if (!failed) {
			failed = true;
			running = false;
			paused = false;
			broadcast(currentServer, Component.literal("부활 금지! " + player.getName().getString()
					+ " 님은 관전 모드로 전환됩니다. 챌린지 실패.").withColor(COLOR_RED));
			save();
		}

		publish();
	}

	// ---------------------------------------------------------------- 명령어

	static String start() {
		if (failed) return "챌린지가 실패 상태입니다. /taboo reset 후 다시 시작하세요.";
		if (locked.size() >= Taboo.COUNT) return "모든 금기가 이미 활성화되었습니다. /taboo reset 으로 초기화하세요.";
		if (running) return paused ? "일시정지 상태입니다. /taboo resume 으로 이어서 진행하세요." : "이미 진행 중입니다.";

		running = true;
		paused = false;

		if (ticksLeft <= 0 || ticksLeft > INTERVAL_TICKS) ticksLeft = INTERVAL_TICKS;

		save();
		publish();
		return "금기 룰렛 시작! 5분마다 금기가 하나씩 영구적으로 추가됩니다.";
	}

	static String pause() {
		if (!running) return "진행 중이 아닙니다.";
		if (paused) return "이미 일시정지 상태입니다.";

		paused = true;
		save();
		publish();
		return "일시정지했습니다. /taboo resume 으로 이어서 진행합니다.";
	}

	static String resume() {
		if (!running) return "진행 중이 아닙니다. /taboo start 로 시작하세요.";
		if (!paused) return "일시정지 상태가 아닙니다.";

		paused = false;
		save();
		publish();
		return "다시 진행합니다.";
	}

	static String spin(MinecraftServer server) {
		if (failed) return "챌린지가 실패 상태입니다. /taboo reset 후 다시 시작하세요.";
		if (paused) return "일시정지 상태에서는 룰렛을 돌릴 수 없습니다.";
		if (rouletteTick >= 0) return "룰렛이 이미 돌아가는 중입니다.";
		if (!startRoulette(server)) return "남은 금기가 없습니다.";

		if (running) ticksLeft = INTERVAL_TICKS;

		publish();
		return "룰렛을 바로 돌립니다!";
	}

	static String reset() {
		resetFields();
		save();
		publish();
		return "초기화했습니다. /taboo start 로 다시 시작하세요. (관전 모드였다면 /gamemode survival 로 되돌리세요)";
	}

	static String status() {
		StringBuilder sb = new StringBuilder();

		if (failed) sb.append("상태: 실패");
		else if (!running) sb.append("상태: 대기 중");
		else if (paused) sb.append("상태: 일시정지");
		else sb.append("상태: 진행 중");

		sb.append(" | 금기 ").append(locked.size()).append("/").append(Taboo.COUNT);

		if (running && locked.size() < Taboo.COUNT) {
			int seconds = (ticksLeft + 19) / 20;
			sb.append(" | 다음 금기까지 ").append(String.format("%02d:%02d", seconds / 60, seconds % 60));
		}

		if (!locked.isEmpty()) {
			sb.append("\n활성 금기: ");

			for (int i = 0; i < locked.size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(locked.get(i).title);
			}
		}

		return sb.toString();
	}

	// ---------------------------------------------------------------- 공유 상태

	private static void publish() {
		TabooState.Roulette roulette = rouletteTick >= 0 && reel != null && rouletteResult != null
				? new TabooState.Roulette(reel, rouletteResult, rouletteTick)
				: null;

		TabooState.publish(new TabooState.Snapshot(
				true, running, running && paused, failed, List.copyOf(locked), ticksLeft, roulette, System.nanoTime()));
	}

	// ---------------------------------------------------------------- 저장 / 불러오기

	private static void save() {
		ticksSinceSave = 0;
		if (saveFile == null) return;

		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		root.addProperty("running", running);
		root.addProperty("paused", paused);
		root.addProperty("failed", failed);
		root.addProperty("ticksLeft", ticksLeft);

		JsonArray lockedJson = new JsonArray();

		for (Taboo taboo : locked) {
			lockedJson.add(taboo.name());
		}

		root.add("locked", lockedJson);

		// 룰렛이 도는 도중에 저장되면, 다시 불러올 때 그 결과를 바로 확정한다.
		if (rouletteTick >= 0 && rouletteResult != null && !locked.contains(rouletteResult)) {
			root.addProperty("pending", rouletteResult.name());
		}

		try {
			Files.writeString(saveFile, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			TabooMod.LOGGER.warn("금기 룰렛 상태를 저장하지 못했습니다: {}", saveFile, e);
		}
	}

	private static void load() {
		if (saveFile == null || !Files.isRegularFile(saveFile)) return;

		try {
			JsonObject root = JsonParser.parseString(Files.readString(saveFile, StandardCharsets.UTF_8)).getAsJsonObject();

			running = root.has("running") && root.get("running").getAsBoolean();
			paused = root.has("paused") && root.get("paused").getAsBoolean();
			failed = root.has("failed") && root.get("failed").getAsBoolean();
			ticksLeft = root.has("ticksLeft") ? root.get("ticksLeft").getAsInt() : INTERVAL_TICKS;

			if (ticksLeft <= 0 || ticksLeft > INTERVAL_TICKS) ticksLeft = INTERVAL_TICKS;

			if (root.has("locked")) {
				for (JsonElement element : root.getAsJsonArray("locked")) {
					Taboo taboo = Taboo.byName(element.getAsString());
					if (taboo != null && !locked.contains(taboo)) locked.add(taboo);
				}
			}

			if (root.has("pending")) {
				Taboo taboo = Taboo.byName(root.get("pending").getAsString());
				if (taboo != null && !locked.contains(taboo)) locked.add(taboo);
			}

			if (!running) paused = false;
		} catch (Exception e) {
			TabooMod.LOGGER.warn("금기 룰렛 상태 파일을 읽지 못했습니다: {}", saveFile, e);
		}
	}

	// ---------------------------------------------------------------- 유틸

	private static void broadcast(MinecraftServer server, Component message) {
		if (server == null) return;
		server.getPlayerList().broadcastSystemMessage(message, false);
	}

	private static void playSound(MinecraftServer server, Object sound, float volume, float pitch) {
		SoundEvent event = asSoundEvent(sound);
		if (event == null) return;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.level().playSound(null, player.blockPosition(), event, SoundSource.MASTER, volume, pitch);
		}
	}

	/** SoundEvents 상수가 SoundEvent 인지 Holder 인지 버전마다 달라서 둘 다 받는다. */
	private static SoundEvent asSoundEvent(Object sound) {
		if (sound instanceof SoundEvent event) return event;
		if (sound instanceof Holder<?> holder && holder.value() instanceof SoundEvent event) return event;
		return null;
	}
}
