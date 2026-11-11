package com.kafkademo.email.api;

import com.kafkademo.email.config.ApiRoles;
import com.kafkademo.email.dto.request.IncomingEventRequest;
import com.kafkademo.email.dto.response.IncomingEventResponse;
import com.kafkademo.email.service.IncomingEventService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize(ApiRoles.ADMIN)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/emails/incoming-events")
public class IncomingEventController {

    private final IncomingEventService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomingEventResponse create(@Valid @RequestBody IncomingEventRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<IncomingEventResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{eventId}")
    public IncomingEventResponse get(@PathVariable UUID eventId) {
        return service.get(eventId);
    }

    @PutMapping("/{eventId}")
    public IncomingEventResponse update(@PathVariable UUID eventId, @Valid @RequestBody IncomingEventRequest request) {
        return service.update(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID eventId) {
        service.delete(eventId);
    }
}
