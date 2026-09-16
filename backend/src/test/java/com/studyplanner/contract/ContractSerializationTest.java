package com.studyplanner.contract;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 契约序列化往返测试。确认 samples/contracts 中的样例 JSON 不丢字段，
 * 且缺少必填字段、未知枚举、过期版本三类异常样例均被拒绝。
 *
 * 验收命令：在 backend 目录执行 mvnw.cmd test -Dtest=ContractSerializationTest
 */
class ContractSerializationTest {

    private static ObjectMapper mapper;
    private static Path samplesDir;

    @BeforeAll
    static void setUp() throws Exception {
        mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        samplesDir = resolveSamplesDir();
        assertTrue(Files.isDirectory(samplesDir), "未找到 samples/contracts 目录: " + samplesDir);
    }

    /** 从 backend 模块目录解析到项目根下的 samples/contracts。 */
    private static Path resolveSamplesDir() {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path candidate = base.resolve("..").resolve("samples").resolve("contracts");
        if (Files.isDirectory(candidate)) {
            return candidate;
        }
        return base.resolve("samples").resolve("contracts");
    }

    private String read(String file) throws IOException {
        return Files.readString(samplesDir.resolve(file));
    }

    private <T> T deserialize(String json, Class<T> type) throws IOException {
        return mapper.readValue(json, type);
    }

    private <T> T roundTrip(String file, Class<T> type) throws IOException {
        T original = deserialize(read(file), type);
        String serialized = mapper.writeValueAsString(original);
        return deserialize(serialized, type);
    }

    // ---- 正常样例：序列化往返后对象等价，字段不丢 ----

    @Test
    void importProposalRoundTrip() throws IOException {
        ImportProposal original = deserialize(read("import-proposal-valid.json"), ImportProposal.class);
        assertEquals(original, roundTrip("import-proposal-valid.json", ImportProposal.class));
        assertTrue(original.title().length() > 0);
    }

    @Test
    void scheduleSnapshotRoundTrip() throws IOException {
        ScheduleSnapshot original = deserialize(read("schedule-snapshot-valid.json"), ScheduleSnapshot.class);
        assertEquals(original, roundTrip("schedule-snapshot-valid.json", ScheduleSnapshot.class));
        assertEquals(1, original.courses().size());
        assertEquals(1, original.tasks().size());
    }

    @Test
    void actionSpecRoundTrip() throws IOException {
        ActionSpec original = deserialize(read("action-spec-valid.json"), ActionSpec.class);
        assertEquals(original, roundTrip("action-spec-valid.json", ActionSpec.class));
        assertEquals(ActionType.CREATE_TASK, original.type());
    }

    @Test
    void profileFactRoundTrip() throws IOException {
        ProfileFact original = deserialize(read("profile-fact-valid.json"), ProfileFact.class);
        assertEquals(original, roundTrip("profile-fact-valid.json", ProfileFact.class));
        assertEquals(3, original.evidenceIds().size());
    }

    // ---- 异常样例：必须被拒绝 ----

    @Test
    void missingRequiredFieldRejected() {
        // import-proposal-missing-field.json 缺少必填字段 title
        assertThrows(Exception.class, () -> deserialize(read("import-proposal-missing-field.json"), ImportProposal.class));
    }

    @Test
    void unknownEnumRejected() {
        // unknown-enum.json 的 ActionSpec.type 为未知枚举值
        assertThrows(Exception.class, () -> deserialize(read("unknown-enum.json"), ActionSpec.class));
    }

    @Test
    void expiredVersionRejected() {
        // expired-version.json 的 schemaVersion = 0，低于最小版本 1
        assertThrows(Exception.class, () -> deserialize(read("expired-version.json"), ImportProposal.class));
    }
}
