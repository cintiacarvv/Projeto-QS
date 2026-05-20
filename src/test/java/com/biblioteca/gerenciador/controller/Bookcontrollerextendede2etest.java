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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Testa o BookController — rotas que ainda não tinham cobertura.
 *
 * VCR com WireMock (modo declarativo — seção 2.2 da aula):
 *  Os arquivos de resposta ficam em:
 *    src/test/resources/mappings/      → define URL + status HTTP
 *    src/test/resources/__files/       → corpo da resposta (JSON gravado)
 *
 *  O WireMock carrega esses arquivos automaticamente ao subir.
 *  Não inventamos respostas no código — usamos "cassetes" gravados,
 *  exatamente como o padrão VCR define.
 *
 * @SpringBootTest com a propriedade api.openlibrary.url apontando para
 * o WireMock local (porta 8090) em vez da internet real.
 * Isso garante que os testes rodam offline e de forma determinística.
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

    // Roda antes de cada teste: limpa o banco e cria um usuário de teste
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.save(
            new User("Test User", "tester", passwordEncoder.encode("password"))
        );
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
        mockMvc.perform(get("/books/add")
                        .with(user("tester").password("password").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("book"));
    }

    @Test
    /*
     * VCR — Cenário de SUCESSO: ISBN encontrado na API externa.
     *
     * O WireMock intercepta a chamada HTTP que o OpenLibraryClient faria
     * para a internet e devolve o cassete gravado em:
     *   mappings/isbn-encontrado.json       → define a URL e o status 200
     *   __files/isbn-encontrado-response.json → corpo da resposta real
     *
     * O controller recebe o livro preenchido e coloca "successMessage" no model.
     * Cobre: o caminho where foundBook != null no showAddForm().
     */
    void showAddForm_ShouldFillForm_WhenIsbnFoundInApi() throws Exception {
        // ISBN definido no cassete: mappings/isbn-encontrado.json
        mockMvc.perform(get("/books/add")
                        .with(user("tester").password("password").roles("USER"))
                        .param("isbn", "9780132350884"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("successMessage"));
    }

    @Test
    /*
     * VCR — Cenário de ERRO 404: ISBN não encontrado na API externa.
     *
     * O WireMock devolve o cassete:
     *   mappings/isbn-nao-encontrado.json → status 404
     *
     * O OpenLibraryClient captura o WebClientResponseException (HTTP 4xx/5xx)
     * e retorna null. O controller coloca "errorMessage" no model.
     * Cobre: o caminho where foundBook == null no showAddForm().
     * Também cobre o bloco catch(WebClientResponseException) do OpenLibraryClient.
     */
    void showAddForm_ShouldShowError_WhenIsbnNotFoundInApi() throws Exception {
        // ISBN definido no cassete: mappings/isbn-nao-encontrado.json
        mockMvc.perform(get("/books/add")
                        .with(user("tester").password("password").roles("USER"))
                        .param("isbn", "0000000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    /*
     * Usuário envia o formulário de adição com campos obrigatórios em branco.
     * As anotações @NotBlank em Book disparam o BindingResult com erros.
     * O controller retorna o formulário novamente sem salvar nada.
     * Cobre: o bloco if (result.hasErrors()) do addBook().
     */
    void addBook_ShouldReturnForm_WhenValidationFails() throws Exception {
        mockMvc.perform(post("/books/add")
                        .with(user("tester").password("password").roles("USER"))
                        .with(csrf())
                        .param("title", "")   // @NotBlank violado
                        .param("author", "")) // @NotBlank violado
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
        // Salvamos um livro real no banco para ter um ID válido
        Book book = bookRepository.save(
            new Book("Livro Original", "Autor", "111", "Editora", 2020, false)
        );

        mockMvc.perform(get("/books/edit/" + book.getId())
                        .with(user("tester").password("password").roles("USER")))
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
                        .with(user("tester").password("password").roles("USER"))
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
                        .with(user("tester").password("password").roles("USER"))
                        .with(csrf())
                        .param("title", "")   // @NotBlank violado
                        .param("author", "")) // @NotBlank violado
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

        mockMvc.perform(get("/books/delete/" + book.getId())
                        .with(user("tester").password("password").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        // Verificação real no banco — o livro não deve mais existir
        assert bookRepository.findById(book.getId()).isEmpty();
    }
}