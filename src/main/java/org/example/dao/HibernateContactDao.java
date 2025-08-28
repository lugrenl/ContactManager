package org.example.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.exceptions.ContactNotFoundException;
import org.example.entity.Contact;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HibernateContactDao implements ContactDao {
    private final SessionFactory sessionFactory;

    @Override
    public long addContact(Contact contact) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            long contactId = (Long) session.save(contact);
            transaction.commit();
            return contactId;
        }
    }

    @Override
    public Contact getContact(long contactId) {
        try (Session session = sessionFactory.openSession()) {
            Contact contact = session.get(Contact.class, contactId);
            if (contact == null) {
                throw new ContactNotFoundException("Contact not found with ID: " + contactId);
            }
            return contact;
        } catch (HibernateException e) {
            log.error("Database error while fetching contact ID: {}", contactId, e);
            throw new DataAccessResourceFailureException("Failed to retrieve contact with ID: " + contactId, e);
        }
    }

    @Override
    public Contact updateContact(long contactId, Contact contact) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            var contactToUpdate = session.get(Contact.class, contactId);
            if (contactToUpdate != null) {
                contactToUpdate.setName(contact.getName());
                contactToUpdate.setSurname(contact.getSurname());
                contactToUpdate.setEmail(contact.getEmail());
                contactToUpdate.setPhoneNumber(contact.getPhoneNumber());
            } else {
                throw new ContactNotFoundException("Contact not found with ID: " + contactId);
            }
            transaction.commit();
            return contactToUpdate;
        }
    }

    @Override
    public List<Contact> getAllContacts() {
        try(var session = sessionFactory.openSession()) {
            return session.createQuery("from Contact", Contact.class).getResultList();
        }
    }

    @Override
    public void deleteContact(long contactId) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            var contact = session.get(Contact.class, contactId);
            if (contact != null) {
                session.remove(contact);
            } else {
                throw new ContactNotFoundException("Contact not found with ID: " + contactId);
            }
            transaction.commit();
        }
    }

    @Override
    public Optional<Contact> findExistingContact(String name, String surname, String email, String phoneNumber) {
        try (var session = sessionFactory.openSession()) {
            String hql = "FROM Contact c WHERE c.name = :name " +
                    "AND c.surname = :surname " +
                    "AND c.email = :email " +
                    "AND c.phoneNumber = :phoneNumber";

            return session.createQuery(hql, Contact.class)
                    .setParameter("name", name)
                    .setParameter("surname", surname)
                    .setParameter("email", email)
                    .setParameter("phoneNumber", phoneNumber)
                    .uniqueResultOptional();
        }
    }
}
