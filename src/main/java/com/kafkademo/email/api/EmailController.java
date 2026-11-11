package com.kafkademo.email.api;

import com.kafkademo.email.config.ApiRoles;
import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.response.EmailResponse;
import com.kafkademo.email.dto.response.PageResponse;
import com.kafkademo.email.service.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize(ApiRoles.ADMIN)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<EmailResponse> create(@Valid @RequestBody EmailRequest request) {
        EmailResponse created = emailService.create(request);
        return ResponseEntity.created(URI.create("/api/emails/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<EmailResponse> list(
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return emailService.search(lastName, state, page, size);
    }

    @GetMapping("/{id}")
    public EmailResponse get(@PathVariable UUID id) {
        return emailService.get(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<EmailResponse> byCustomer(@PathVariable UUID customerId) {
        return emailService.findByCustomer(customerId);
    }

    @PutMapping("/{id}")
    public EmailResponse update(@PathVariable UUID id, @Valid @RequestBody EmailRequest request) {
        return emailService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        emailService.delete(id);
    }
}
