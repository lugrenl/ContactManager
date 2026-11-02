package org.example.exceptions;

import org.springframework.security.access.AccessDeniedException;

public class ContactAccessDeniedException extends AccessDeniedException {

    public ContactAccessDeniedException(String message) {
        super(message);
    }
}
