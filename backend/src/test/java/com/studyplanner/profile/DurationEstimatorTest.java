package com.studyplanner.profile;

import com.studyplanner.contract.ProfileFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 耗时估算测试：三样本前用原始估计、三样本后用平滑倍率、倍率钳制。
 */
class DurationEstimatorTest {

    private final DurationEstimator estimator = new DurationEstimator();

    private ProfileFact factWith(String value, int samples) {
        List<String> evidence = new java.util.ArrayList<>();
        for (int i = 0; i < samples; i++) {
            evidence.add("e" + i);
        }
        return new ProfileFact(1, "duration.factor.t1", value, "GLOBAL", 0.7, "OBSERVED", "ACTIVE", evidence, 1);
    }

    @Test
    void belowThreeSamplesUsesRawEstimate() {
        assertEquals(1.0, estimator.factorFor(factWith("1.5", 2)), 1e-9);
    }

    @Test
    void threeSamplesUsesSmoothedFactor() {
        assertEquals(1.5, estimator.factorFor(factWith("1.5", 3)), 1e-9);
    }

    @Test
    void factorClampedToRange() {
        assertEquals(DurationEstimator.MIN_FACTOR, estimator.factorFor(factWith("0.1", 3)), 1e-9);
        assertEquals(DurationEstimator.MAX_FACTOR, estimator.factorFor(factWith("5.0", 3)), 1e-9);
    }

    @Test
    void nullFactUsesRawEstimate() {
        assertEquals(1.0, estimator.factorFor(null), 1e-9);
    }

    @Test
    void adjustedMinutesRounds() {
        assertEquals(150, estimator.adjustedMinutes(100, factWith("1.5", 3)));
    }
}
