package com.biblioteca.gerenciador.controller;

import com.biblioteca.gerenciador.model.Book;
import com.biblioteca.gerenciador.service.BookService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.findAll());
        return "books/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model, @RequestParam(value = "isbn", required = false) String isbn) {
        Book book = new Book();
        if (isbn != null && !isbn.isEmpty()) {
            Book foundBook = bookService.findByIsbnExternal(isbn);
            if (foundBook != null) {
                book = foundBook;
                model.addAttribute("successMessage", "Livro encontrado com sucesso!");
            } else {
                model.addAttribute("errorMessage", "Livro não encontrado para o ISBN informado.");
            }
        }
        model.addAttribute("book", book);
        return "books/form";
    }

    @PostMapping("/add")
    public String addBook(@Valid @ModelAttribute("book") Book book, BindingResult result) {
        if (result.hasErrors()) {
            return "books/form";
        }
        bookService.save(book);
        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        bookService.findById(id).ifPresent(book -> model.addAttribute("book", book));
        return "books/form";
    }

    @PostMapping("/edit/{id}")
    public String updateBook(@PathVariable("id") String id, @Valid @ModelAttribute("book") Book book, BindingResult result) {
        if (result.hasErrors()) {
            return "books/form";
        }
        book.setId(id);
        bookService.save(book);
        return "redirect:/books";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable("id") String id) {
        bookService.deleteById(id);
        return "redirect:/books";
    }
}
