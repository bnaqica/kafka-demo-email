package com.kafkademo.email.repository;

import com.kafkademo.email.entity.EmailMessage;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/** Spring Data picks this up by name as the implementation of {@link EmailSearchRepository}. */
@RequiredArgsConstructor
public class EmailSearchRepositoryImpl implements EmailSearchRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("sentAt"), Sort.Order.desc("_id"));

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<EmailMessage> search(String lastName, String state, Pageable pageable) {
        Query filter = new Query();
        if (hasText(lastName)) {
            filter.addCriteria(Criteria.where("lastName").regex("^" + Pattern.quote(lastName.trim()), "i"));
        }
        if (hasText(state)) {
            filter.addCriteria(Criteria.where("state").regex("^" + Pattern.quote(state.trim()) + "$", "i"));
        }
        long total = mongoTemplate.count(filter, EmailMessage.class);
        List<EmailMessage> content =
                mongoTemplate.find(Query.of(filter).with(NEWEST_FIRST).with(pageable), EmailMessage.class);
        return new PageImpl<>(content, pageable, total);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
