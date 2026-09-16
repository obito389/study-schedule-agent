package com.studyplanner.profile;

import com.studyplanner.contract.ProfileFact;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存用户事实存储，供单元测试与无数据库演示使用。
 */
public class InMemoryProfileFactPort implements ProfileFactPort {

    private final ConcurrentMap<String, ProfileFact> store = new ConcurrentHashMap<>();

    @Override
    public List<ProfileFact> findActive() {
        return store.values().stream()
                .filter(f -> "ACTIVE".equals(f.state()))
                .sorted(Comparator.comparing(ProfileFact::key))
                .toList();
    }

    @Override
    public Optional<ProfileFact> findByKey(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public ProfileFact save(ProfileFact fact) {
        store.put(fact.key(), fact);
        return fact;
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }
}
