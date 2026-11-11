package com.kafkademo.email.service;

import com.kafkademo.email.dto.request.IncomingEventRequest;
import com.kafkademo.email.dto.response.IncomingEventResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CRUD for the incoming_event collection. */
public interface IncomingEventService {

    IncomingEventResponse create(IncomingEventRequest request);

    List<IncomingEventResponse> findAll();

    Optional<IncomingEventResponse> find(UUID eventId);

    IncomingEventResponse get(UUID eventId);

    IncomingEventResponse update(UUID eventId, IncomingEventRequest request);

    void delete(UUID eventId);
}
