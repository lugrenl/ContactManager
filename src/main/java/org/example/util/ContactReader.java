package org.example.util;

import org.apache.commons.csv.CSVException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.entity.Contact;
import org.example.exceptions.InvalidCsvException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContactReader {
    static final int EXPECTED_COLUMNS = 4;

    public List<Contact> readFromStream(InputStream inputStream) {
        try (Reader reader = new InputStreamReader(skipUtf8Bom(inputStream), StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, csvFormat())) {
            List<Contact> contacts = new ArrayList<>();
            for (CSVRecord record : parser) {
                contacts.add(parseContact(record));
            }
            if (contacts.isEmpty()) {
                throw new InvalidCsvException("CSV file is empty");
            }
            return contacts;
        } catch (IOException e) {
            throw new InvalidCsvException("Unable to read CSV file", e);
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof CSVException) {
                throw new InvalidCsvException("Invalid CSV format: " + e.getCause().getMessage(), e);
            }
            throw e;
        }
    }

    private static InputStream skipUtf8Bom(InputStream inputStream) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(inputStream, 3);
        byte[] prefix = new byte[3];
        int count = 0;
        while (count < prefix.length) {
            int read = pushback.read(prefix, count, prefix.length - count);
            if (read == -1) {
                break;
            }
            count += read;
        }

        boolean hasBom = count == prefix.length
                && prefix[0] == (byte) 0xEF
                && prefix[1] == (byte) 0xBB
                && prefix[2] == (byte) 0xBF;
        if (count > 0 && !hasBom) {
            pushback.unread(prefix, 0, count);
        }
        return pushback;
    }

    private static CSVFormat csvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .build();
    }

    private Contact parseContact(CSVRecord record) {
        if (record.size() != EXPECTED_COLUMNS) {
            throw new InvalidCsvException("Invalid CSV record " + record.getRecordNumber()
                    + ": expected " + EXPECTED_COLUMNS + " columns, but got " + record.size());
        }
        String name = record.get(0);
        String surname = record.get(1);
        if (name.isBlank() || surname.isBlank()) {
            throw new InvalidCsvException("Invalid CSV record " + record.getRecordNumber()
                    + ": name and surname must not be empty");
        }
        return new Contact(name, surname, blankToNull(record.get(2)), blankToNull(record.get(3)));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
