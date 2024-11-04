package com.gichungasoftwares.book_network.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Integer>, JpaSpecificationExecutor<Book> {

    @Query("""
           SELECT book
           FROM Book book
           WHERE book.isArchived = false
           AND book.isShareable = true
           AND book.createdBy != :userId
           
    """)
    Page<Book> findAllDisplayableBooks(Pageable pageable, Integer userId);
}
