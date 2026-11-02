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
        var session = sessionFactory.getCurrentSession();
        return (Long) session.save(contact);
    }

    @Override
    public Contact getContact(long contactId) {
        try {
            Session session = sessionFactory.getCurrentSession();
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
        var session = sessionFactory.getCurrentSession();
        var contactToUpdate = session.get(Contact.class, contactId);
        if (contactToUpdate == null) {
            throw new ContactNotFoundException("Contact not found with ID: " + contactId);
        }
        contactToUpdate.setName(contact.getName());
        contactToUpdate.setSurname(contact.getSurname());
        contactToUpdate.setEmail(contact.getEmail());
        contactToUpdate.setPhoneNumber(contact.getPhoneNumber());
        return contactToUpdate;
    }

    @Override
    public List<Contact> getAllContacts() {
        var session = sessionFactory.getCurrentSession();
        return session.createQuery("select c from Contact c join fetch c.owner", Contact.class)
                .getResultList();
    }

    @Override
    public void deleteContact(long contactId) {
        var session = sessionFactory.getCurrentSession();
        var contact = session.get(Contact.class, contactId);
        if (contact == null) {
            throw new ContactNotFoundException("Contact not found with ID: " + contactId);
        }
        session.remove(contact);
    }

    @Override
    public Optional<Contact> findExistingContact(long ownerId, String name, String surname, String email,
                                                 String phoneNumber) {
        var session = sessionFactory.getCurrentSession();
        StringBuilder hql = new StringBuilder(
                "FROM Contact c WHERE c.owner.id = :ownerId AND c.name = :name AND c.surname = :surname");
        if (email == null) {
            hql.append(" AND c.email IS NULL");
        } else {
            hql.append(" AND c.email = :email");
        }
        if (phoneNumber == null) {
            hql.append(" AND c.phoneNumber IS NULL");
        } else {
            hql.append(" AND c.phoneNumber = :phoneNumber");
        }

        var query = session.createQuery(hql.toString(), Contact.class)
                .setParameter("ownerId", ownerId)
                .setParameter("name", name)
                .setParameter("surname", surname);
        if (email != null) {
            query.setParameter("email", email);
        }
        if (phoneNumber != null) {
            query.setParameter("phoneNumber", phoneNumber);
        }
        return query.uniqueResultOptional();
    }
}