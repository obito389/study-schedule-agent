package com.studyplanner.agent;
import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.common.ApiException;
import com.studyplanner.contract.*;
import org.junit.jupiter.api.*;
class DeepSeekModelAdapterTest {
    private HttpServer server;
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final AtomicReference<String> requestBody=new AtomicReference<>();
    private final AtomicReference<String> authorization=new AtomicReference<>();
    private final AtomicReference<String> response=new AtomicReference<>();
    private int status=200;
    @BeforeEach void start() throws Exception {
        response.set("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"type\\\":\\\"QUERY_SCHEDULE\\\",\\\"targets\\\":[],\\\"changes\\\":{},\\\"reasonFacts\\\":[\\\"当前没有任务\\\"],\\\"ambiguities\\\":[]}\"}}]}");
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat/completions",exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes=response.get().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
    }
    @AfterEach void stop(){server.stop(0);}
    DeepSeekModelAdapter adapter(){return new DeepSeekModelAdapter(mapper,"test-key","http://127.0.0.1:"+server.getAddress().getPort(),"test-model",Duration.ofSeconds(2));}
    ScheduleSnapshot snapshot(){return new ScheduleSnapshot(1,Instant.parse("2026-10-19T00:00:00Z"),"Asia/Shanghai",List.of(),List.of(),List.of(),List.of(),0);}
    @Test void sendsStructuredJsonContextAndMapsResponse() throws Exception {
        assertThat(adapter().generate("查看日程",snapshot()).type()).isEqualTo(ActionType.QUERY_SCHEDULE);
        var body=mapper.readTree(requestBody.get());assertThat(body.path("model").asText()).isEqualTo("test-model");
        assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(body.path("messages").path(1).path("content").asText()).contains("Asia/Shanghai","查看日程");
        assertThat(authorization.get()).isEqualTo("Bearer test-key");
    }
    @Test void upstreamFailureDoesNotExposeResponseOrKey(){
        status=401;response.set("upstream-private-detail");
        assertThatThrownBy(()->adapter().generate("查询",snapshot())).isInstanceOfSatisfying(ApiException.class,e->{
            assertThat(e.code).isEqualTo("AI_AUTH_FAILED");assertThat(e.getMessage()).doesNotContain("test-key","upstream-private-detail");});
    }
    @Test void invalidJsonIsRejected(){
        response.set("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"not json\"}}]}");
        assertThatThrownBy(()->adapter().generate("查询",snapshot())).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("AI_INVALID_RESPONSE"));
    }
    @Test void missingCredentialDoesNotSendARequest(){
        var adapter=new DeepSeekModelAdapter(mapper,"","http://127.0.0.1:"+server.getAddress().getPort(),"test-model",Duration.ofSeconds(2));
        assertThatThrownBy(()->adapter.generate("查询",snapshot())).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("AI_NOT_CONFIGURED"));
        assertThat(requestBody.get()).isNull();
    }
    @Test void incompleteResponseIsRejected(){
        response.set("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}");
        assertThatThrownBy(()->adapter().generate("查询",snapshot())).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("AI_INVALID_RESPONSE"));
    }
}
