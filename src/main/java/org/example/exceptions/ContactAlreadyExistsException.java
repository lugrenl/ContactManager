package org.example.exceptions;

public class ContactAlreadyExistsException extends RuntimeException {

    public ContactAlreadyExistsException(String message) {
        super(message);
    }
}
