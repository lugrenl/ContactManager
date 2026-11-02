package org.example.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContactImportIT {

    private static final String CSV_CONTENT_TYPE = "text/csv";

    @LocalServerPort
    private int port;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        token = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "testuser",
                            "password": "testpass123"
                        }
                        """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("jwt-token");
    }

    @Test
    void importsQuotedFieldsAndTrailingEmptyFieldWithoutShiftingColumns() {
        importCsv("\"John, Jr.\",Doe,john@example.com,\n"
                + "\"Jane \"\"JJ\"\" Smith\",Smith,jane@example.com,555\n")
                .statusCode(HttpStatus.OK.value());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("name", containsInAnyOrder("John, Jr.", "Jane \"JJ\" Smith"))
                .body("find { it.name == 'John, Jr.' }.surname", equalTo("Doe"))
                .body("find { it.name == 'John, Jr.' }.email", equalTo("john@example.com"))
                .body("find { it.name == 'John, Jr.' }.phoneNumber", nullValue())
                .body("find { it.name == 'Jane \"JJ\" Smith' }.phoneNumber", equalTo("555"));
    }

    @Test
    void invalidRowDoesNotPartiallyImportEarlierRows() {
        importCsv("Valid,Row,valid@example.com,123\n"
                + "Broken,Row,broken@example.com\n")
                .statusCode(HttpStatus.BAD_REQUEST.value());

        assertCurrentUserHasNoContacts();
    }

    @Test
    void emptyFileIsRejectedWithoutImporting() {
        importCsv("")
                .statusCode(HttpStatus.BAD_REQUEST.value());

        assertCurrentUserHasNoContacts();
    }

    @Test
    void oversizedFileIsRejectedWithoutImporting() {
        importCsv("A".repeat(1024 * 1024 + 1))
                .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value());

        assertCurrentUserHasNoContacts();
    }

    @Test
    void requestWithoutJwtIsRejected() {
        given()
                .multiPart("file", "contacts.csv",
                        "John,Doe,john@example.com,123".getBytes(StandardCharsets.UTF_8), CSV_CONTENT_TYPE)
                .when()
                .post("/api/contacts/import")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    private ValidatableResponse importCsv(String content) {
        return given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "contacts.csv",
                        content.getBytes(StandardCharsets.UTF_8), CSV_CONTENT_TYPE)
                .when()
                .post("/api/contacts/import")
                .then();
    }

    private void assertCurrentUserHasNoContacts() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(0));
    }
}
