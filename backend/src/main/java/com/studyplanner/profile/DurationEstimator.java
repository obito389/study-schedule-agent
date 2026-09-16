package com.studyplanner.profile;

import com.studyplanner.contract.ProfileFact;

/**
 * 耗时估算器。三个有效样本之前使用原始估计（倍率 1.0）；
 * 达到三个样本后使用平滑倍率，并限制在 0.5 至 2.0 之间。
 * 倍率来源为用户事实 value，样本数为 evidenceIds 数量。
 */
public class DurationEstimator {

    public static final double MIN_FACTOR = 0.5;
    public static final double MAX_FACTOR = 2.0;
    public static final int SAMPLE_THRESHOLD = 3;

    public double factorFor(ProfileFact fact) {
        if (fact == null) {
            return 1.0;
        }
        int samples = fact.evidenceIds() == null ? 0 : fact.evidenceIds().size();
        if (samples < SAMPLE_THRESHOLD) {
            return 1.0;
        }
        double factor;
        try {
            factor = Double.parseDouble(fact.value().trim());
        } catch (NumberFormatException e) {
            return 1.0;
        }
        return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, factor));
    }

    public int adjustedMinutes(int rawMinutes, ProfileFact fact) {
        return (int) Math.round(rawMinutes * factorFor(fact));
    }
}
