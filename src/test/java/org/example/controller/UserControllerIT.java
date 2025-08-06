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
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerIT {

    @LocalServerPort
    private int port;

    private String adminAuthToken;
    private String userAuthToken;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        
        // Login as admin
        adminAuthToken = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "admin",
                        "password": "12345678"
                    }
                    """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("jwt-token");

        // Login as regular user
        userAuthToken = given()
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
                .statusCode(200)
                .extract()
                .path("jwt-token");
    }

    @Test
    public void whenGetAllUsersAsAdmin_thenReturnsUsers() {
        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    public void whenGetAllUsersAsUser_thenReturnsForbidden() {
        given()
                .header("Authorization", "Bearer " + userAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void whenGetUserByIdAsAdmin_thenReturnsUser() {
        // First get all users to get an ID
        Long userId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("[0].id");

        // Then get the user by ID
        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(userId.intValue()));
    }

    @Test
    public void whenUpdateUserAsAdmin_thenReturnsUpdatedUser() {
        // First get a user to update
        Long userId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("[0].id");

        // Update the user
        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "updateduser",
                        "email": "updated@example.com",
                        "password": "newpassword123"
                    }
                    """)
                .when()
                .put("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("updateduser"))
                .body("email", equalTo("updated@example.com"));
    }

    @Test
    public void whenUpdateUserAsUser_thenReturnsForbidden() {
        given()
                .header("Authorization", "Bearer " + userAuthToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "unauthorized",
                        "email": "unauthorized@example.com"
                    }
                    """)
                .when()
                .put("/api/users/1")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void whenDeleteUserAsAdmin_thenReturnsNoContent() {
        // First create a new user to delete
        String newUserToken = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "tobedeleted",
                        "password": "deletepass123",
                        "email": "delete@example.com"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("jwt-token");

        // Get the new user's ID
        Long userId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .extract()
                .jsonPath()
                .getLong("find { it.name == 'tobedeleted' }.id");

        // Delete the user
        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .delete("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verify the user is deleted
        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value()); // TODO: switch to 404
    }

    @Test
    public void whenUpdateUserWithInvalidData_thenReturnsBadRequest() {
        // First get a user to update
        Long userId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("[0].id");

        // Try to update with invalid data
        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "",
                        "email": "invalid-email"
                    }
                    """)
                .when()
                .put("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(not(emptyOrNullString()));
    }
}
