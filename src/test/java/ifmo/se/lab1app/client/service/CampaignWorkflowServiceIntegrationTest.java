package ifmo.se.lab1app.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.client.domain.creative.CreativeType;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.infra.CreativeUploadTaskRepository;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.shared.infra.KafkaOutboxEventRepository;
import ifmo.se.lab1app.system.kafka.CreativeUploadSagaHandler;
import ifmo.se.lab1app.system.kafka.dto.CreativeUploadRequestEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CampaignWorkflowServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private CreativeUploadTaskRepository creativeUploadTaskRepository;

    @Autowired
    private KafkaOutboxEventRepository outboxEventRepository;

    @Autowired
    private CreativeUploadSagaHandler creativeUploadSagaHandler;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        outboxEventRepository.deleteAll();
        creativeUploadTaskRepository.deleteAll();
        creativeRepository.deleteAll();
        campaignRepository.deleteAll();
    }

    @Test
    void shouldAddAndDeleteCreativeAcrossSeparateXaDatabases() throws Exception {
        String token = registerAndGetToken();
        Long campaignId = createConfiguredCampaign(token);

        MvcResult addResult = mockMvc.perform(post("/advertisement/client/{campaignId}/creatives", campaignId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "url", "https://cdn.example.com/banner.png",
                                "type", "IMAGE"
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.campaignStatus").value("CREATIVES_LOADING"))
                .andExpect(jsonPath("$.url").value("https://cdn.example.com/banner.png"))
                .andExpect(jsonPath("$.type").value("IMAGE"))
                .andReturn();

        JsonNode addBody = objectMapper.readTree(addResult.getResponse().getContentAsString());
        String taskId = addBody.path("taskId").asText();
        assertThat(taskId).isNotBlank();
        assertThat(creativeRepository.countByCampaignId(campaignId)).isZero();

        mockMvc.perform(get("/advertisement/client/{campaignId}/creative-loads/{taskId}", campaignId, taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.campaignStatus").value("CREATIVES_LOADING"));

        creativeUploadSagaHandler.processRequest(new CreativeUploadRequestEvent(
                taskId,
                campaignId,
                "https://cdn.example.com/banner.png",
                CreativeType.IMAGE
        ));
        assertThat(creativeRepository.countByCampaignId(campaignId)).isEqualTo(1);

        mockMvc.perform(get("/advertisement/client/{campaignId}/creative-loads/{taskId}", campaignId, taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.campaignStatus").value("CREATIVES_UPLOADED"))
                .andExpect(jsonPath("$.creativeId").isNumber());

        MvcResult campaignResult = mockMvc.perform(get("/advertisement/{campaignId}", campaignId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATIVES_UPLOADED"))
                .andExpect(jsonPath("$.creatives[0].name").value("https://cdn.example.com/banner.png"))
                .andExpect(jsonPath("$.creatives[0].type").value("IMAGE"))
                .andReturn();

        Long creativeId = objectMapper.readTree(campaignResult.getResponse().getContentAsString())
                .path("creatives")
                .get(0)
                .path("id")
                .asLong();

        mockMvc.perform(delete("/advertisement/client/{campaignId}/creatives/{creativeId}", campaignId, creativeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIGURED"))
                .andExpect(jsonPath("$.creatives").isEmpty());

        assertThat(creativeRepository.countByCampaignId(campaignId)).isZero();
    }

    @Test
    void shouldKeepCreativeUploadIdempotentWhenSameTaskIsConsumedTwice() throws Exception {
        String token = registerAndGetToken();
        Long campaignId = createConfiguredCampaign(token);
        String url = "https://cdn.example.com/idempotent.png";

        MvcResult addResult = enqueueCreative(token, campaignId, url);
        String taskId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("taskId")
                .asText();

        CreativeUploadRequestEvent event = new CreativeUploadRequestEvent(taskId, campaignId, url, CreativeType.IMAGE);
        creativeUploadSagaHandler.processRequest(event);
        creativeUploadSagaHandler.processRequest(event);

        assertThat(creativeRepository.countByCampaignId(campaignId)).isEqualTo(1);
        assertThat(creativeUploadTaskRepository.findById(taskId).orElseThrow().getStatus())
                .isEqualTo(CreativeUploadStatus.COMPLETED);
    }

    @Test
    void shouldRejectCreativeUploadInInvalidCampaignState() throws Exception {
        String token = registerAndGetToken();

        MvcResult createResult = mockMvc.perform(post("/advertisement/client")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Draft campaign",
                                "objective", "TRAFFIC",
                                "campaignType", "DISPLAY",
                                "url", "https://example.com",
                                "startMode", "MANUAL_START"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        Long campaignId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        mockMvc.perform(post("/advertisement/client/{campaignId}/creatives", campaignId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "url", "https://cdn.example.com/draft.png",
                                "type", "IMAGE"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldEnforceMaxCreativeLimitIncludingActiveUploadTasks() throws Exception {
        String token = registerAndGetToken();
        Long campaignId = createConfiguredCampaign(token);

        for (int i = 0; i < 10; i++) {
            enqueueCreative(token, campaignId, "https://cdn.example.com/banner-" + i + ".png");
        }

        assertThat(creativeUploadTaskRepository.countByCampaignIdAndStatusIn(
                campaignId,
                java.util.Set.of(CreativeUploadStatus.PENDING, CreativeUploadStatus.PROCESSING)
        )).isEqualTo(10);
        assertThat(creativeRepository.countByCampaignId(campaignId)).isZero();

        mockMvc.perform(post("/advertisement/client/{campaignId}/creatives", campaignId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "url", "https://cdn.example.com/banner-10.png",
                                "type", "IMAGE"
                        ))))
                .andExpect(status().isConflict());
    }

    private String registerAndGetToken() throws Exception {
        String username = "client-" + UUID.randomUUID();
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "secret123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken")
                .asText();
    }

    private Long createConfiguredCampaign(String token) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/advertisement/client")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "XA campaign",
                                "objective", "TRAFFIC",
                                "campaignType", "DISPLAY",
                                "url", "https://example.com",
                                "startMode", "MANUAL_START"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        Long campaignId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        mockMvc.perform(post("/advertisement/client/{campaignId}/configure", campaignId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "budgetAmount", new BigDecimal("1000.00"),
                                "requestedStartAt", LocalDateTime.now().plusDays(1).withNano(0).toString(),
                                "durationDays", 7
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIGURED"));

        return campaignId;
    }

    private MvcResult enqueueCreative(String token, Long campaignId, String url) throws Exception {
        return mockMvc.perform(post("/advertisement/client/{campaignId}/creatives", campaignId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "url", url,
                                "type", "IMAGE"
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.campaignStatus").value("CREATIVES_LOADING"))
                .andReturn();
    }
}
