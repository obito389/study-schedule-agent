package com.studyplanner.profile;

import com.studyplanner.contract.ActionSpec;
import com.studyplanner.contract.ProfileFact;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 证据化用户模型服务。支持明确偏好、指定日期临时例外和事实删除；
 * 耗时倍率平滑、范围限制（0.5-2.0）与样本统计；删除事实后重算，
 * 引用已删除事实 key 的动作草案失效。
 */
public class ProfileService {

    public static final String STATE_ACTIVE = "ACTIVE";
    public static final String SCOPE_GLOBAL = "GLOBAL";

    private final ProfileFactPort port;
    private final DurationEstimator estimator;

    public ProfileService(ProfileFactPort port, DurationEstimator estimator) {
        this.port = port;
        this.estimator = estimator;
    }

    public List<ProfileFact> listFacts() {
        return port.findActive();
    }

    /** 记录一次任务完成反馈：写入实测/估计倍率，达到三个样本后参与平滑。 */
    public ProfileFact recordCompletion(String key, double observedFactor, String source, String scope, String evidenceId) {
        Optional<ProfileFact> existing = port.findByKey(key);
        long version = existing.map(f -> f.version() + 1).orElse(1L);
        List<String> evidence = new ArrayList<>(existing.map(f -> f.evidenceIds()).orElse(List.of()));
        evidence.add(evidenceId);

        double factor = observedFactor;
        if (existing.isPresent()) {
            double previous = parseFactor(existing.get().value(), 1.0);
            factor = (previous * (evidence.size() - 1) + observedFactor) / evidence.size();
        }
        return port.save(new ProfileFact(1, key, String.valueOf(factor), scope, 0.7,
                source, STATE_ACTIVE, evidence, version));
    }

    /** 添加明确偏好（用户主动声明），scope 如 GLOBAL / TASK:xxx / COURSE:xxx。 */
    public ProfileFact addExplicitPreference(String key, String value, String scope, double strength, String source) {
        long version = port.findByKey(key).map(f -> f.version() + 1).orElse(1L);
        return port.save(new ProfileFact(1, key, value, scope, strength, source, STATE_ACTIVE, List.of(), version));
    }

    /** 指定日期临时例外，scope 为 DATE:yyyy-MM-dd，优先于全局偏好。 */
    public ProfileFact addDateException(String key, String value, Instant date, ZoneId zone) {
        String scope = "DATE:" + LocalDate.ofInstant(date, zone);
        return addExplicitPreference(key, value, scope, 1.0, "USER_INPUT");
    }

    public void deleteFact(String key) {
        port.delete(key);
    }

    /**
     * 计算任务实际应采用的学习时长。manualOverride 为 true 表示用户手动修改了
     * 剩余量，本轮不再二次应用倍率。
     */
    public int adjustedDurationMinutes(String key, int rawMinutes, Instant date, ZoneId zone, boolean manualOverride) {
        if (manualOverride) {
            return rawMinutes;
        }
        ProfileFact fact = applicableFact(key, date, zone);
        return estimator.adjustedMinutes(rawMinutes, fact);
    }

    /** 动作草案引用的用户事实 key 全部仍然存在时有效；引用已删除事实则失效。 */
    public boolean draftStillValid(ActionSpec spec) {
        for (String reasonKey : spec.reasonFacts()) {
            if (port.findByKey(reasonKey).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ProfileFact applicableFact(String key, Instant date, ZoneId zone) {
        String dateScope = "DATE:" + LocalDate.ofInstant(date, zone);
        for (ProfileFact fact : port.findActive()) {
            if (fact.key().equals(key) && dateScope.equals(fact.scope())) {
                return fact;
            }
        }
        for (ProfileFact fact : port.findActive()) {
            if (fact.key().equals(key) && SCOPE_GLOBAL.equals(fact.scope())) {
                return fact;
            }
        }
        return null;
    }

    private double parseFactor(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
