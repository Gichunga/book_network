package com.gichungasoftwares.book_network.history;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface BookTransactionHistoryRepository extends JpaRepository<BookTransactionHistory, Integer> {

    @Query("""
            SELECT CASE WHEN COUNT(bookTransactionHistory) > 0 THEN true ELSE false END
            FROM BookTransactionHistory bookTransactionHistory
            WHERE bookTransactionHistory.user.user_id = :userId
            AND bookTransactionHistory.book.id = :bookId
            AND bookTransactionHistory.isReturnApproved = false
            """)
    boolean isAlreadyBorrowedByUser(@Param("bookId") Integer bookId, @Param("userId") Integer userId);

    @Query("""
            SELECT CASE WHEN COUNT(bookTransactionHistory) > 0 THEN true ELSE false END
            FROM BookTransactionHistory bookTransactionHistory
            WHERE bookTransactionHistory.book.id = :bookId
            AND bookTransactionHistory.isReturnApproved = false
            """)
    boolean isAlreadyBorrowed(@Param("bookId") Integer bookId);

    @Query("""
           SELECT transaction
           FROM BookTransactionHistory transaction
           WHERE transaction.user.user_id = :userId
           AND transaction.book.id = :bookId
           AND transaction.isReturned = false
           AND transaction.isReturnApproved = false
           """)
    Optional<BookTransactionHistory> findByBookIdAndUserId(@Param("bookId") Integer bookId, @Param("userId") Integer userId);

    @Query("""
           SELECT transaction
           FROM BookTransactionHistory transaction
           WHERE transaction.book.createdBy = :userId
           AND transaction.book.id = :bookId
           AND transaction.isReturned = true
           AND transaction.isReturnApproved = false
           """)
    Optional<BookTransactionHistory> findByBookIdAndOwnerId(@Param("bookId") Integer bookId, @Param("userId") Integer userId);

    @Query("""
           SELECT history
           FROM BookTransactionHistory history
           WHERE history.user.user_id = :userId
           """)
    Page<BookTransactionHistory> findAllBorrowedBooks(Pageable pageable, Integer userId);

    @Query("""
           SELECT history
           FROM BookTransactionHistory history
           WHERE history.book.createdBy = :userId
           AND history.isReturned = true
           """)
    Page<BookTransactionHistory> findAllReturnedBooks(Pageable pageable, Integer userId);

}
