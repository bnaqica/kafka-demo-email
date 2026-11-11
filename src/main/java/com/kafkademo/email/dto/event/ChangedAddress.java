package com.kafkademo.email.dto.event;

public record ChangedAddress(String street, String apartment, String city, String state, String zip) {}
