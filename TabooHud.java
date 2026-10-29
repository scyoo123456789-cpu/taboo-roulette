package com.taboochallenge.client;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.taboochallenge.RouletteAnim;
import com.taboochallenge.Taboo;
import com.taboochallenge.TabooManager;
import com.taboochallenge.TabooState;

/**
 * 오른쪽 상단 타이머 패널 + 화면 중앙 룰렛 연출.
 */
final class TabooHud {
	private static final int PANEL_WIDTH = 150;
	private static final int ROW_HEIGHT = 18;
	private static final int VISIBLE_ROWS = 5;

	private static final int GOLD = 0xFFC94A;
	private static final int RED = 0xFF5050;
	private static final int GREEN = 0x55FF77;
	private static final int WHITE = 0xFFFFFF;
	private static final int GRAY = 0xAAAAAA;
	private static final int DARK = 0x0B0B10;

	private TabooHud() {
	}

	static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		TabooState.Snapshot state = TabooState.get();
		if (!state.active()) return;

		Font font = client.font;
		boolean hasEffects = !client.player.getActiveEffects().isEmpty();

		drawStatusPanel(graphics, font, state, hasEffects);

		if (state.roulette() != null) {
			drawRoulette(graphics, font, state);
		}
	}

	// ================================================================ 오른쪽 상단 패널

	private static void drawStatusPanel(GuiGraphicsExtractor g, Font font, TabooState.Snapshot s, boolean hasEffects) {
		int right = g.guiWidth() - 4;
		int left = right - PANEL_WIDTH;
		int top = hasEffects ? 54 : 4; // 물약 효과 아이콘과 겹치지 않게
		List<Taboo> locked = s.locked();
		int count = locked.size();

		// 아직 시작 전: 작은 안내만
		if (!s.running() && count == 0 && !s.failed() && s.roulette() == null) {
			String hint = "금기 룰렛 · /taboo start";
			int width = font.width(hint) + 12;
			g.fill(right - width, top, right, top + 17, argb(0.7f, DARK));
			g.text(font, hint, right - width + 6, top + 5, argb(1f, GRAY), true);
			return;
		}

		boolean allLocked = count >= Taboo.COUNT;
		boolean ended = s.failed() || allLocked;
		boolean lastSpin = !ended && s.roulette() != null && count + 1 >= Taboo.COUNT;
		int listTop = top + 40;
		int bottom = count > 0 ? listTop + count * 11 + 2 : top + 38;

		g.fill(left, top, right, bottom, argb(0.72f, DARK));
		g.fill(left, top, right, top + 1, argb(1f, ended ? RED : GOLD));

		// 상단: 라벨 + 큰 타이머
		String label;
		if (s.failed()) label = "챌린지 실패";
		else if (allLocked) label = "모든 금기 활성화";
		else if (lastSpin) label = "마지막 금기 추첨";
		else if (!s.running()) label = "대기 중";
		else if (s.paused()) label = "일시정지";
		else label = "다음 금기까지";

		g.text(font, label, left + 6, top + 7, argb(1f, ended || lastSpin ? RED : GRAY), true);

		String time = ended || lastSpin ? "--:--" : formatTime(s.ticksLeft());
		int seconds = (s.ticksLeft() + 19) / 20;
		int timeColor = WHITE;

		if (ended || lastSpin) {
			timeColor = RED;
		} else if (s.running() && !s.paused() && seconds <= 10) {
			timeColor = (System.currentTimeMillis() / 250L) % 2L == 0L ? RED : WHITE; // 마지막 10초 깜빡임
		} else if (s.running() && seconds <= 60) {
			timeColor = GOLD;
		}

		int timeWidth = font.width(time);
		g.pose().pushMatrix();
		g.pose().translate(right - 6 - timeWidth * 1.5f, top + 4f);
		g.pose().scale(1.5f, 1.5f);
		g.text(font, time, 0, 0, argb(1f, timeColor), true);
		g.pose().popMatrix();

		// 진행 바 (남은 시간)
		int barLeft = left + 6;
		int barRight = right - 6;
		int barTop = top + 21;
		float remain = ended || lastSpin ? 0f : Math.max(0f, Math.min(1f, s.ticksLeft() / (float) TabooManager.INTERVAL_TICKS));
		int barColor = remain > 0.5f ? GREEN : remain > 0.2f ? GOLD : RED;

		g.fill(barLeft, barTop, barRight, barTop + 3, argb(1f, 0x2A2A2A));
		int filled = Math.round((barRight - barLeft) * remain);
		if (filled > 0) g.fill(barLeft, barTop, barLeft + filled, barTop + 3, argb(1f, barColor));

		// 금기 개수 + 목록
		g.text(font, "금기 " + count + " / " + Taboo.COUNT, left + 6, top + 28, argb(1f, WHITE), true);

		for (int i = 0; i < count; i++) {
			g.text(font, "× " + locked.get(i).title, left + 8, listTop + i * 11, argb(1f, RED), true);
		}
	}

	// ================================================================ 룰렛

	private static void drawRoulette(GuiGraphicsExtractor g, Font font, TabooState.Snapshot s) {
		TabooState.Roulette roulette = s.roulette();

		// 서버 틱 사이를 부드럽게 보간 (게임이 멈추면 보간도 멈춘다)
		float partial = s.paused() ? 0f : Math.min(1f, (System.nanoTime() - s.publishedAtNanos()) / 50_000_000f);
		float t = roulette.elapsedTicks() + Math.max(0f, partial);

		if (t < RouletteAnim.SPIN_TICKS) {
			drawReel(g, font, s, roulette, t);
		} else {
			drawResult(g, font, s, roulette, t - RouletteAnim.SPIN_TICKS);
		}
	}

	private static void drawReel(GuiGraphicsExtractor g, Font font, TabooState.Snapshot s, TabooState.Roulette roulette, float t) {
		int centerX = g.guiWidth() / 2;
		int titleY = Math.max(24, g.guiHeight() / 6);
		int boxWidth = 210;
		int boxLeft = centerX - boxWidth / 2;
		int boxRight = centerX + boxWidth / 2;
		int boxTop = titleY + 18;
		int boxBottom = boxTop + ROW_HEIGHT * VISIBLE_ROWS;
		int centerY = boxTop + ROW_HEIGHT * VISIBLE_ROWS / 2;

		drawCentered(g, font, "금기 룰렛", centerX, titleY, 1.6f, argb(1f, GOLD));

		g.fill(boxLeft, boxTop, boxRight, boxBottom, argb(0.88f, DARK));
		g.fill(boxLeft, centerY - ROW_HEIGHT / 2, boxRight, centerY + ROW_HEIGHT / 2, argb(0.22f, GOLD));
		g.outline(boxLeft - 1, boxTop - 1, boxWidth + 2, boxBottom - boxTop + 2, argb(1f, GOLD));

		float position = RouletteAnim.position(t);
		int base = (int) Math.floor(position);
		float fraction = position - base;

		g.enableScissor(boxLeft, boxTop, boxRight, boxBottom);

		for (int k = -3; k <= 3; k++) {
			Taboo entry = RouletteAnim.entryAt(roulette.reel(), base + k);
			if (entry == null) continue;

			float offset = k - fraction; // 가운데 칸에서 몇 칸 떨어져 있는지
			int y = Math.round(centerY + offset * ROW_HEIGHT) - 4;
			float distance = Math.abs(offset);
			int color = distance < 0.5f
					? argb(1f, WHITE)
					: argb(Math.max(0.2f, 0.85f - distance * 0.25f), GRAY);

			g.centeredText(font, entry.title, centerX, y, color);
		}

		g.disableScissor();

		g.text(font, "▶", boxLeft - 11, centerY - 4, argb(1f, GOLD), true);
		g.text(font, "◀", boxRight + 4, centerY - 4, argb(1f, GOLD), true);

		int nth = Math.min(Taboo.COUNT, s.locked().size() + 1);
		g.centeredText(font, "금기 " + nth + " / " + Taboo.COUNT + " 추첨 중...", centerX, boxBottom + 6, argb(1f, GRAY));
	}

	private static void drawResult(GuiGraphicsExtractor g, Font font, TabooState.Snapshot s, TabooState.Roulette roulette, float hold) {
		float fade = Math.min(1f, (RouletteAnim.HOLD_TICKS - hold) / 12f);
		if (fade <= 0.02f) return;

		int screenWidth = g.guiWidth();
		int screenHeight = g.guiHeight();
		int centerX = screenWidth / 2;

		// 확정 순간 화면 번쩍임
		if (hold < 8f) {
			g.fill(0, 0, screenWidth, screenHeight, argb((1f - hold / 8f) * 0.35f, 0xFFF2D0));
		}

		int titleY = Math.max(24, screenHeight / 6);
		int boxWidth = 240;
		int boxLeft = centerX - boxWidth / 2;
		int boxRight = centerX + boxWidth / 2;
		int boxTop = titleY + 18;
		int boxBottom = boxTop + 88;

		drawCentered(g, font, "새로운 금기", centerX, titleY, 1.6f, argb(fade, GOLD));

		g.fill(boxLeft, boxTop, boxRight, boxBottom, argb(0.9f * fade, 0x170606));
		g.outline(boxLeft - 1, boxTop - 1, boxWidth + 2, boxBottom - boxTop + 2, argb(fade, RED));

		Taboo result = roulette.result();
		float pop = hold < 5f ? 1f + (5f - hold) * 0.1f : 1f;
		float fit = (boxWidth - 20f) / Math.max(1, font.width(result.title));
		float scale = Math.min(2.4f, fit) * pop;
		drawCentered(g, font, result.title, centerX, boxTop + 18, scale, argb(fade, RED));

		g.centeredText(font, result.description, centerX, boxTop + 52, argb(fade, WHITE));

		int index = s.locked().indexOf(result) + 1;
		if (index <= 0) index = Math.min(Taboo.COUNT, s.locked().size() + 1);
		g.centeredText(font, "금기 " + index + " / " + Taboo.COUNT, centerX, boxTop + 70, argb(fade, GRAY));
	}

	// ================================================================ 유틸

	private static void drawCentered(GuiGraphicsExtractor g, Font font, String text, int centerX, int y, float scale, int color) {
		g.pose().pushMatrix();
		g.pose().translate(centerX, y);
		g.pose().scale(scale, scale);
		g.centeredText(font, text, 0, 0, color);
		g.pose().popMatrix();
	}

	private static String formatTime(int ticks) {
		int seconds = Math.max(0, (ticks + 19) / 20);
		return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}

	private static int argb(float alpha, int rgb) {
		int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}
}
