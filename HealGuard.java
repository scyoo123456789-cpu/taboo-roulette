package com.taboochallenge;

/**
 * 자연회복을 막으면 바닐라가 회복 비용(허기)만 깎아가는 문제가 있어서,
 * 회복이 막힌 직후의 허기 소모 1회를 함께 취소하기 위한 표시.
 */
public final class HealGuard {
	private static final ThreadLocal<Boolean> BLOCKED = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private HealGuard() {
	}

	public static void markBlocked() {
		BLOCKED.set(Boolean.TRUE);
	}

	public static boolean consume() {
		if (BLOCKED.get()) {
			BLOCKED.set(Boolean.FALSE);
			return true;
		}

		return false;
	}

	static void reset() {
		BLOCKED.set(Boolean.FALSE);
	}
}
