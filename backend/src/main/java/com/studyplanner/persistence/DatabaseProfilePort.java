package com.studyplanner.persistence;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.common.JsonCodec;
import com.studyplanner.contract.ProfileFact;
import com.studyplanner.profile.ProfileFactPort;
@Component
public class DatabaseProfilePort implements ProfileFactPort {
    private final FactRepository repository;
    private final JsonCodec json;
    public DatabaseProfilePort(FactRepository repository, JsonCodec json) { this.repository=repository; this.json=json; }
    @Override @Transactional(readOnly=true)
    public List<ProfileFact> findAll() {
        return repository.findByUserIdOrderById("demo").stream().map(e -> json.read(e.factJson, ProfileFact.class))
                .sorted(Comparator.comparingLong(ProfileFact::version)).toList();
    }
    @Override @Transactional
    public ProfileFact save(ProfileFact fact) {
        FactEntity e = new FactEntity(); e.id=UUID.randomUUID().toString(); e.userId="demo";
        e.factJson=json.write(fact); e.factVersion=fact.version(); repository.save(e); return fact;
    }
}
