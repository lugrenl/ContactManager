package org.example.util;

import org.example.entity.Contact;
import org.example.exceptions.InvalidCsvException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactReaderTest {

    private final ContactReader contactReader = new ContactReader();

    @Test
    void readsTrailingEmptyFieldAsEmptyValue() {
        List<Contact> contacts = read("John,Doe,john@example.com,");

        assertThat(contacts).hasSize(1);
        Contact contact = contacts.get(0);
        assertThat(contact.getName()).isEqualTo("John");
        assertThat(contact.getSurname()).isEqualTo("Doe");
        assertThat(contact.getEmail()).isEqualTo("john@example.com");
        assertThat(contact.getPhoneNumber()).isNull();
    }

    @Test
    void readsCommaInsideQuotedField() {
        List<Contact> contacts = read("\"John, Jr.\",Doe,john@example.com,1234567890");

        Contact contact = contacts.get(0);
        assertThat(contact.getName()).isEqualTo("John, Jr.");
        assertThat(contact.getSurname()).isEqualTo("Doe");
        assertThat(contact.getEmail()).isEqualTo("john@example.com");
        assertThat(contact.getPhoneNumber()).isEqualTo("1234567890");
    }

    @Test
    void readsEscapedQuotesInsideQuotedField() {
        List<Contact> contacts = read("\"John \"\"JT\"\" Doe\",Doe,john@example.com,1234567890");

        assertThat(contacts.get(0).getName()).isEqualTo("John \"JT\" Doe");
    }

    @Test
    void skipsUtf8BomAtStartOfStream() {
        List<Contact> contacts = read("\uFEFFJohn,Doe,john@example.com,");

        Contact contact = contacts.get(0);
        assertThat(contact.getName()).isEqualTo("John");
        assertThat(contact.getSurname()).isEqualTo("Doe");
        assertThat(contact.getEmail()).isEqualTo("john@example.com");
        assertThat(contact.getPhoneNumber()).isNull();
    }

    @Test
    void skipsUtf8BomBeforeQuotedFirstField() {
        List<Contact> contacts = read("\uFEFF\"John, Jr.\",Doe,john@example.com,1234567890");

        assertThat(contacts.get(0).getName()).isEqualTo("John, Jr.");
    }

    @Test
    void keepsUtf8BomInsideField() {
        List<Contact> contacts = read("John,Doe,\uFEFFjohn@example.com,1234567890");

        assertThat(contacts.get(0).getEmail()).isEqualTo("\uFEFFjohn@example.com");
    }

    @Test
    void rejectsBomOnlyInput() {
        assertThatThrownBy(() -> contactReader.readFromStream(stream("\uFEFF")))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void readsEmptyOptionalFields() {
        List<Contact> contacts = read("John,Doe,,");

        Contact contact = contacts.get(0);
        assertThat(contact.getEmail()).isNull();
        assertThat(contact.getPhoneNumber()).isNull();
    }

    @Test
    void rejectsEmptyInput() {
        assertThatThrownBy(() -> contactReader.readFromStream(stream("")))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsRecordWithWrongNumberOfColumns() {
        assertThatThrownBy(() -> contactReader.readFromStream(stream("John,Doe,john@example.com")))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("expected 4 columns");
    }

    @Test
    void rejectsRecordWithTooManyColumns() {
        assertThatThrownBy(() -> contactReader.readFromStream(
                        stream("John,Doe,john@example.com,1234567890,extra")))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("expected 4 columns");
    }

    @Test
    void rejectsUnbalancedQuotes() {
        assertThatThrownBy(() -> contactReader.readFromStream(
                        stream("John,\"Doe,john@example.com,1234567890")))
                .isInstanceOf(InvalidCsvException.class);
    }

    @Test
    void rejectsRecordWithoutRequiredName() {
        assertThatThrownBy(() -> contactReader.readFromStream(
                        stream(",Doe,john@example.com,1234567890")))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("name and surname");
    }

    @Test
    void keepsColumnsAlignedForMultipleRows() {
        String content = "John,Doe,john@example.com,1234567890\n"
                + "Jane,Smith,jane@example.com,\n"
                + "\"Ann, Marie\",Brown,ann@example.com,555";
        List<Contact> contacts = read(content);

        assertThat(contacts).hasSize(3);
        assertThat(contacts.get(0).getName()).isEqualTo("John");
        assertThat(contacts.get(0).getSurname()).isEqualTo("Doe");
        assertThat(contacts.get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(contacts.get(0).getPhoneNumber()).isEqualTo("1234567890");
        assertThat(contacts.get(1).getName()).isEqualTo("Jane");
        assertThat(contacts.get(1).getSurname()).isEqualTo("Smith");
        assertThat(contacts.get(1).getEmail()).isEqualTo("jane@example.com");
        assertThat(contacts.get(1).getPhoneNumber()).isNull();
        assertThat(contacts.get(2).getName()).isEqualTo("Ann, Marie");
        assertThat(contacts.get(2).getSurname()).isEqualTo("Brown");
        assertThat(contacts.get(2).getEmail()).isEqualTo("ann@example.com");
        assertThat(contacts.get(2).getPhoneNumber()).isEqualTo("555");
    }

    private List<Contact> read(String content) {
        return contactReader.readFromStream(stream(content));
    }

    private ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
