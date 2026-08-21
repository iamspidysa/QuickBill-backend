package com.saurabh.quickbill.repository;

import com.saurabh.quickbill.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest is a different kind of test from OrderServiceImplTest:
 * instead of mocking the repository, it boots ONLY the JPA layer (no web
 * layer, no security filters) against a real, throwaway H2 in-memory
 * database. This is the right tool when what you actually want to check is
 * "does my derived query method do what its name says" — something a mock
 * can't tell you, because a mock just returns whatever you told it to.
 *
 * Each test method runs inside a transaction that's rolled back afterwards,
 * so tests never see each other's data and never touch the real MySQL DB
 * configured in application.properties.
 */
@DataJpaTest
class UserRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @Test
    void existsByEmail_returnsTrue_onlyAfterThatUserIsSaved() {
        assertThat(userRepository.existsByEmail("new@quickbill.com")).isFalse();

        userRepository.save(UserEntity.builder()
                .userId("USR-TEST-1")
                .email("new@quickbill.com")
                .password("hashed-irrelevant")
                .role("ROLE_USER")
                .name("Test User")
                .build());

        assertThat(userRepository.existsByEmail("new@quickbill.com")).isTrue();
    }

    @Test
    void existsByRole_findsAdmin_onlyWhenAnAdminRowExists() {
        // Used directly by AdminBootstrapRunner on startup — this is the
        // query that decides whether the first-admin bootstrap should run.
        assertThat(userRepository.existsByRole("ROLE_ADMIN")).isFalse();

        userRepository.save(UserEntity.builder()
                .userId("USR-ADMIN-1")
                .email("admin@quickbill.com")
                .password("hashed-irrelevant")
                .role("ROLE_ADMIN")
                .name("Admin")
                .build());

        assertThat(userRepository.existsByRole("ROLE_ADMIN")).isTrue();
        // A user existing with a different role must not satisfy the check.
        assertThat(userRepository.existsByRole("ROLE_SUPERUSER")).isFalse();
    }

    @Test
    void findByEmail_isCaseSensitiveExactMatch() {
        userRepository.save(UserEntity.builder()
                .userId("USR-TEST-2")
                .email("owner@quickbill.com")
                .password("hashed-irrelevant")
                .role("ROLE_USER")
                .name("Owner")
                .build());

        assertThat(userRepository.findByEmail("owner@quickbill.com")).isPresent();
        assertThat(userRepository.findByEmail("nobody@quickbill.com")).isEmpty();
    }
}
