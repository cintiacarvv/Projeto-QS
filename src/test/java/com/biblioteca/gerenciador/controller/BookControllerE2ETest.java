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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Testa o BookController de ponta a ponta (Caixa Preta / E2E).
 *
 * Autenticação: feita via POST /login real — sem .with(user(...)).
 * A sessão autenticada retornada pelo Spring Security é capturada como
 * MockHttpSession e reutilizada nas requisições seguintes, exatamente
 * como um navegador real faria com cookies de sessão.
 *
 * Sem mocks de componente — controller, service, repositório e banco
 * (MongoDB via Testcontainers) são todos reais.
 */
@SpringBootTest(properties = {"api.openlibrary.url=http://localhost:8090"})
@AutoConfigureMockMvc
@WireMockTest(httpPort = 8090)
class BookControllerE2ETest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Sessão autenticada obtida após login real — reutilizada em cada teste
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        bookRepository.deleteAll();

        // Salva usuário de teste com senha codificada (BCrypt real)
        userRepository.save(new User("Test User", "tester", passwordEncoder.encode("password")));

        // Faz o login real via POST /login e captura a sessão autenticada
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tester")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"))
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        bookRepository.deleteAll();
    }

    // =========================================================================
    // Acesso não autenticado
    // =========================================================================

    @Test
    /*
     * Requisição sem sessão deve ser redirecionada para /login.
     * Cobre: a regra anyRequest().authenticated() do SecurityConfig.
     */
    void listBooks_ShouldReturnRedirect_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // GET /books — listagem de livros
    // =========================================================================

    @Test
    /*
     * Usuário autenticado (sessão real) acessa /books.
     * O controller carrega a lista do banco e renderiza books/list.
     * Cobre: o método listBooks() do BookController.
     */
    void listBooks_ShouldReturnBooks_WhenAuthenticated() throws Exception {
        bookRepository.save(new Book("Test Book", "Author", "123", "Pub", 2020, true));

        mockMvc.perform(get("/books").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("books"));
    }

    // =========================================================================
    // POST /books/add — adicionar livro
    // =========================================================================

    @Test
    /*
     * Usuário autenticado envia formulário válido.
     * O controller salva o livro e redireciona para /books.
     * Cobre: o caminho de sucesso do addBook().
     */
    void addBook_ShouldSaveBook_WhenValidFormSubmitted() throws Exception {
        mockMvc.perform(post("/books/add")
                        .session(session)
                        .with(csrf())
                        .param("title", "New Book")
                        .param("author", "New Author")
                        .param("isbn", "9999")
                        .param("publisher", "New Pub")
                        .param("publishedYear", "2023")
                        .param("read", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        assertEquals(1, bookRepository.findAll().size());
        assertEquals("New Book", bookRepository.findAll().get(0).getTitle());
    }
}