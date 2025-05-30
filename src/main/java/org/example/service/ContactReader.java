package org.example.service;

import org.example.model.Contact;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactReader {
    private static final String SEPARATOR = ",";

    public List<Contact> readFromFile(Path filePath) {
        try {
            return Files.readAllLines(filePath)
                    .stream()
                    .map(this::parseContact)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Contact parseContact(String csvLine) {
        String[] parts = csvLine.split(SEPARATOR);
        return new Contact(parts[0], parts[1], parts[2], parts[3]);
    }
}
