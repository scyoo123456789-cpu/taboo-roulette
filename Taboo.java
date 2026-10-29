package com.taboochallenge;

/**
 * 룰렛에서 뽑히는 12가지 금기. 한 번 뽑히면 영구적으로 잠긴다.
 */
public enum Taboo {
	MINING("채굴 금지", "블록을 캘 수 없습니다"),
	ATTACK("공격 금지", "무엇도 때릴 수 없습니다"),
	MOVE("거동 금지", "스스로 움직일 수 없습니다"),
	JUMP("점프 금지", "점프할 수 없습니다"),
	SHIELD("방패 금지", "방패를 들 수 없습니다"),
	SNEAK("웅크리기 금지", "웅크릴 수 없습니다"),
	FOOD("음식 금지", "아무것도 먹을 수 없습니다"),
	BLOCK_INTERACT("블록간 상호작용 금지", "상자·문·작업대 등을 쓸 수 없습니다"),
	RESPAWN("부활 금지", "죽으면 리스폰 없이 끝납니다"),
	HEAL("피 회복 금지", "체력이 절대 회복되지 않습니다"),
	PLACE("설치 금지", "아무것도 설치할 수 없습니다"),
	PROJECTILE("투사체 금지", "아무것도 쏘거나 던질 수 없습니다");

	public static final int COUNT = values().length;

	public final String title;
	public final String description;

	Taboo(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public long bit() {
		return 1L << ordinal();
	}

	public static Taboo byName(String name) {
		try {
			return valueOf(name);
		} catch (IllegalArgumentException | NullPointerException e) {
			return null;
		}
	}
}
