package com.biblioteca.gerenciador.controller;

import com.biblioteca.gerenciador.AbstractIntegrationTest;
import com.biblioteca.gerenciador.model.Book;
import com.biblioteca.gerenciador.model.User;
import com.biblioteca.gerenciador.repository.BookRepository;
import com.biblioteca.gerenciador.repository.UserRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Testa o BookController — rotas de edição, deleção, validação e ISBN (VCR).
 *
 * Autenticação: feita via POST /login real no setUp() — sem .with(user(...)).
 * A sessão autenticada retornada pelo Spring Security é capturada como
 * MockHttpSession e reutilizada em todas as requisições do teste,
 * reproduzindo o ciclo de sessão real de um navegador.
 *
 * VCR com WireMock (modo declarativo):
 *  Os cassetes ficam em:
 *    src/test/resources/mappings/    → define URL + status HTTP
 *    src/test/resources/__files/     → corpo da resposta (JSON gravado)
 *  O WireMock carrega esses arquivos automaticamente ao subir.
 *  A propriedade api.openlibrary.url aponta para o WireMock local (porta 8090),
 *  garantindo que os testes rodam offline e de forma determinística.
 */
@SpringBootTest(properties = {"api.openlibrary.url=http://localhost:8090"})
@AutoConfigureMockMvc
@WireMockTest(httpPort = 8090)
class BookControllerExtendedE2ETest extends AbstractIntegrationTest {

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
        userRepository.save(
            new User("Test User", "tester", passwordEncoder.encode("password"))
        );

        // Login real via POST /login — captura a sessão autenticada
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tester")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"))
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    // =========================================================================
    // GET /books/add — formulário de adição de livro
    // =========================================================================

    @Test
    /*
     * Usuário acessa o formulário sem informar ISBN.
     * O controller cria um Book vazio e passa para o model.
     * Cobre: o caminho onde isbn == null no showAddForm().
     */
    void showAddForm_ShouldReturnEmptyForm_WhenNoIsbn() throws Exception {
        mockMvc.perform(get("/books/add").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("book"));
    }

    @Test
    /*
     * VCR — Cenário de SUCESSO: ISBN encontrado na API externa.
     *
     * O WireMock intercepta a chamada HTTP para a internet e devolve o cassete:
     *   mappings/isbn-encontrado.json          → define URL + status 200
     *   __files/isbn-encontrado-response.json  → corpo da resposta gravado
     *
     * O controller recebe o livro preenchido e coloca "successMessage" no model.
     * Cobre: o caminho where foundBook != null no showAddForm().
     */
    void showAddForm_ShouldFillForm_WhenIsbnFoundInApi() throws Exception {
        mockMvc.perform(get("/books/add")
                        .session(session)
                        .param("isbn", "9780132350884"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("successMessage"));
    }

    @Test
    /*
     * VCR — Cenário de ERRO 404: ISBN não encontrado na API externa.
     *
     * O WireMock devolve o cassete com status 404.
     * O OpenLibraryClient captura o WebClientResponseException e retorna null.
     * O controller coloca "errorMessage" no model.
     * Cobre: o caminho where foundBook == null no showAddForm().
     * Também cobre o bloco catch(WebClientResponseException) do OpenLibraryClient.
     */
    void showAddForm_ShouldShowError_WhenIsbnNotFoundInApi() throws Exception {
        mockMvc.perform(get("/books/add")
                        .session(session)
                        .param("isbn", "0000000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    /*
     * Usuário envia formulário de adição com campos obrigatórios em branco.
     * As anotações @NotBlank em Book disparam o BindingResult com erros.
     * O controller retorna o formulário novamente sem salvar nada.
     * Cobre: o bloco if (result.hasErrors()) do addBook().
     */
    void addBook_ShouldReturnForm_WhenValidationFails() throws Exception {
        mockMvc.perform(post("/books/add")
                        .session(session)
                        .with(csrf())
                        .param("title", "")
                        .param("author", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"));
    }

    // =========================================================================
    // GET /books/edit/{id} e POST /books/edit/{id}
    // =========================================================================

    @Test
    /*
     * Usuário acessa o formulário de edição de um livro existente.
     * O controller busca o livro pelo ID e coloca no model.
     * Cobre: o método showEditForm() e o BookService.findById().
     */
    void showEditForm_ShouldReturnFormWithBook() throws Exception {
        Book book = bookRepository.save(
            new Book("Livro Original", "Autor", "111", "Editora", 2020, false)
        );

        mockMvc.perform(get("/books/edit/" + book.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("book"));
    }

    @Test
    /*
     * Usuário envia o formulário de edição com dados válidos.
     * O controller seta o ID no livro, salva e redireciona para /books.
     * Cobre: o caminho de sucesso do updateBook().
     */
    void updateBook_ShouldSaveAndRedirect_WhenValidData() throws Exception {
        Book book = bookRepository.save(
            new Book("Título Original", "Autor", "222", "Editora", 2019, false)
        );

        mockMvc.perform(post("/books/edit/" + book.getId())
                        .session(session)
                        .with(csrf())
                        .param("title", "Título Atualizado")
                        .param("author", "Autor Atualizado")
                        .param("isbn", "222")
                        .param("publisher", "Editora")
                        .param("publishedYear", "2019")
                        .param("read", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
    }

    @Test
    /*
     * Usuário envia o formulário de edição com campos obrigatórios em branco.
     * O controller retorna o formulário com os erros de validação.
     * Cobre: o bloco if (result.hasErrors()) do updateBook().
     */
    void updateBook_ShouldReturnForm_WhenValidationFails() throws Exception {
        Book book = bookRepository.save(
            new Book("Livro", "Autor", "333", "Editora", 2021, false)
        );

        mockMvc.perform(post("/books/edit/" + book.getId())
                        .session(session)
                        .with(csrf())
                        .param("title", "")
                        .param("author", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"));
    }

    // =========================================================================
    // GET /books/delete/{id}
    // =========================================================================

    @Test
    /*
     * Usuário deleta um livro existente.
     * O controller chama bookService.deleteById() e redireciona para /books.
     * Verificamos também que o livro foi realmente removido do banco.
     * Cobre: o método deleteBook() e o BookService.deleteById().
     */
    void deleteBook_ShouldDeleteAndRedirect() throws Exception {
        Book book = bookRepository.save(
            new Book("Livro para Deletar", "Autor", "444", "Editora", 2022, false)
        );

        mockMvc.perform(get("/books/delete/" + book.getId()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        assert bookRepository.findById(book.getId()).isEmpty();
    }
}