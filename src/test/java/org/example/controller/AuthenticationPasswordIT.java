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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthenticationPasswordIT {

    private static final String NAME = "passworduser";
    private static final String PASSWORD = "originalpass123";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void originalPasswordRemainsValidAfterContactOperations() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "password": "%s",
                            "email": "passworduser@example.com"
                        }
                        """.formatted(NAME, PASSWORD))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        String token = login(PASSWORD);

        Integer contactId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "John",
                            "surname": "Doe",
                            "email": "john.doe@example.com",
                            "phoneNumber": "+1234567890"
                        }
                        """)
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");

        Integer updatedContactId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Jane",
                            "surname": "Doe",
                            "email": "jane.doe@example.com",
                            "phoneNumber": "+0987654321"
                        }
                        """)
                .when()
                .put("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/contacts/" + updatedContactId)
                .then()
                .statusCode(HttpStatus.OK.value());

        login(PASSWORD);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "password": "wrongpassword"
                        }
                        """.formatted(NAME))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    private String login(String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "password": "%s"
                        }
                        """.formatted(NAME, password))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("jwt-token", notNullValue())
                .extract()
                .path("jwt-token");
    }
}