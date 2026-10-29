package com.taboochallenge;

import java.util.List;

/**
 * 서버(내장 서버) 스레드가 쓰고, 클라이언트 렌더/입력 스레드가 읽는 공유 상태.
 * 싱글플레이 전용이라 두 쪽이 같은 JVM 안에 있으므로 네트워크 동기화 없이 이 스냅샷 하나로 공유한다.
 */
public final class TabooState {
	/** 룰렛이 돌아가는 중일 때의 정보. reel 은 {@link RouletteAnim#buildReel} 이 만든 릴. */
	public record Roulette(List<Taboo> reel, Taboo result, int elapsedTicks) {
	}

	public record Snapshot(
			boolean active,
			boolean running,
			boolean paused,
			boolean failed,
			List<Taboo> locked,
			int ticksLeft,
			Roulette roulette,
			long publishedAtNanos
	) {
		public static final Snapshot INACTIVE = new Snapshot(false, false, false, false, List.of(), 0, null, 0L);
	}

	private static volatile Snapshot current = Snapshot.INACTIVE;
	private static volatile long lockedBits = 0L;

	private TabooState() {
	}

	public static Snapshot get() {
		return current;
	}

	public static boolean isLocked(Taboo taboo) {
		return (lockedBits & taboo.bit()) != 0L;
	}

	public static boolean anyLocked() {
		return lockedBits != 0L;
	}

	static void publish(Snapshot snapshot) {
		long bits = 0L;

		for (Taboo taboo : snapshot.locked()) {
			bits |= taboo.bit();
		}

		lockedBits = bits;
		current = snapshot;
	}

	static void clear() {
		lockedBits = 0L;
		current = Snapshot.INACTIVE;
	}
}
