package com.studyplanner.profile;

import com.studyplanner.contract.ProfileFact;

import java.util.List;
import java.util.Optional;

/**
 * 用户事实存储端口。实现由 persistence 模块提供；初版使用
 * {@link InMemoryProfileFactPort} 保证无数据库时仍可演示。
 */
public interface ProfileFactPort {

    List<ProfileFact> findActive();

    Optional<ProfileFact> findByKey(String key);

    ProfileFact save(ProfileFact fact);

    void delete(String key);
}
