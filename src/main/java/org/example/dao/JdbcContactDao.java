package org.example.dao;

import org.example.exceptions.ContactNotFoundException;
import org.example.model.Contact;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class JdbcContactDao implements ContactDao {

    private static final String GET_ALL_CONTACTS = "SELECT ID, NAME, SURNAME, EMAIL, PHONE_NUMBER FROM CONTACT";
    private static final String GET_CONTACT_BY_ID = GET_ALL_CONTACTS + " WHERE ID = :id";
    private static final String SAVE_CONTACT = "INSERT INTO CONTACT (NAME, SURNAME, EMAIL, PHONE_NUMBER) VALUES (:name, :surname, :email, :phoneNumber)";
    private static final String UPDATE_CONTACT = "UPDATE CONTACT SET NAME = :name, SURNAME = :surname, EMAIL = :email, PHONE_NUMBER = :phoneNumber WHERE ID = :id";
    private static final String UPDATE_EMAIL = "UPDATE CONTACT SET EMAIL = :email WHERE ID = :id";
    private static final String UPDATE_PHONE_NUMBER = "UPDATE CONTACT SET PHONE_NUMBER = :phoneNumber WHERE ID = :id";
    private static final String DELETE_CONTACT = "DELETE FROM CONTACT WHERE ID = :id";

    private static final RowMapper<Contact> CONTACT_ROW_MAPPER =
            (rs, rowNum) -> new Contact(
                    rs.getLong("ID"),
                    rs.getString("NAME"),
                    rs.getString("SURNAME"),
                    rs.getString("EMAIL"),
                    rs.getString("PHONE_NUMBER"));

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public JdbcContactDao(NamedParameterJdbcTemplate namedJdbcTemplate) { this.namedJdbcTemplate = namedJdbcTemplate; }

    @Override
    public List<Contact> getAllContacts() {
        return namedJdbcTemplate.query(GET_ALL_CONTACTS, CONTACT_ROW_MAPPER);
    }

    @Override
    public Contact getContact(long contactId) {
        return namedJdbcTemplate.queryForObject(GET_CONTACT_BY_ID, new MapSqlParameterSource("id", contactId), CONTACT_ROW_MAPPER);
    }

    @Override
    public Contact updateContact(long contactId, String name, String surname, String email, String phoneNumber) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        var args = new MapSqlParameterSource()
                .addValue("id", contactId)
                .addValue("name", name)
                .addValue("surname", surname)
                .addValue("email", email)
                .addValue("phoneNumber", phoneNumber);

        namedJdbcTemplate.update(UPDATE_CONTACT, args, keyHolder, new String[] { "id" });

        if (keyHolder.getKey() == null) {
            throw new ContactNotFoundException("Contact not found: " + contactId);
        }
        return getContact(contactId);
    }

    @Override
    public long addContact(Contact contact) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
         var args = contactToArgs(contact);

        namedJdbcTemplate.update(SAVE_CONTACT, args, keyHolder, new String[] { "id" });
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    @Override
    public Contact addContactReturnContact(String name, String surname, String email, String phoneNumber) {
        Contact contact = new Contact(name, surname, email, phoneNumber);
        long contactId = addContact(contact);
        contact.setId(contactId);
        return contact;
    }

    @Override
    public void updateEmail(long contactId, String email) {
        namedJdbcTemplate.update(UPDATE_EMAIL, new MapSqlParameterSource("id", contactId).addValue("email", email));
    }

    @Override
    public void updatePhoneNumber(long contactId, String phoneNumber) {
        namedJdbcTemplate.update(UPDATE_PHONE_NUMBER, new MapSqlParameterSource("id", contactId).addValue("phoneNumber", phoneNumber));
    }

    @Override
    public void deleteContact(long contactId) {
        namedJdbcTemplate.update(DELETE_CONTACT, new MapSqlParameterSource("id", contactId));
    }

    @Override
    public void saveAll(Collection<Contact> contacts) {
        var args = contacts.stream().map(JdbcContactDao::contactToArgs).toArray(MapSqlParameterSource[]::new);
        namedJdbcTemplate.batchUpdate(SAVE_CONTACT, args);

    }

    private static MapSqlParameterSource contactToArgs(Contact contact) {
        return new MapSqlParameterSource()
                .addValue("name", contact.getName())
                .addValue("surname", contact.getSurname())
                .addValue("email", contact.getEmail())
                .addValue("phoneNumber", contact.getPhoneNumber());
    }
}