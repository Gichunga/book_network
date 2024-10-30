package com.gichungasoftwares.book_network.history;

import com.gichungasoftwares.book_network.book.Book;
import com.gichungasoftwares.book_network.common.BaseEntity;
import com.gichungasoftwares.book_network.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BookTransactionHistory extends BaseEntity {

    // user relationship
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // book relationship
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;


    private boolean isReturned;
    private boolean isReturnApproved;
}
