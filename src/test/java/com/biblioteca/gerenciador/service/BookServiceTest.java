package com.biblioteca.gerenciador.service;

import com.biblioteca.gerenciador.AbstractIntegrationTest;
import com.biblioteca.gerenciador.model.Book;
import com.biblioteca.gerenciador.repository.BookRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
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
    void findBookByIsbnExternal_ShouldReturnBook_WhenApiReturnsData() {
        String isbn = "0451526538";
        
        // VCR Config
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
}
