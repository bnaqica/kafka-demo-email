package com.kafkademo.email.service;

import com.kafkademo.email.dto.request.EmailRequest;

/**
 * Delivers an email. The demo implementation only logs; swap in SMTP/SES/etc. later.
 */
public interface EmailSender {

    void send(EmailRequest email);
}
