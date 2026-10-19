package com.studyplanner.persistence;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.common.JsonCodec;
import com.studyplanner.importing.*;
@Component
public class DatabaseImportRepository implements ImportJobRepository {
    private final ImportRepository repository;
    private final JsonCodec json;
    public DatabaseImportRepository(ImportRepository repository,JsonCodec json) { this.repository=repository;this.json=json; }
    @Override @Transactional(readOnly=true)
    public Optional<ImportJob> findByRequestKey(String key) {
        return repository.findById(key).filter(e -> e.userId.equals("demo")).map(e -> json.read(e.jobJson,ImportJob.class));
    }
    @Override @Transactional
    public ImportJob save(ImportJob job) {
        if (job.requestKey().length()>160) throw new IllegalArgumentException("requestKey 不能超过 160 字符");
        ImportEntity e=new ImportEntity();e.id=job.requestKey();e.userId="demo";e.jobJson=json.write(job);repository.save(e);return job;
    }
}
