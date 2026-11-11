package com.kafkademo.email.processors;

import static org.assertj.core.api.Assertions.assertThat;

import com.kafkademo.email.EventFixtures;
import com.kafkademo.email.dto.EmailContent;
import com.kafkademo.email.dto.event.ChangedAddress;
import com.kafkademo.email.dto.event.ChangedPhone;
import com.kafkademo.email.dto.event.CustomerChanges;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerEmailComposerTest {

    private static final CustomerChanges NOTHING = new CustomerChanges(null, null, null, null);
    private static final ChangedPhone CELL = new ChangedPhone("CELL", "555-0100");

    private final CustomerEmailComposer composer = new CustomerEmailComposer();

    @Test
    void additionIsAWelcome() {
        EmailContent email = compose("customer-addition", NOTHING);

        assertThat(email.subject()).isEqualTo("Welcome to Kafka Demo, Ada!");
        assertThat(email.body()).contains("Ada Lovelace");
    }

    @Test
    void changeListsWhatChanged() {
        EmailContent email =
                compose("customer-change", new CustomerChanges(List.of("lastName", "address"), null, null, null));

        assertThat(email.subject()).isEqualTo("Your information has changed");
        assertThat(email.body()).contains("lastName, address");
    }

    @Test
    void addressChangeShowsTheNewAddress() {
        ChangedAddress address = new ChangedAddress("9 Elm St", "Unit 2", "Reno", "NV", "89501");

        EmailContent email = compose("customer-address-change", new CustomerChanges(null, address, null, null));

        assertThat(email.subject()).isEqualTo("Your address has changed");
        assertThat(email.body())
                .contains("9 Elm St, Unit 2")
                .doesNotContain("Apt")
                .contains("Reno, NV 89501");
    }

    @Test
    void addressChangeLeavesOutAMissingApartment() {
        ChangedAddress address = new ChangedAddress("9 Elm St", null, "Reno", "NV", "89501");

        assertThat(compose("customer-address-change", new CustomerChanges(null, address, null, null))
                        .body())
                .contains("9 Elm St\n")
                .doesNotContain("null");
    }

    @Test
    void phoneAddNamesThePhone() {
        EmailContent email = compose("customer-phone-add", new CustomerChanges(null, null, CELL, null));

        assertThat(email.subject()).isEqualTo("A phone was added to your account");
        assertThat(email.body()).contains("CELL").contains("555-0100");
    }

    @Test
    void phoneChangeShowsOldAndNew() {
        EmailContent email = compose(
                "customer-phone-change", new CustomerChanges(null, null, new ChangedPhone("CELL", "555-0111"), CELL));

        assertThat(email.subject()).isEqualTo("Your phone number has changed");
        assertThat(email.body()).contains("555-0100").contains("555-0111");
    }

    @Test
    void phoneDeleteNamesThePhone() {
        EmailContent email = compose("customer-phone-delete", new CustomerChanges(null, null, CELL, null));

        assertThat(email.subject()).isEqualTo("A phone was removed from your account");
        assertThat(email.body()).contains("555-0100");
    }

    @Test
    void deleteIsARemovalNotice() {
        EmailContent email = compose("customer-delete", NOTHING);

        assertThat(email.subject()).isEqualTo("You have been removed from Kafka Demo");
        assertThat(email.body()).contains("Ada");
    }

    @Test
    void unknownTypeHasNoEmail() {
        assertThat(composer.compose(EventFixtures.event("something-else", NOTHING)))
                .isEmpty();
    }

    private EmailContent compose(String type, CustomerChanges changes) {
        return composer.compose(EventFixtures.event(type, changes)).orElseThrow();
    }
}
