package com.gichungasoftwares.book_network.feedback;

import com.gichungasoftwares.book_network.book.Book;
import com.gichungasoftwares.book_network.book.BookRepository;
import com.gichungasoftwares.book_network.common.PageResponse;
import com.gichungasoftwares.book_network.exception.OperationNotPermittedException;
import com.gichungasoftwares.book_network.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final BookRepository bookRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackRepository feedbackRepository;

    public Integer save(FeedbackRequest feedbackRequest, Authentication connectedUser) {
        Book book = bookRepository.findById(feedbackRequest.bookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found with ID:: " + feedbackRequest.bookId()));
        // check if book is shareable or archived
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("You cannot give feedback for an archived or not shareable book");
        }
        // get the user from the authentication
        User user = ((User) connectedUser.getPrincipal());
        // check if user owns this book
        if (Objects.equals(book.getOwner().getUser_id(), user.getUser_id())) {
            throw new OperationNotPermittedException("You cannot give feedback to your own book");
        }
        Feedback feedback = feedbackMapper.toFeedback(feedbackRequest);
        return feedbackRepository.save(feedback).getId();
    }

    public PageResponse<FeedbackResponse> findAllFeedbackByBook(Integer bookId, int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        User user = ((User) connectedUser.getPrincipal());
        Page<Feedback> feedbacks = feedbackRepository.findAllByBookId(bookId, pageable);
        List<FeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> feedbackMapper.toFeedbackResponse(f, user.getUser_id()))
                .toList();
        return new PageResponse<>(
                feedbackResponses,
                feedbacks.getNumber(),
                feedbacks.getNumber(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }
}
