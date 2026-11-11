package com.kafkademo.email.dto.response;

import java.util.List;

/** One page of results, in a JSON shape this API owns. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
