package com.kafkademo.email.service;

import com.kafkademo.email.dto.request.IncomingEventRequest;
import com.kafkademo.email.dto.response.IncomingEventResponse;
import com.kafkademo.email.entity.IncomingEvent;
import com.kafkademo.email.entity.IncomingEventStatus;
import com.kafkademo.email.exception.DuplicateIncomingEventException;
import com.kafkademo.email.exception.IncomingEventNotFoundException;
import com.kafkademo.email.repository.IncomingEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IncomingEventServiceImpl implements IncomingEventService {

    private final IncomingEventRepository repository;
    private final Clock clock;

    @Override
    public IncomingEventResponse create(IncomingEventRequest request) {
        try {
            Instant now = Instant.now(clock);
            Instant processedAt = request.status() == IncomingEventStatus.PROCESSED ? now : null;
            return toResponse(repository.insert(toEntity(request.eventId(), request, now, processedAt)));
        } catch (DuplicateKeyException ex) {
            throw new DuplicateIncomingEventException(request.eventId());
        }
    }

    @Override
    public List<IncomingEventResponse> findAll() {
        return repository.findAll().stream()
                .map(IncomingEventServiceImpl::toResponse)
                .toList();
    }

    @Override
    public Optional<IncomingEventResponse> find(UUID eventId) {
        return repository.findById(eventId).map(IncomingEventServiceImpl::toResponse);
    }

    @Override
    public IncomingEventResponse get(UUID eventId) {
        return toResponse(load(eventId));
    }

    @Override
    public IncomingEventResponse update(UUID eventId, IncomingEventRequest request) {
        IncomingEvent existing = load(eventId);
        return toResponse(repository.save(
                toEntity(eventId, request, existing.receivedAt(), processedAt(existing, request.status()))));
    }

    @Override
    public void delete(UUID eventId) {
        repository.delete(load(eventId));
    }

    private Instant processedAt(IncomingEvent existing, IncomingEventStatus status) {
        if (status != IncomingEventStatus.PROCESSED) {
            return null;
        }
        return existing.processedAt() != null ? existing.processedAt() : Instant.now(clock);
    }

    private IncomingEvent load(UUID eventId) {
        return repository.findById(eventId).orElseThrow(() -> new IncomingEventNotFoundException(eventId));
    }

    private static IncomingEvent toEntity(
            UUID eventId, IncomingEventRequest request, Instant receivedAt, Instant processedAt) {
        return IncomingEvent.builder()
                .eventId(eventId)
                .eventType(request.eventType())
                .customerId(request.customerId())
                .payload(request.payload())
                .status(request.status())
                .receivedAt(receivedAt)
                .processedAt(processedAt)
                .build();
    }

    private static IncomingEventResponse toResponse(IncomingEvent event) {
        return IncomingEventResponse.builder()
                .eventId(event.eventId())
                .eventType(event.eventType())
                .customerId(event.customerId())
                .payload(event.payload())
                .status(event.status())
                .receivedAt(event.receivedAt())
                .processedAt(event.processedAt())
                .build();
    }
}
