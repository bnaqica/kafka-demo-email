package com.kafkademo.email.repository;

import com.kafkademo.email.entity.IncomingEvent;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IncomingEventRepository extends MongoRepository<IncomingEvent, UUID> {}
