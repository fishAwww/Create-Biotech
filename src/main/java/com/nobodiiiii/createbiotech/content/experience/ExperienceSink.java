package com.nobodiiiii.createbiotech.content.experience;

public interface ExperienceSink {
	int insertExperience(int amount, boolean simulate);

	int getExperienceSpace();

	default boolean isExperienceInputBlocked() {
		return getExperienceSpace() <= 0;
	}
}
