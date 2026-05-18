package com.nobodiiiii.createbiotech.content.experience;

public interface ExperienceSource {
	int extractExperience(int maxAmount, boolean simulate);

	int getStoredExperience();
}
