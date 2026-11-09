package com.kafkademo.email.service;

import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.response.EmailResponse;
import java.util.List;
import java.util.UUID;

public interface EmailService {

    EmailResponse create(EmailRequest request);

    List<EmailResponse> findAll();

    EmailResponse get(UUID id);

    List<EmailResponse> findByCustomer(UUID customerId);

    boolean existsBySourceEventId(UUID sourceEventId);

    EmailResponse update(UUID id, EmailRequest request);

    void delete(UUID id);
}
