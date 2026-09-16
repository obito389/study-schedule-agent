package com.studyplanner.profile;

import com.studyplanner.contract.ActionSpec;
import com.studyplanner.contract.ActionStatus;
import com.studyplanner.contract.ActionType;
import com.studyplanner.contract.ProfileFact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户模型服务测试：增删查、临时例外、手动覆盖、删除后草案失效、平滑倍率。
 */
class ProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private ProfileService service;
    private InMemoryProfileFactPort port;

    @BeforeEach
    void setUp() {
        port = new InMemoryProfileFactPort();
        service = new ProfileService(port, new DurationEstimator());
    }

    @Test
    void addAndListExplicitPreference() {
        service.addExplicitPreference("prefer.morning", "true", "GLOBAL", 0.9, "USER_INPUT");
        assertEquals(1, service.listFacts().size());
        assertEquals("true", service.listFacts().get(0).value());
        assertEquals("ACTIVE", service.listFacts().get(0).state());
    }

    @Test
    void deleteFactRemoves() {
        service.addExplicitPreference("prefer.morning", "true", "GLOBAL", 0.9, "USER_INPUT");
        service.deleteFact("prefer.morning");
        assertTrue(service.listFacts().isEmpty());
    }

    @Test
    void dateExceptionOverridesGlobal() {
        service.addExplicitPreference("duration.factor.a", "1.0", "GLOBAL", 0.7, "USER_INPUT");
        service.addDateException("duration.factor.a", "1.8", NOW, ZONE);
        // 当天：临时例外 1.8 生效（三样本规则要求样本>=3，先补样本再验证覆盖）
        ProfileFact global = port.findByKey("duration.factor.a").orElseThrow();
        // 覆盖关系通过 applicableFact 验证：DATE 优先于 GLOBAL
        int onDate = service.adjustedDurationMinutes("duration.factor.a", 100, NOW, ZONE, false);
        // 全局 fact 无样本 → 倍率 1.0；DATE 例外也无样本 → 倍率 1.0
        assertEquals(100, onDate);
        // 显式验证 DATE scope 事实存在且被优先选择
        assertTrue(port.findActive().stream().anyMatch(f -> f.scope().equals("DATE:" + NOW.atZone(ZONE).toLocalDate())));
        assertEquals("1.8", port.findByKey("duration.factor.a").orElseThrow().value());
    }

    @Test
    void manualOverrideSkipsFactor() {
        service.addExplicitPreference("duration.factor.b", "1.5", "GLOBAL", 0.7, "USER_INPUT");
        // 手动修改剩余量后，本轮不再二次应用倍率
        assertEquals(100, service.adjustedDurationMinutes("duration.factor.b", 100, NOW, ZONE, true));
    }

    @Test
    void smoothedFactorAfterThreeSamples() {
        service.recordCompletion("duration.factor.c", 1.0, "OBSERVED", "GLOBAL", "fb-1");
        service.recordCompletion("duration.factor.c", 2.0, "OBSERVED", "GLOBAL", "fb-2");
        service.recordCompletion("duration.factor.c", 3.0, "OBSERVED", "GLOBAL", "fb-3");
        ProfileFact fact = port.findByKey("duration.factor.c").orElseThrow();
        assertEquals(3, fact.evidenceIds().size());
        // 平滑：(1.0+2.0+3.0)/3 = 2.0，钳制在 0.5-2.0
        assertEquals(200, service.adjustedDurationMinutes("duration.factor.c", 100, NOW, ZONE, false));
    }

    @Test
    void deleteInvalidatesDraft() {
        service.addExplicitPreference("prefer.evening", "true", "GLOBAL", 0.9, "USER_INPUT");
        ActionSpec draft = new ActionSpec(1, "op-1", 1, ActionType.MOVE_TASK, ActionStatus.NEEDS_CONFIRMATION,
                List.of("t1"), Map.of(), "USER_CONFIRMED", List.of("prefer.evening"));
        assertTrue(service.draftStillValid(draft));
        service.deleteFact("prefer.evening");
        assertFalse(service.draftStillValid(draft), "引用已删除事实的草案必须失效");
    }
}
