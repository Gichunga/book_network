package com.gichungasoftwares.book_network.book;

import com.gichungasoftwares.book_network.common.PageResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "Book")
@RequestMapping("books")
public class BookController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<Integer> saveBook(
            @Valid @RequestBody BookRequest bookRequest, Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.save(bookRequest, connectedUser));
    }

    @GetMapping("{book-id}")
    public ResponseEntity<BookResponse> findBookById(
            @PathVariable("book-id") Integer bookId
    ) {
        return ResponseEntity.ok(bookService.findById(bookId));
    }

    // find all displayable books
    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> findAllBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.findAllBooks(page, size, connectedUser));
    }

    // fetch all books by the owner
    @GetMapping("/owner")
    public ResponseEntity<PageResponse<BookResponse>> findAllBooksByOwner(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.findAllBooksByOwner(page, size, connectedUser));
    }

    // find all borrowed books
    @GetMapping("/borrowed")
    public ResponseEntity<PageResponse<BorrowedBookResponse>> findAllBorrowedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.findAllBorrowedBooks(page, size, connectedUser));
    }

    // find all borrowed books
    @GetMapping("/returned")
    public ResponseEntity<PageResponse<BorrowedBookResponse>> findAllReturnedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.findAllReturnedBooks(page, size, connectedUser));
    }

    // update shareable status
    @PatchMapping("/shareable/{book-id}")
    public ResponseEntity<Integer> updateShareableStatus(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.updateShareableStatus(bookId, connectedUser));
    }

    // update archived status
    @PatchMapping("/archived/{book-id}")
    public ResponseEntity<Integer> updateArchivedStatus(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.updateArchivedStatus(bookId, connectedUser));
    }

    // borrow book
    @PostMapping("borrow/{book-id}")
    public ResponseEntity<Integer> borrowBook(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.borrowBook(bookId, connectedUser));
    }

    // return borrowed book
    @PostMapping("borrow/return/{book-id}")
    public ResponseEntity<Integer> returnBorrowedBook(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.returnBorrowedBook(bookId, connectedUser));
    }

    // approve returned borrowed book
    @PostMapping("borrow/return/approve/{book-id}")
    public ResponseEntity<Integer> ApproveReturnedBorrowedBook(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(bookService.ApproveReturnedBorrowedBook(bookId, connectedUser));
    }

    @PostMapping(value = "/cover/{book-id}", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadBookCoverPicture(
            @Parameter()
            @RequestPart("file") MultipartFile file,
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser
    ) {
        bookService.uploadBookCoverPicture(file, connectedUser, bookId);
        return ResponseEntity.accepted().build();
    }

}
