package com.studyplanner.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.studyplanner.common.BusinessException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Binds every operation key to the complete ordered batch, including authorization. */
public final class RequestFingerprint {
    private RequestFingerprint() {}

    public static String of(ActionBatchRequest request, ObjectMapper mapper) {
        try {
            ObjectMapper canonical = mapper.copy();
            canonical.setConfig(canonical.getSerializationConfig()
                    .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
            byte[] bytes = canonical.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new BusinessException(500, "无法生成请求指纹");
        }
    }
}
