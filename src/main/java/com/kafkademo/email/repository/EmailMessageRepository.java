package com.kafkademo.email.repository;

import com.kafkademo.email.entity.EmailMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailMessageRepository extends MongoRepository<EmailMessage, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);

    List<EmailMessage> findAllByOrderBySentAtDesc();

    List<EmailMessage> findByCustomerIdOrderBySentAtDesc(UUID customerId);
}
