package com.gichungasoftwares.book_network.book;

import com.gichungasoftwares.book_network.file.FileStorageService;
import com.gichungasoftwares.book_network.exception.OperationNotPermittedException;
import com.gichungasoftwares.book_network.history.BookTransactionHistory;
import com.gichungasoftwares.book_network.history.BookTransactionHistoryRepository;
import com.gichungasoftwares.book_network.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest request, Authentication connectedUser) {
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // transform book request to book object
        Book book = bookMapper.toBook(request);
        book.setOwner(user);
        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID:: " + bookId));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // create pageable object
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getName());
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // create pageable object
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(BookSpecification.withOwnerId(user.getName()), pageable);
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // create pageable object
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllBorrowedBooks(pageable, user.getName());
        List<BorrowedBookResponse> bookResponses = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // create pageable object
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allReturnedBooks = transactionHistoryRepository.findAllReturnedBooks(pageable, user.getName());
        List<BorrowedBookResponse> bookResponses = allReturnedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponses,
                allReturnedBooks.getNumber(),
                allReturnedBooks.getSize(),
                allReturnedBooks.getTotalElements(),
                allReturnedBooks.getTotalPages(),
                allReturnedBooks.isFirst(),
                allReturnedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // get the book with this id
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID:: " + bookId));
        // check if user owns this book
        if (!Objects.equals(book.getOwner().getUser_id(), user.getUser_id())) {
            throw new OperationNotPermittedException("You cannot update others books shareable status");
        }
        book.setShareable(!book.isShareable()); // inverse the value
        bookRepository.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // get the book with this id
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID:: " + bookId));
        // check if user owns this book
        if (!Objects.equals(book.getOwner().getUser_id(), user.getUser_id())) {
            throw new OperationNotPermittedException("You cannot update others books archived status");
        }
        book.setArchived(!book.isArchived()); // inverse the value
        bookRepository.save(book);
        return bookId;
    }


    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        // get the book with this id
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID:: " + bookId));
        // check if book is shareable or archived
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");
        }
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // check if user owns this book
        if (Objects.equals(book.getOwner().getUser_id(), user.getUser_id())) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }
        // check if the book is already borrowed
        final boolean isAlreadyBorrowed = transactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getName());
        if (isAlreadyBorrowed) {
            throw new OperationNotPermittedException("You have already borrowed this book");
        }
        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .book(book)
                .user(user)
                .isReturned(false)
                .isReturnApproved(false)
                .build();
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }


    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        // get the book with this id
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID:: " + bookId));
        // check if book is shareable or archived
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book cannot be returned since it is archived or not shareable");
        }
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // check if user owns this book
        if (Objects.equals(book.getOwner().getUser_id(), user.getUser_id())) {
            throw new OperationNotPermittedException("You cannot return your own book");
        }
        // check if user has indeed borrowed this book
        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndUserId(book.getId(), user.getName())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));
        bookTransactionHistory.setReturned(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer ApproveReturnedBorrowedBook(Integer bookId, Authentication connectedUser) {
        // get the book with this id
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID:: " + bookId));
        // check if book is shareable or archived
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book cannot be borrowed or returned since it is archived or not shareable");
        }
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // check if user owns this book
        if (Objects.equals(book.getOwner().getUser_id(), user.getUser_id())) {
            throw new OperationNotPermittedException("You cannot approve your own book");
        }
        // check if user has indeed borrowed this book
        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndOwnerId(book.getId(), user.getName())
                .orElseThrow(() -> new OperationNotPermittedException("The book is not returned yet so you cannot approve its return"));
        bookTransactionHistory.setReturnApproved(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        // get the book with this id
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID:: " + bookId));
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        var bookCover = fileStorageService.saveFile(file, user.getUser_id());
        book.setBookCover(bookCover);
        bookRepository.save(book);
    }
}
