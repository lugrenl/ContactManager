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
public class ContactControllerIT {

    @LocalServerPort
    private int port;

    private String authToken;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        
        // Authenticate and get token
        authToken = given()
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
    public void whenAddValidContact_thenReturnsCreated() {
        given()
                .header("Authorization", "Bearer " + authToken)
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
                .body("name", equalTo("John"))
                .body("surname", equalTo("Doe"))
                .body("email", equalTo("john.doe@example.com"))
                .body("phoneNumber", equalTo("+1234567890"))
                .body("id", notNullValue());
    }

    @Test
    public void whenAddInvalidContact_thenReturnsBadRequest() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "Jane",
                        "surname": "Doe",
                        "email": "invalid-email"
                    }
                    """)
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(not(emptyOrNullString()));
    }

    @Test
    public void whenGetExistingContact_thenReturnsContact() {
        // First add a contact
        Integer contactId = given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "Jane",
                        "surname": "Smith",
                        "email": "jane.smith@example.com"
                    }
                    """)
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");

        // Then get the contact
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(contactId))
                .body("name", equalTo("Jane"))
                .body("surname", equalTo("Smith"));
    }

    @Test
    public void whenUpdateContact_thenReturnsUpdatedContact() {
        // First add a contact
        Integer contactId = given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "Original",
                        "surname": "Name",
                        "email": "original@example.com"
                    }
                    """)
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");

        // Then update the contact
        given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "Updated",
                        "surname": "Name",
                        "email": "updated@example.com"
                    }
                    """)
                .when()
                .put("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(contactId+1))
                .body("name", equalTo("Updated"))
                .body("email", equalTo("updated@example.com"));
    }

    @Test
    public void whenDeleteContact_thenReturnsNoContent() {
        // First add a contact
        Integer contactId = given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "ToDelete",
                        "surname": "Contact",
                        "email": "delete@example.com"
                    }
                    """)
                .when()
                .post("/api/contacts")
                .then()
                .extract()
                .path("id");

        // Then delete the contact
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.OK.value());

        // Verify the contact is deleted
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void whenGetAllContactsForCurrentUser_thenReturnsContacts() {
        // Add a couple of contacts
        given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "First",
                        "surname": "Contact",
                        "email": "first@example.com"
                    }
                    """)
                .when()
                .post("/api/contacts");

        given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "Second",
                        "surname": "Contact",
                        "email": "second@example.com"
                    }
                    """)
                .when()
                .post("/api/contacts");

        // Get all contacts for current user
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", greaterThanOrEqualTo(2));
    }
}
