package com.kafkademo.email.repository;

import com.kafkademo.email.entity.EmailMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmailSearchRepository {

    /**
     * Newest first. {@code lastName} matches the start of the name and {@code state} the whole value, both ignoring
     * case.
     */
    Page<EmailMessage> search(String lastName, String state, Pageable pageable);
}
