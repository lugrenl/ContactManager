package org.example.controller;

import org.example.model.Contact;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactDto {
    @JsonProperty("contactId")
    private final long contactId;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("surname")
    private final String surname;
    @JsonProperty("email")
    private final String email;
    @JsonProperty("phoneNumber")
    private final String phoneNumber;

    public ContactDto(Contact contact) {
        this.contactId = contact.getId();
        this.name = contact.getName();
        this.surname = contact.getSurname();
        this.email = contact.getEmail();
        this.phoneNumber = contact.getPhoneNumber();
    }

    public long getContactId() {
        return contactId;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
