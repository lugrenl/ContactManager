package org.example.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ContactsControllerIT {

    @LocalServerPort
    private int port;

    private String adminToken;
    private String userToken;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        // Login as admin
        adminToken = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "admin",
                        "password": "adminpass123"
                    }
                    """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("jwt-token");

        // Login as regular user
        userToken = given()
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
    public void whenGetAllContactsAsAdmin_thenReturnOk() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/contacts/admin")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    public void whenGetAllContactsAsUser_thenReturnUnauthorized() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/contacts/admin")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void whenImportContactsAsAdmin_thenReturnOk() {
        String csvContent = "John,Doe,testmail1@mail.com,1234567890\n" +
                            "Jane,Smith,testmail2@mail.com,0987654321";

        given()
                .header("Authorization", "Bearer " + adminToken)
                .multiPart("file", "contacts.csv",
                        csvContent.getBytes(StandardCharsets.UTF_8), "text/csv")
                .when()
                .post("/api/contacts/import")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void whenImportContactsAsUser_thenReturnOk() {
        String csvContent = "John,Doe,john@example.com,1234567890\n" +
                            "Jane,Smith,jane@example.com,0987654321";

        given()
                .header("Authorization", "Bearer " + userToken)
                .multiPart("file", "contacts.csv",
                        csvContent.getBytes(StandardCharsets.UTF_8), "text/csv")
                .when()
                .post("/api/contacts/import")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void whenImportContactsWithoutFilePart_thenReturnBadRequest() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .multiPart("other", "contacts.csv",
                        "John,Doe,john@example.com,123".getBytes(StandardCharsets.UTF_8), "text/csv")
                .when()
                .post("/api/contacts/import")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void whenAccessProtectedEndpointWithoutToken_thenReturnUnauthorized() {
        given()
                .when()
                .get("/api/contacts/admin")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}