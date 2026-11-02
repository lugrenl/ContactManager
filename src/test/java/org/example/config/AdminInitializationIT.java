package org.example.config;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.example.entity.User;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "admin.username=configuredadmin",
        "admin.password=ConfiguredPass123",
        "admin.email=configured-admin@example.com"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminInitializationIT {

    private static final String ADMIN_USERNAME = "configuredadmin";
    private static final String ADMIN_PASSWORD = "ConfiguredPass123";
    private static final String ADMIN_EMAIL = "configured-admin@example.com";

    @LocalServerPort
    private int port;

    @Autowired
    private ContactsManagerConfig config;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void configuredAdminIsCreatedWithEncodedPasswordAndAdminRole() {
        User admin = userService.findUserByUsername(ADMIN_USERNAME).orElseThrow();

        assertThat(admin.getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(admin.getEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(admin.getPassword()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(admin.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword())).isTrue();
    }

    @Test
    void configuredAdminCanLogInAndUseAdminApi() {
        String token = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "password": "%s"
                        }
                        """.formatted(ADMIN_USERNAME, ADMIN_PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("jwt-token", notNullValue())
                .extract()
                .path("jwt-token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/users")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void repeatedInitializationKeepsSingleAccountAndDoesNotResetPassword() {
        User before = userService.findUserByUsername(ADMIN_USERNAME).orElseThrow();

        User recreated = config.createAdminUser();

        User after = userService.findUserByUsername(ADMIN_USERNAME).orElseThrow();
        assertThat(recreated.getId()).isEqualTo(before.getId());
        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getPassword()).isEqualTo(before.getPassword());
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, after.getPassword())).isTrue();
        assertThat(userService.getAllUsers().stream()
                .filter(user -> ADMIN_USERNAME.equals(user.getName()))
                .count()).isEqualTo(1);
    }
}
