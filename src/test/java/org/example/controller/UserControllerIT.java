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
                .statusCode(HttpStatus.OK.value())
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
    public void whenGetAllUsersAsUser_thenReturnsUnauthorized() {
        given()
                .header("Authorization", "Bearer " + userAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
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
    public void whenUpdateUserAsUser_thenReturnsUnauthorized() {
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
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void whenDeleteUserAsAdmin_thenReturnsOK() {
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
                .statusCode(HttpStatus.OK.value())
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
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void whenAdminUpdatesUser_thenContactsAndLinksArePreserved() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "contactsuser",
                        "password": "contactspass123",
                        "email": "contactsuser@example.com"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        String contactsUserToken = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "contactsuser",
                        "password": "contactspass123"
                    }
                    """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("jwt-token");

        Integer firstContactId = given()
                .header("Authorization", "Bearer " + contactsUserToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "First",
                        "surname": "Contact",
                        "email": "first.contact@example.com"
                    }
                    """)
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");

        Integer secondContactId = given()
                .header("Authorization", "Bearer " + contactsUserToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "Second",
                        "surname": "Contact",
                        "email": "second.contact@example.com"
                    }
                    """)
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");

        Long userId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("find { it.name == 'contactsuser' }.id");

        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "contactsuser",
                        "email": "contactsuser.updated@example.com",
                        "password": "newcontactspass123",
                        "role": "ROLE_USER"
                    }
                    """)
                .when()
                .put("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("contactIds", containsInAnyOrder(firstContactId, secondContactId));

        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("email", equalTo("contactsuser.updated@example.com"))
                .body("contactIds", containsInAnyOrder(firstContactId, secondContactId));

        given()
                .header("Authorization", "Bearer " + contactsUserToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2));
    }

    @Test
    public void whenAdminChangesRole_thenNewRoleIsPersistedAndUsedForAuthorization() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "roleuser",
                        "password": "rolepass123",
                        "email": "roleuser@example.com"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        String roleUserToken = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "roleuser",
                        "password": "rolepass123"
                    }
                    """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("jwt-token");

        Long userId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("find { it.name == 'roleuser' }.id");

        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "roleuser",
                        "email": "roleuser@example.com",
                        "password": "newrolepass123",
                        "role": "ROLE_ADMIN"
                    }
                    """)
                .when()
                .put("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("role", equalTo("ROLE_ADMIN"));

        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("role", equalTo("ROLE_ADMIN"));

        given()
                .header("Authorization", "Bearer " + roleUserToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void whenRegisteringDuplicateName_thenReturnsConflict() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "duplicateuser",
                        "password": "duplicatepass123",
                        "email": "duplicate1@example.com"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "duplicateuser",
                        "password": "duplicatepass456",
                        "email": "duplicate2@example.com"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    public void whenAdminRenamesUserToOccupiedName_thenReturnsConflictAndKeepsOriginalName() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "firstowner",
                        "password": "firstpass123",
                        "email": "firstowner@example.com"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "secondowner",
                        "password": "secondpass123",
                        "email": "secondowner@example.com"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        Long firstUserId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("find { it.name == 'firstowner' }.id");

        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "secondowner",
                        "email": "firstowner@example.com",
                        "password": "firstpass123",
                        "role": "ROLE_USER"
                    }
                    """)
                .when()
                .put("/api/users/" + firstUserId)
                .then()
                .statusCode(HttpStatus.CONFLICT.value());

        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users/" + firstUserId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("firstowner"));
    }

    @Test
    public void whenAdminSetsUnsupportedRole_thenReturnsBadRequest() {
        Long userId = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("[0].id");

        given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "admin",
                        "email": "admin@example.com",
                        "password": "adminpass123",
                        "role": "ROLE_SUPERUSER"
                    }
                    """)
                .when()
                .put("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
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
