package com.assetmind.application;

import com.assetmind.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for all REST controllers.
 * Boots the full Spring context with H2 in-memory DB.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateAccessToken("test-user-id", "testuser", "USER");
    }

    // =====================================================================
    // Asset CRUD
    // =====================================================================
    @Nested
    class AssetEndpoints {

        @Test
        void createAndRetrieveAsset() throws Exception {
            String body = """
                    {
                      "id": "ASSET-IT-001",
                      "description": "Dell XPS 15 Laptop",
                      "assetClass": "COMPUTER_EQUIPMENT",
                      "costBasis": 1500.00,
                      "inServiceDate": "2026-01-15",
                      "usefulLifeYears": 5
                    }
                    """;

            mockMvc.perform(post("/api/v1/assets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("ASSET-IT-001"))
                    .andExpect(jsonPath("$.assetClass").value("COMPUTER_EQUIPMENT"))
                    .andExpect(jsonPath("$.costBasis").value(1500.00));

            mockMvc.perform(get("/api/v1/assets/ASSET-IT-001")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Dell XPS 15 Laptop"));
        }

        @Test
        void listAssets() throws Exception {
            mockMvc.perform(get("/api/v1/assets")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void paginatedAssets() throws Exception {
            mockMvc.perform(get("/api/v1/assets/page?page=0&size=10")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        void updateAsset() throws Exception {
            // Create
            String createBody = """
                    {
                      "id": "ASSET-UPD-001",
                      "description": "Old description",
                      "assetClass": "FURNITURE",
                      "costBasis": 500.00,
                      "inServiceDate": "2026-02-01",
                      "usefulLifeYears": 7
                    }
                    """;
            mockMvc.perform(post("/api/v1/assets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody))
                    .andExpect(status().isCreated());

            // Update
            String updateBody = """
                    {
                      "id": "ASSET-UPD-001",
                      "description": "Updated standing desk",
                      "assetClass": "FURNITURE",
                      "costBasis": 700.00,
                      "inServiceDate": "2026-02-01",
                      "usefulLifeYears": 7
                    }
                    """;
            mockMvc.perform(put("/api/v1/assets/ASSET-UPD-001")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Updated standing desk"))
                    .andExpect(jsonPath("$.costBasis").value(700.00));
        }

        @Test
        void deleteAsset() throws Exception {
            String body = """
                    {
                      "id": "ASSET-DEL-001",
                      "description": "To be deleted",
                      "assetClass": "OTHER",
                      "costBasis": 100.00,
                      "inServiceDate": "2026-03-01",
                      "usefulLifeYears": 5
                    }
                    """;
            mockMvc.perform(post("/api/v1/assets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete("/api/v1/assets/ASSET-DEL-001")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        void createAsset_validationFails() throws Exception {
            String body = """
                    {
                      "id": "",
                      "description": "",
                      "assetClass": "COMPUTER_EQUIPMENT",
                      "costBasis": 100.00,
                      "inServiceDate": "2026-01-01",
                      "usefulLifeYears": 5
                    }
                    """;
            mockMvc.perform(post("/api/v1/assets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unauthenticatedRequestIsRejected() throws Exception {
            mockMvc.perform(get("/api/v1/assets"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =====================================================================
    // Depreciation
    // =====================================================================
    @Nested
    class DepreciationEndpoints {

        @Test
        void runDepreciation_returnSchedule() throws Exception {
            String body = """
                    {
                      "assetId": "DEP-001",
                      "bookType": "TAX",
                      "method": "STRAIGHT_LINE",
                      "assetClass": "COMPUTER_EQUIPMENT",
                      "inServiceDate": "2026-01-01",
                      "costBasis": 10000.00,
                      "salvageValue": 0,
                      "usefulLifeYears": 5,
                      "section179Enabled": false,
                      "section179Amount": 0,
                      "bonusDepreciationRate": 0
                    }
                    """;
            mockMvc.perform(post("/api/v1/depreciation/run")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].yearNumber").value(1))
                    .andExpect(jsonPath("$[0].depreciationExpense").isNumber());
        }

        @Test
        void recommend_returnsRecommendation() throws Exception {
            String body = """
                    {
                      "stateCode": "CA",
                      "equipmentType": "Dell laptop",
                      "assetClass": "COMPUTER_EQUIPMENT",
                      "immediateDeductionPreferred": true,
                      "longHorizonAsset": false
                    }
                    """;
            mockMvc.perform(post("/api/v1/depreciation/recommend")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recommendedMethod").isString())
                    .andExpect(jsonPath("$.confidence").isNumber());
        }
    }

    // =====================================================================
    // Classification
    // =====================================================================
    @Nested
    class ClassificationEndpoints {

        @Test
        void suggest_returnsClassification() throws Exception {
            String body = """
                    { "documentText": "Invoice for Dell XPS 15 laptop for engineering team" }
                    """;
            mockMvc.perform(post("/api/v1/classification/suggest")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assetClass").value("COMPUTER_EQUIPMENT"))
                    .andExpect(jsonPath("$.glCode").isString())
                    .andExpect(jsonPath("$.confidence").isNumber());
        }

        @Test
        void suggest_validationFailsOnBlank() throws Exception {
            String body = """
                    { "documentText": "" }
                    """;
            mockMvc.perform(post("/api/v1/classification/suggest")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // =====================================================================
    // Tax Strategy
    // =====================================================================
    @Nested
    class TaxStrategyEndpoints {

        @Test
        void recommend_returnsTaxStrategy() throws Exception {
            String body = """
                    {
                      "stateCode": "TX",
                      "equipmentType": "Industrial press",
                      "immediateDeductionPreferred": false,
                      "longHorizonAsset": true
                    }
                    """;
            mockMvc.perform(post("/api/v1/tax-strategy/recommend")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recommendedMethod").isString())
                    .andExpect(jsonPath("$.confidence").isNumber());
        }
    }

    // =====================================================================
    // Breakout
    // =====================================================================
    @Nested
    class BreakoutEndpoints {

        @Test
        void suggest_returnsBreakoutComponents() throws Exception {
            String body = """
                    { "documentText": "Commercial office building at 500 Main Street with parking lot" }
                    """;
            mockMvc.perform(post("/api/v1/breakout/suggest")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.components").isArray())
                    .andExpect(jsonPath("$.components", hasSize(greaterThanOrEqualTo(3))))
                    .andExpect(jsonPath("$.confidence").isNumber())
                    .andExpect(jsonPath("$.source").isString());
        }

        @Test
        void suggest_validationFailsOnBlank() throws Exception {
            String body = """
                    { "documentText": "" }
                    """;
            mockMvc.perform(post("/api/v1/breakout/suggest")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // =====================================================================
    // Auth
    // =====================================================================
    @Nested
    class AuthEndpoints {

        @Test
        void registerAndLogin() throws Exception {
            String registerBody = """
                    {
                      "username": "integrationuser",
                      "password": "SecureP@ss123",
                      "email": "integration@test.com"
                    }
                    """;
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody))
                    .andExpect(status().isCreated());

            String loginBody = """
                    {
                      "username": "integrationuser",
                      "password": "SecureP@ss123"
                    }
                    """;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        void login_invalidCredentials() throws Exception {
            String body = """
                    {
                      "username": "nonexistent",
                      "password": "wrong"
                    }
                    """;
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =====================================================================
    // Swagger / OpenAPI
    // =====================================================================
    @Nested
    class SwaggerEndpoints {

        @Test
        void swaggerUiIsAccessible() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }

        @Test
        void apiDocsAreAccessible() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openapi").isString());
        }
    }

    // =====================================================================
    // Correlation ID
    // =====================================================================
    @Nested
    class CorrelationIdTests {

        @Test
        void responseIncludesCorrelationIdHeader() throws Exception {
            mockMvc.perform(get("/api/v1/assets")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Correlation-ID"));
        }

        @Test
        void echoesProvidedCorrelationId() throws Exception {
            mockMvc.perform(get("/api/v1/assets")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Correlation-ID", "test-cid-12345"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Correlation-ID", "test-cid-12345"));
        }
    }
}
