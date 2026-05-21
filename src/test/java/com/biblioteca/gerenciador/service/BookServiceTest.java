package com.biblioteca.gerenciador.service;

import com.biblioteca.gerenciador.AbstractIntegrationTest;
import com.biblioteca.gerenciador.model.Book;
import com.biblioteca.gerenciador.repository.BookRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"api.openlibrary.url=http://localhost:8080"})
@WireMockTest(httpPort = 8080)
class BookServiceTest extends AbstractIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @AfterEach
    void tearDown() {
        bookRepository.deleteAll();
    }

    @Test
    void saveAndFindBook() {
        Book book = new Book("O Senhor dos Anéis", "J.R.R. Tolkien", "1234567890", "HarperCollins", 1954, true);
        Book savedBook = bookService.save(book);

        assertNotNull(savedBook.getId());

        List<Book> books = bookService.findAll();
        assertEquals(1, books.size());
        assertEquals("O Senhor dos Anéis", books.get(0).getTitle());
    }

    @Test
    /*
     * VCR — JSON completo: todos os campos presentes.
     * Cobre: todos os caminhos "true" dos if(has(...)) do OpenLibraryClient.
     */
    void findBookByIsbnExternal_ShouldReturnBook_WhenApiReturnsData() {
        String isbn = "0451526538";

        stubFor(get(urlEqualTo("/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"ISBN:0451526538\": { \"title\": \"The Adventures of Tom Sawyer\", \"authors\": [{\"name\": \"Mark Twain\"}], \"publishers\": [{\"name\": \"Signet Classic\"}], \"publish_date\": \"1997\" } }")));

        Book foundBook = bookService.findByIsbnExternal(isbn);

        assertNotNull(foundBook);
        assertEquals("0451526538", foundBook.getIsbn());
        assertEquals("The Adventures of Tom Sawyer", foundBook.getTitle());
        assertEquals("Mark Twain", foundBook.getAuthor());
        assertEquals("Signet Classic", foundBook.getPublisher());
        assertEquals(1997, foundBook.getPublishedYear());

        verify(getRequestedFor(urlEqualTo("/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data")));
    }

    @Test
    /*
     * VCR — JSON mínimo: apenas o ISBN presente, sem title, authors,
     * publishers nem publish_date.
     * Cobre: todos os caminhos "false" dos if(has(...)) do OpenLibraryClient,
     * garantindo que nenhum campo opcional causa NullPointerException.
     */
    void findBookByIsbnExternal_ShouldReturnBookWithNulls_WhenApiReturnsMinimalData() {
        String isbn = "0000000001";

        stubFor(get(urlEqualTo("/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"ISBN:0000000001\": {} }")));

        Book foundBook = bookService.findByIsbnExternal(isbn);

        assertNotNull(foundBook);
        assertEquals("0000000001", foundBook.getIsbn());
        assertNull(foundBook.getTitle());
        assertNull(foundBook.getAuthor());
        assertNull(foundBook.getPublisher());
        assertEquals(0, foundBook.getPublishedYear());
    }

    @Test
    /*
     * VCR — API retorna JSON vazio (ISBN não encontrado na base).
     * Cobre: o caminho onde response.has("ISBN:...") é false.
     */
    void findBookByIsbnExternal_ShouldReturnNull_WhenIsbnNotInResponse() {
        String isbn = "0000000002";

        stubFor(get(urlEqualTo("/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        Book foundBook = bookService.findByIsbnExternal(isbn);

        assertNull(foundBook);
    }

    @Test
    /*
     * VCR — API retorna publish_date como texto sem ano de 4 dígitos.
     * Cobre: o caminho onde dateStr.length() >= 4 mas o regex não encontra
     * 4 dígitos seguidos (m.find() == false).
     */
    void findBookByIsbnExternal_ShouldHandleUnparsableDate() {
        String isbn = "0000000003";

        stubFor(get(urlEqualTo("/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"ISBN:0000000003\": { \"title\": \"Test\", \"publish_date\": \"abc\" } }")));

        Book foundBook = bookService.findByIsbnExternal(isbn);

        assertNotNull(foundBook);
        assertEquals("Test", foundBook.getTitle());
        assertEquals(0, foundBook.getPublishedYear());
    }

    @Test
    /*
     * VCR — API retorna authors como array vazio.
     * Cobre: o caminho onde isArray() == true mas size() == 0.
     */
    void findBookByIsbnExternal_ShouldHandleEmptyAuthorsArray() {
        String isbn = "0000000004";

        stubFor(get(urlEqualTo("/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"ISBN:0000000004\": { \"title\": \"Test\", \"authors\": [], \"publishers\": [] } }")));

        Book foundBook = bookService.findByIsbnExternal(isbn);

        assertNotNull(foundBook);
        assertNull(foundBook.getAuthor());
        assertNull(foundBook.getPublisher());
    }
}