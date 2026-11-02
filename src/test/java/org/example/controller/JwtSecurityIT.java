package org.example.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JwtSecurityIT {

    private static final String USER = "testuser";
    private static final String USER_PASSWORD = "testpass123";
    private static final String KNOWN_DEFAULT_SECRET = "SECRET";

    @LocalServerPort
    private int port;

    @Value("${jwt_secret}")
    private String jwtSecret;

    @Value("${jwt_expiration}")
    private long jwtExpirationMinutes;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void validJwtAllowsAccessToProtectedEndpoint() {
        String token = login(USER, USER_PASSWORD);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void expiredJwtIsRejected() {
        String token = signedToken(USER, Instant.now().minusSeconds(60), jwtSecret);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void jwtWithWrongSignatureIsRejected() {
        String token = signedToken(USER, Instant.now().plusSeconds(300), "another-secret-value-0123456789abcdef");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void malformedJwtIsRejected() {
        given()
                .header("Authorization", "Bearer not.a.valid.jwt")
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void tokenSignedWithKnownDefaultSecretIsRejected() {
        String token = signedToken(USER, Instant.now().plusSeconds(300), KNOWN_DEFAULT_SECRET);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void logoutRevokesJwt() {
        String token = login(USER, USER_PASSWORD);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(equalTo("Logged out successfully"));

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", containsString("revoked"));
    }

    @Test
    void repeatedLogoutWithRevokedTokenIsRejected() {
        String token = login(USER, USER_PASSWORD);
        logout(token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void logoutWithoutTokenIsRejected() {
        given()
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void logoutWithInvalidTokenIsRejected() {
        given()
                .header("Authorization", "Bearer invalid.token.value")
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void logoutDoesNotRevokeOtherTokensOfTheSameUser() {
        String firstToken = login(USER, USER_PASSWORD);
        String secondToken = login(USER, USER_PASSWORD);

        assertThat(firstToken).isNotEqualTo(secondToken);

        logout(firstToken);

        given()
                .header("Authorization", "Bearer " + secondToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void newLoginAfterLogoutIsUsable() {
        String token = login(USER, USER_PASSWORD);
        logout(token);

        String newToken = login(USER, USER_PASSWORD);

        given()
                .header("Authorization", "Bearer " + newToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void jwtOfDeletedUserIsRejectedWithoutServerError() {
        String deletedUserToken = register("deleteduser", "deletedpass123", "deleted@example.com");
        String adminToken = login(adminUsername, adminPassword);

        Long userId = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getLong("find { it.name == 'deleteduser' }.id");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/api/users/" + userId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header("Authorization", "Bearer " + deletedUserToken)
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", containsString("User not found"));
    }

    @Test
    void configuredExpirationIsAppliedToIssuedTokens() {
        String token = login(USER, USER_PASSWORD);

        Instant expiresAt = JWT.decode(token).getExpiresAt().toInstant();
        long minutes = Duration.between(Instant.now(), expiresAt).toMinutes();

        assertThat(jwtExpirationMinutes).isEqualTo(60);
        assertThat(minutes).isBetween(jwtExpirationMinutes - 2, jwtExpirationMinutes);
    }

    @Test
    void knownDefaultAdminPasswordIsRejected() {
        given()
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
                .statusCode(HttpStatus.UNAUTHORIZED.value());
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

    private void logout(String token) {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    private static String signedToken(String username, Instant expiresAt, String secret) {
        return JWT.create()
                .withClaim("username", username)
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.HMAC256(secret));
    }
}
