package org.example.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PersonalContactsIT {

    private static final String ALICE = "aliceuser";
    private static final String BOB = "bobuser";
    private static final String PASSWORD = "contactpass123";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String aliceToken;
    private String bobToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        aliceToken = register(ALICE, PASSWORD, "alice@example.com");
        bobToken = register(BOB, PASSWORD, "bob@example.com");
        adminToken = login("admin", "adminpass123");
    }

    @Test
    void identicalContactsOfDifferentUsersAreIndependentAndAdminSeesOwners() {
        int aliceContactId = createContact(aliceToken, "Shared", "Person", "shared@example.com", "1234567890");
        int bobContactId = createContact(bobToken, "Shared", "Person", "shared@example.com", "1234567890");

        assertThat(aliceContactId).isNotEqualTo(bobContactId);

        long aliceId = userId(ALICE);
        long bobId = userId(BOB);

        List<Integer> ids = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/contacts/admin")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("id");

        List<Integer> ownerIds = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/contacts/admin")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("ownerId");

        assertThat(ids).contains(aliceContactId, bobContactId);
        assertThat(ownerIds).contains(Math.toIntExact(aliceId), Math.toIntExact(bobId));
    }

    @Test
    void repeatedAddForOneUserDeduplicatesIncludingNullOptionalFields() {
        int first = createContact(aliceToken, "Local", "Person", null, "1234567890");
        int second = createContact(aliceToken, "Local", "Person", null, "1234567890");
        int third = createContact(aliceToken, "Other", "Person", null, null);
        int fourth = createContact(aliceToken, "Other", "Person", null, null);

        assertThat(second).isEqualTo(first);
        assertThat(fourth).isEqualTo(third);

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2));
    }

    @Test
    void updateKeepsIdAndDoesNotAffectOtherUsersContact() {
        int aliceContactId = createContact(aliceToken, "Shared", "Person", "shared@example.com", "1234567890");
        int bobContactId = createContact(bobToken, "Shared", "Person", "shared@example.com", "1234567890");

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(ContentType.JSON)
                .body(contactBody("Alice", "Updated", "alice.updated@example.com", "5555555555"))
                .when()
                .put("/api/contacts/" + aliceContactId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(aliceContactId))
                .body("name", equalTo("Alice"));

        given()
                .header("Authorization", "Bearer " + bobToken)
                .when()
                .get("/api/contacts/" + bobContactId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("Shared"))
                .body("email", equalTo("shared@example.com"));
    }

    @Test
    void updateToAnotherOwnContactReturnsConflict() {
        int firstId = createContact(aliceToken, "First", "Person", "first@example.com", null);
        int secondId = createContact(aliceToken, "Second", "Person", "second@example.com", null);

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(ContentType.JSON)
                .body(contactBody("First", "Person", "first@example.com", null))
                .when()
                .put("/api/contacts/" + secondId)
                .then()
                .statusCode(HttpStatus.CONFLICT.value());

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get("/api/contacts/" + secondId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("Second"));

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get("/api/contacts/" + firstId)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void otherUserCannotReadUpdateOrDeleteContact() {
        int contactId = createContact(aliceToken, "Private", "Person", "private@example.com", null);

        given()
                .header("Authorization", "Bearer " + bobToken)
                .when()
                .get("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .header("Authorization", "Bearer " + bobToken)
                .contentType(ContentType.JSON)
                .body(contactBody("Hacked", "Person", "hacked@example.com", null))
                .when()
                .put("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .header("Authorization", "Bearer " + bobToken)
                .when()
                .delete("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get("/api/contacts/" + contactId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("Private"));
    }

    @Test
    void deletingOneUsersContactDoesNotAffectAnotherUsersContact() {
        int aliceContactId = createContact(aliceToken, "Shared", "Person", "shared@example.com", null);
        int bobContactId = createContact(bobToken, "Shared", "Person", "shared@example.com", null);

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .delete("/api/contacts/" + aliceContactId)
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get("/api/contacts/" + aliceContactId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());

        given()
                .header("Authorization", "Bearer " + bobToken)
                .when()
                .get("/api/contacts/" + bobContactId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("Shared"));
    }

    @Test
    void importCreatesPersonalContactsAndDeduplicatesLocally() {
        String csv = "Imported,Person,imported@example.com,1234567890\n"
                + "Imported,Person,imported@example.com,1234567890\n"
                + "Second,Person,second@example.com,\n";

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .multiPart("file", "contacts.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .when()
                .post("/api/contacts/import")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .header("Authorization", "Bearer " + aliceToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("name", org.hamcrest.Matchers.containsInAnyOrder("Imported", "Second"));

        given()
                .header("Authorization", "Bearer " + bobToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(0));
    }

    @Test
    void deletingUserDeletesPersonalContacts() {
        int contactId = createContact(aliceToken, "Cascade", "Person", "cascade@example.com", null);
        long aliceId = userId(ALICE);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/api/users/" + aliceId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from contacts where contact_id = ?", Integer.class, contactId);
        assertThat(remaining).isZero();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/contacts/admin")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("ownerId", not(org.hamcrest.Matchers.hasItem(Math.toIntExact(aliceId))));
    }

    private String register(String name, String password, String email) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "password": "%s",
                            "email": "%s"
                        }
                        """.formatted(name, password, email))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("jwt-token");
    }

    private String login(String name, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "password": "%s"
                        }
                        """.formatted(name, password))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("jwt-token");
    }

    private int createContact(String token, String name, String surname, String email, String phone) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(contactBody(name, surname, email, phone))
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");
    }

    private String contactBody(String name, String surname, String email, String phone) {
        return """
                {
                    "name": "%s",
                    "surname": "%s",
                    "email": %s,
                    "phoneNumber": %s
                }
                """.formatted(name, surname, jsonValue(email), jsonValue(phone));
    }

    private String jsonValue(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private long userId(String name) {
        return given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("find { it.name == '" + name + "' }.id");
    }
}
