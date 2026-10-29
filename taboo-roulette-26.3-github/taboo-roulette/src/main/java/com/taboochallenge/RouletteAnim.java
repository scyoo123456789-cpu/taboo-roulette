package com.taboochallenge;

import java.util.ArrayList;
import java.util.List;

/**
 * 룰렛 연출 타이밍. 서버(효과음)와 클라이언트(화면)가 같은 계산식을 써서 소리와 화면이 맞아떨어진다.
 */
public final class RouletteAnim {
	/** 릴이 회전하는 시간 (5초). 이 시점에 금기가 확정된다. */
	public static final int SPIN_TICKS = 100;
	/** 결과 카드를 보여주는 시간 (3.5초). */
	public static final int HOLD_TICKS = 70;
	public static final int TOTAL_TICKS = SPIN_TICKS + HOLD_TICKS;
	/** 회전하는 동안 지나가는 칸 수. */
	public static final int DISTANCE = 40;
	/** 릴 앞뒤로 화면 밖에 미리 깔아두는 칸 수. */
	public static final int PAD = 3;

	private RouletteAnim() {
	}

	/** 경과 틱 → 릴 위치(칸 단위). 처음엔 빠르고 끝에서 천천히 멈춘다(ease-out). */
	public static float position(float elapsedTicks) {
		if (elapsedTicks <= 0f) return 0f;
		if (elapsedTicks >= SPIN_TICKS) return DISTANCE;

		float inv = 1f - elapsedTicks / SPIN_TICKS;
		return DISTANCE * (1f - inv * inv * inv * inv);
	}

	/** 현재 가운데 칸에 걸려 있는 줄 번호. */
	public static int centeredRow(float elapsedTicks) {
		return Math.round(position(elapsedTicks));
	}

	/** 섞인 후보 목록으로 릴을 만든다. 마지막 칸(DISTANCE)에 반드시 result 가 오도록 시작점을 맞춘다. */
	public static List<Taboo> buildReel(List<Taboo> shuffled, Taboo result) {
		int size = shuffled.size();
		int start = Math.floorMod(shuffled.indexOf(result) - DISTANCE, size);
		List<Taboo> reel = new ArrayList<>(DISTANCE + 2 * PAD + 1);

		for (int row = -PAD; row <= DISTANCE + PAD; row++) {
			reel.add(shuffled.get(Math.floorMod(start + row, size)));
		}

		return List.copyOf(reel);
	}

	public static Taboo entryAt(List<Taboo> reel, int row) {
		int index = row + PAD;
		return index < 0 || index >= reel.size() ? null : reel.get(index);
	}
}
