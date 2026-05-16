package com.biblioteca.gerenciador.service;

import com.biblioteca.gerenciador.AbstractIntegrationTest;
import com.biblioteca.gerenciador.model.User;
import com.biblioteca.gerenciador.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_ShouldHashPasswordAndSave() {
        User user = new User("João da Silva", "joao123", "senha123");
        User savedUser = userService.registerUser(user);

        assertNotNull(savedUser.getId());
        assertEquals("joao123", savedUser.getUsername());
        assertNotEquals("senha123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("senha123", savedUser.getPassword()));
        assertEquals("ROLE_USER", savedUser.getRole());
    }

    @Test
    void registerUser_ShouldThrowExceptionIfUsernameExists() {
        User user1 = new User("User One", "existing_user", "password");
        userService.registerUser(user1);

        User user2 = new User("User Two", "existing_user", "password");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(user2);
        });
        assertEquals("Nome de usuário já existe", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user1", "user2", "admin"})
    void registerUser_MultipleUsers_ShouldSaveAll(String username) {
        User user = new User("Test " + username, username, "pass123");
        User savedUser = userService.registerUser(user);
        assertNotNull(savedUser.getId());
        assertEquals(username, savedUser.getUsername());
    }
}
