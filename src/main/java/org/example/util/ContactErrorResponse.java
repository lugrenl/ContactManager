package org.example.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ContactErrorResponse {
    private String message;
    private long timestamp;
}
