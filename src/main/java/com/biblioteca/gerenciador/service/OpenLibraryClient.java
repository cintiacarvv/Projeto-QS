package com.biblioteca.gerenciador.service;

import com.biblioteca.gerenciador.model.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class OpenLibraryClient {

    private final WebClient webClient;

    public OpenLibraryClient(@Value("${api.openlibrary.url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Book findBookByIsbn(String isbn) {
        try {
            JsonNode response = webClient.get()
                .uri("/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data", isbn)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response != null && response.has("ISBN:" + isbn)) {
                JsonNode bookData = response.get("ISBN:" + isbn);
                Book book = new Book();
                book.setIsbn(isbn);
                
                if (bookData.has("title")) {
                    book.setTitle(bookData.get("title").asText());
                }
                
                if (bookData.has("authors") && bookData.get("authors").isArray() && bookData.get("authors").size() > 0) {
                    book.setAuthor(bookData.get("authors").get(0).get("name").asText());
                }
                
                if (bookData.has("publishers") && bookData.get("publishers").isArray() && bookData.get("publishers").size() > 0) {
                    book.setPublisher(bookData.get("publishers").get(0).get("name").asText());
                }
                
                if (bookData.has("publish_date")) {
                    try {
                        String dateStr = bookData.get("publish_date").asText();
                        // simplistic extraction of year if it's a full date
                        if(dateStr.length() >= 4) {
                             // find the first 4-digit number
                             java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{4}").matcher(dateStr);
                             if(m.find()) {
                                 book.setPublishedYear(Integer.parseInt(m.group()));
                             }
                        }
                    } catch (Exception e) {
                        // ignore parsing error
                    }
                }
                
                return book;
            }
        } catch (WebClientResponseException e) {
            // handle api errors
        }
        return null;
    }
}
