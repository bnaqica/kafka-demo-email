package com.kafkademo.email.processors;

import com.kafkademo.email.dto.EmailContent;
import com.kafkademo.email.dto.event.ChangedAddress;
import com.kafkademo.email.dto.event.ChangedPhone;
import com.kafkademo.email.dto.event.CustomerChangeEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The wording of each email. Empty for an event type that doesn't send one. */
@Component
public class CustomerEmailComposer {

    public Optional<EmailContent> compose(CustomerChangeEvent event) {
        String greeting = "Hi " + event.firstName() + ",\n\n";
        return Optional.ofNullable(
                switch (event.eventType()) {
                    case "customer-addition" ->
                        new EmailContent(
                                "Welcome to Kafka Demo, " + event.firstName() + "!",
                                "Hi " + event.firstName() + " " + event.lastName() + ",\n\nThanks for signing up.");
                    case "customer-change" ->
                        new EmailContent(
                                "Your information has changed",
                                greeting + "These details were updated: "
                                        + String.join(", ", event.changes().fields()) + ".");
                    case "customer-address-change" ->
                        new EmailContent(
                                "Your address has changed",
                                greeting + "Your address is now:\n"
                                        + format(event.changes().address()));
                    case "customer-phone-add" ->
                        new EmailContent(
                                "A phone was added to your account",
                                greeting + "We added this phone: "
                                        + format(event.changes().phone()) + ".");
                    case "customer-phone-change" ->
                        new EmailContent(
                                "Your phone number has changed",
                                greeting + "We changed "
                                        + format(event.changes().previousPhone()) + " to "
                                        + format(event.changes().phone()) + ".");
                    case "customer-phone-delete" ->
                        new EmailContent(
                                "A phone was removed from your account",
                                greeting + "We removed this phone: "
                                        + format(event.changes().phone()) + ".");
                    case "customer-delete" ->
                        new EmailContent(
                                "You have been removed from Kafka Demo",
                                greeting + "Your details have been removed from our system.");
                    default -> null;
                });
    }

    private static String format(ChangedPhone phone) {
        return phone.type() + " " + phone.number();
    }

    private static String format(ChangedAddress address) {
        String apartment = address.apartment() == null ? "" : ", " + address.apartment();
        return address.street() + apartment + "\n" + address.city() + ", " + address.state() + " " + address.zip();
    }
}
