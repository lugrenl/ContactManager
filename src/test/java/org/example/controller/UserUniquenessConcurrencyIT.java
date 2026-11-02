package org.example.controller;

import org.example.entity.User;
import org.example.service.RegistrationService;
import org.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserUniquenessConcurrencyIT {

    private static final String USERNAME = "concurrentuser";

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @Test
    void concurrentRegistrationOfSameNameCreatesOnlyOneUser() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = executor.submit(() -> register(ready, start, "concurrent1@example.com"));
            Future<Boolean> second = executor.submit(() -> register(ready, start, "concurrent2@example.com"));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Boolean> results = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(userService.findUserByUsername(USERNAME)).isPresent();
            assertThat(userService.getAllUsers().stream()
                    .filter(user -> USERNAME.equals(user.getName()))
                    .count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean register(CountDownLatch ready, CountDownLatch start, String email) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            User user = new User();
            user.setName(USERNAME);
            user.setPassword("concurrentpass123");
            user.setEmail(email);
            registrationService.register(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}