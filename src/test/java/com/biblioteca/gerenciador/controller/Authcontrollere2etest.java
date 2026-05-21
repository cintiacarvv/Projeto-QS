package com.biblioteca.gerenciador.controller;

import com.biblioteca.gerenciador.AbstractIntegrationTest;
import com.biblioteca.gerenciador.model.User;
import com.biblioteca.gerenciador.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Testa o AuthController — páginas de login e cadastro (Caixa Preta / E2E).
 *
 * Ferramentas:
 *  - @SpringBootTest: sobe o contexto completo do Spring (todos os beans reais)
 *  - @AutoConfigureMockMvc: configura o MockMvc para simular requisições HTTP
 *  - AbstractIntegrationTest: sobe MongoDB real via Testcontainers (Docker)
 *  - MockMvc: simula o navegador — faz GET/POST sem precisar de servidor rodando
 *
 * Sem nenhum Mock de componente — tudo é real: controller, service, banco.
 * A autenticação é verificada diretamente pelo fluxo real de POST /login,
 * sem qualquer atalho sintético de autenticação.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerE2ETest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // =========================================================================
    // GET /login
    // =========================================================================

    @Test
    /*
     * Qualquer pessoa (sem login) pode acessar a página de login.
     * O controller simplesmente retorna a view "login".
     * Cobre: o método loginPage() do AuthController.
     */
    void loginPage_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // =========================================================================
    // GET /register
    // =========================================================================

    @Test
    /*
     * Qualquer pessoa pode acessar o formulário de cadastro.
     * O controller coloca um objeto User vazio no model.
     * Cobre: o método registerPage() do AuthController.
     */
    void registerPage_ShouldReturnRegisterViewWithUserObject() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    // =========================================================================
    // POST /register — cenários do processRegistration()
    // =========================================================================

    @Test
    /*
     * Cenário feliz: usuário preenche todos os campos corretamente.
     * O controller registra o usuário e redireciona para /login?registered.
     * Cobre: o caminho de sucesso do processRegistration().
     */
    void processRegistration_ShouldRedirect_WhenValidData() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Maria Souza")
                        .param("username", "mariasouza")
                        .param("password", "senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    /*
     * Cenário de erro de validação: senha com menos de 6 caracteres.
     * A anotação @Size(min=6) no model User rejeita a entrada.
     * Cobre: o bloco if (result.hasErrors()) do processRegistration().
     */
    void processRegistration_ShouldReturnRegister_WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Maria Souza")
                        .param("username", "mariasouza")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    /*
     * Cenário de erro de validação: campos obrigatórios em branco.
     * As anotações @NotBlank no model User rejeitam valores vazios.
     * Cobre: o bloco if (result.hasErrors()) com múltiplos erros.
     */
    void processRegistration_ShouldReturnRegister_WhenFieldsAreBlank() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "")
                        .param("username", "")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    /*
     * Cenário de username duplicado: o UserService lança IllegalArgumentException.
     * O controller captura a exceção e coloca "errorMessage" no model.
     * Cobre: o bloco catch (IllegalArgumentException e) do processRegistration().
     */
    void processRegistration_ShouldShowError_WhenUsernameAlreadyExists() throws Exception {
        userRepository.save(
            new User("Usuário Existente", "duplicado", passwordEncoder.encode("senha123"))
        );

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Outro Nome")
                        .param("username", "duplicado")
                        .param("password", "senha123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // =========================================================================
    // POST /login — fluxo de autenticação real
    // =========================================================================

    @Test
    /*
     * Login com credenciais corretas: Spring Security autentica contra o MongoDB
     * (Testcontainers) e redireciona para /books.
     * Este teste prova que o fluxo real de autenticação funciona — e é
     * exatamente o mesmo mecanismo usado nos testes de BookController
     * para obter a sessão autenticada.
     * Cobre: o fluxo formLogin() configurado no SecurityConfig.
     */
    void login_ShouldRedirectToBooks_WhenValidCredentials() throws Exception {
        userRepository.save(
            new User("Test User", "tester", passwordEncoder.encode("password"))
        );

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tester")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
    }

    @Test
    /*
     * Login com senha errada: Spring Security rejeita e redireciona para /login?error.
     * Cobre: o caminho de falha do formLogin().
     */
    void login_ShouldRedirectToError_WhenInvalidCredentials() throws Exception {
        userRepository.save(
            new User("Test User", "tester", passwordEncoder.encode("password"))
        );

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tester")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}