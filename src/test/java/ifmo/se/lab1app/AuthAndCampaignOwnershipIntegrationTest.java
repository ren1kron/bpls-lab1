package ifmo.se.lab1app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.auth.application.BaseModeratorBootstrap;
import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.shared.domain.UserRole;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import java.util.Map;
import java.util.Set;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthAndCampaignOwnershipIntegrationTest {

    private static final Set<String> BOOTSTRAPPED_USERNAMES = Set.of("moderator");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private BaseModeratorBootstrap baseModeratorBootstrap;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        creativeRepository.deleteAll();
        campaignRepository.deleteAll();
        userAccountRepository.findAll().stream()
                .filter(user -> !BOOTSTRAPPED_USERNAMES.contains(user.getUsername()))
                .map(UserAccount::getId)
                .forEach(userAccountRepository::deleteById);
    }

    @Test
    void shouldLoginBootstrappedModeratorFromDatabase() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "moderator",
                                "password", "moderator"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("moderator"))
                .andExpect(jsonPath("$.user.role").value("COMPANY_MODERATOR"));
    }

    @Test
    void shouldNormalizeRawBaseModeratorPasswordOnBootstrap() throws Exception {
        UserAccount moderator = userAccountRepository.findByUsername("moderator").orElseThrow();
        moderator.setPassword("moderator");
        moderator.setRole(UserRole.COMPANY_MODERATOR);
        userAccountRepository.save(moderator);

        baseModeratorBootstrap.run(null);

        UserAccount normalizedModerator = userAccountRepository.findByUsername("moderator").orElseThrow();
        assertThat(normalizedModerator.getPassword()).startsWith("{bcrypt}");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "moderator",
                                "password", "moderator"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("COMPANY_MODERATOR"));
    }

    @Test
    void shouldRegisterClientAndReturnToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "password", "secret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.user.role").value("CLIENT"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("tokenType").asText()).isEqualTo("Bearer");

        UserAccount savedUser = userAccountRepository.findByUsername("alice").orElseThrow();
        assertThat(savedUser.getRole().name()).isEqualTo("CLIENT");
        assertThat(savedUser.getPassword()).startsWith("{bcrypt}");
        assertThat(savedUser.getPassword()).doesNotContain("secret123");
    }

    @Test
    void shouldForbidClientToModifyAnotherUsersCampaign() throws Exception {
        String aliceToken = registerAndGetToken("alice", "secret123");
        String bobToken = registerAndGetToken("bob", "secret123");

        MvcResult createResult = mockMvc.perform(post("/advertisement/client")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alice campaign",
                                "objective", "TRAFFIC",
                                "campaignType", "DISPLAY",
                                "url", "https://example.com",
                                "startMode", "MANUAL_START"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        Long campaignId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        mockMvc.perform(patch("/advertisement/client/{campaignId}", campaignId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Bob hijack"
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/advertisement/{campaignId}", campaignId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/advertisement/client/{campaignId}", campaignId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alice updated"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice updated"));
    }

    private String registerAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken")
                .asText();
    }
}
