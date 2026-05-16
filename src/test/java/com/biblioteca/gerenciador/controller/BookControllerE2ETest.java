package com.biblioteca.gerenciador.controller;

import com.biblioteca.gerenciador.AbstractIntegrationTest;
import com.biblioteca.gerenciador.model.Book;
import com.biblioteca.gerenciador.model.User;
import com.biblioteca.gerenciador.repository.BookRepository;
import com.biblioteca.gerenciador.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerE2ETest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        bookRepository.deleteAll();
        User testUser = new User("Test User", "tester", passwordEncoder.encode("password"));
        userRepository.save(testUser);
    }

    @Test
    void listBooks_ShouldReturnRedirect_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void listBooks_ShouldReturnBooks_WhenAuthenticated() throws Exception {
        bookRepository.save(new Book("Test Book", "Author", "123", "Pub", 2020, true));

        mockMvc.perform(get("/books").with(user("tester").password("password").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("books"));
    }

    @Test
    void addBook_ShouldSaveBook_WhenValidFormSubmitted() throws Exception {
        mockMvc.perform(post("/books/add")
                        .with(user("tester").password("password").roles("USER"))
                        .with(csrf())
                        .param("title", "New Book")
                        .param("author", "New Author")
                        .param("isbn", "9999")
                        .param("publisher", "New Pub")
                        .param("publishedYear", "2023")
                        .param("read", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        assert(bookRepository.findAll().size() == 1);
        assert(bookRepository.findAll().get(0).getTitle().equals("New Book"));
    }
}
