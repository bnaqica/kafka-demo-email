package com.kafkademo.email.service;

import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.response.EmailResponse;
import com.kafkademo.email.dto.response.PageResponse;
import com.kafkademo.email.entity.EmailMessage;
import com.kafkademo.email.exception.EmailNotFoundException;
import com.kafkademo.email.repository.EmailMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final EmailMessageRepository repository;
    private final Clock clock;

    @Override
    public EmailResponse create(EmailRequest request) {
        UUID sourceEventId = request.sourceEventId() != null ? request.sourceEventId() : UUID.randomUUID();
        EmailMessage saved = repository.save(toEntity(UUID.randomUUID(), sourceEventId, request, Instant.now(clock)));
        log.info("Created email {}", saved.id());
        return toResponse(saved);
    }

    @Override
    public PageResponse<EmailResponse> search(String lastName, String state, int page, int size) {
        Page<EmailMessage> results = repository.search(lastName, state, PageRequest.of(page, size));
        return new PageResponse<>(
                results.getContent().stream().map(this::toResponse).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    @Override
    public EmailResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Override
    public List<EmailResponse> findByCustomer(UUID customerId) {
        return repository.findByCustomerIdOrderBySentAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public boolean existsBySourceEventId(UUID sourceEventId) {
        return repository.existsBySourceEventId(sourceEventId);
    }

    @Override
    public EmailResponse update(UUID id, EmailRequest request) {
        EmailMessage existing = find(id);
        EmailMessage saved = repository.save(toEntity(id, existing.sourceEventId(), request, existing.sentAt()));
        log.info("Updated email {}", id);
        return toResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        repository.delete(find(id));
        log.info("Deleted email {}", id);
    }

    private EmailMessage find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EmailNotFoundException(id));
    }

    private static EmailMessage toEntity(UUID id, UUID sourceEventId, EmailRequest request, Instant sentAt) {
        return EmailMessage.builder()
                .id(id)
                .sourceEventId(sourceEventId)
                .customerId(request.customerId())
                .from(request.from())
                .to(request.to())
                .subject(request.subject())
                .body(request.body())
                .status(request.status())
                .sentAt(sentAt)
                .lastName(request.lastName())
                .state(request.state())
                .eventType(request.eventType())
                .build();
    }

    private EmailResponse toResponse(EmailMessage email) {
        return EmailResponse.builder()
                .id(email.id())
                .sourceEventId(email.sourceEventId())
                .customerId(email.customerId())
                .from(email.from())
                .to(email.to())
                .subject(email.subject())
                .body(email.body())
                .status(email.status())
                .sentAt(email.sentAt())
                .lastName(email.lastName())
                .state(email.state())
                .eventType(email.eventType())
                .build();
    }
}
