package org.example.dao;

import lombok.extern.slf4j.Slf4j;
import org.example.exceptions.ContactNotFoundException;
import org.example.model.Contact;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class HibernateContactDao implements ContactDao {
    private final SessionFactory sessionFactory;

    @Autowired
    public HibernateContactDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional
    public long addContact(Contact contact) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            long contactId = (Long) session.save(contact);
            transaction.commit();
            return contactId;
        }
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional
    public Contact updateContact(long contactId, Contact contactToUpdate) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            var contact = session.get(Contact.class, contactId);
            if (contact != null) {
                contact.setName(contactToUpdate.getName());
                contact.setSurname(contactToUpdate.getSurname());
                contact.setEmail(contactToUpdate.getEmail());
                contact.setPhoneNumber(contactToUpdate.getPhoneNumber());
            } else {
                throw new ContactNotFoundException("Contact not found: " + contactId);
            }
            transaction.commit();
            return contact;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contact> getAllContacts() {
        try(var session = sessionFactory.openSession()) {
            return session.createQuery("from Contact", Contact.class).getResultList();
        }
    }

    @Override
    @Transactional
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
    @Transactional
    public void saveAll(Collection<Contact> contacts) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            contacts.forEach(session::save);
            transaction.commit();
        }
    }
}
