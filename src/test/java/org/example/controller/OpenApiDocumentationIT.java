package org.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OpenApiDocumentationIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    private JsonNode apiDocs;

    @BeforeEach
    void setUp() throws IOException {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        String body = given()
                .when()
                .get("/v3/api-docs")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .asString();

        apiDocs = objectMapper.readTree(body);
    }

    @Test
    void publicAuthEndpointsDoNotRequireBearerToken() {
        assertThat(path("/api/auth/register", "post", "security").isMissingNode()).isTrue();
        assertThat(path("/api/auth/login", "post", "security").isMissingNode()).isTrue();
    }

    @Test
    void logoutRequiresBearerTokenAndReturnsPlainText() {
        assertSecurityContainsBearerAuth("/api/auth/logout", "post");

        JsonNode success = apiDocs.path("paths").path("/api/auth/logout")
                .path("post").path("responses").path("200");
        assertThat(success.path("content").has("text/plain")).isTrue();
    }

    @Test
    void protectedControllersDeclareBearerSecurity() {
        assertSecurityContainsBearerAuth("/api/contacts", "get");
        assertSecurityContainsBearerAuth("/api/contacts/{contactId}", "delete");
        assertSecurityContainsBearerAuth("/api/users", "get");
        assertSecurityContainsBearerAuth("/api/contacts/admin", "get");
        assertSecurityContainsBearerAuth("/api/contacts/import", "post");
    }

    @Test
    void deleteContactDocumentsActualStatusCode() {
        JsonNode responses = apiDocs.path("paths").path("/api/contacts/{contactId}")
                .path("delete").path("responses");

        assertThat(responses.has("200")).isTrue();
        assertThat(responses.has("204")).isFalse();
    }

    @Test
    void authResponsesUseExplicitJwtSchema() {
        JsonNode jwtProperty = apiDocs.path("components").path("schemas")
                .path("JwtResponse").path("properties").path("jwt-token");

        assertThat(jwtProperty.isMissingNode()).isFalse();
        assertThat(jwtProperty.path("type").asText()).isEqualTo("string");
    }

    @Test
    void updateUserRequestMarksPasswordAsRequiredAndRoleAsOptional() {
        JsonNode required = apiDocs.path("components").path("schemas")
                .path("RequestUserDto").path("required");

        assertThat(required.isArray()).isTrue();
        assertThat(required.toString()).contains("\"name\"").contains("\"password\"").contains("\"email\"");
        assertThat(required.toString()).doesNotContain("\"role\"");
    }

    @Test
    void adminContactsExposeOwnerIdAndConflictResponseIsDocumented() {
        JsonNode adminProperties = apiDocs.path("components").path("schemas")
                .path("AdminContactDto").path("properties");
        assertThat(adminProperties.has("ownerId")).isTrue();

        JsonNode adminResponse = apiDocs.path("paths").path("/api/contacts/admin")
                .path("get").path("responses").path("200")
                .path("content").path("application/json").path("schema");
        assertThat(adminResponse.path("items").path("$ref").asText()).endsWith("/AdminContactDto");

        JsonNode updateResponses = apiDocs.path("paths").path("/api/contacts/{contactId}")
                .path("put").path("responses");
        assertThat(updateResponses.has("409")).isTrue();
    }

    @Test
    void importUsesMultipartFileUpload() {
        JsonNode operation = apiDocs.path("paths").path("/api/contacts/import").path("post");
        JsonNode multipart = operation.path("requestBody").path("content").path("multipart/form-data");

        assertThat(multipart.isMissingNode()).isFalse();

        JsonNode schema = multipart.path("schema");
        JsonNode file = schema.path("properties").path("file");
        assertThat(file.path("type").asText()).isEqualTo("string");
        assertThat(file.path("format").asText()).isEqualTo("binary");
        assertThat(schema.path("required").toString()).contains("\"file\"");
        assertThat(operation.path("responses").has("413")).isTrue();
    }

    @Test
    void adminEndpointsDoNotDocumentUnusedForbiddenResponse() {
        JsonNode responses = apiDocs.path("paths").path("/api/users")
                .path("get").path("responses");

        assertThat(responses.has("401")).isTrue();
        assertThat(responses.has("403")).isFalse();
    }

    private JsonNode path(String url, String method, String field) {
        return apiDocs.path("paths").path(url).path(method).path(field);
    }

    private void assertSecurityContainsBearerAuth(String url, String method) {
        JsonNode security = path(url, method, "security");

        assertThat(security.isArray()).isTrue();
        assertThat(security.toString()).contains("bearerAuth");
    }
}
