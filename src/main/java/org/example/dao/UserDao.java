package org.example.dao;

import lombok.extern.slf4j.Slf4j;
import org.example.exceptions.UserNotFoundException;
import org.example.model.User;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class UserDao {
    private final SessionFactory sessionFactory;

    @Autowired
    public UserDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        try (var session = sessionFactory.openSession()) {
            return session.createQuery("from User", User.class).getResultList();
        }
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        try (var session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }
            return user;
        } catch (HibernateException e) {
            log.error("Database error while fetching user ID: {}", userId, e);
            throw new DataAccessResourceFailureException("Failed to retrieve user with ID: " + userId, e);
        }
    }

    @Transactional
    public long addUser(User user) {
        try (var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            long userId = (Long) session.save(user);
            transaction.commit();
            return userId;
        }
    }

    @Transactional
    public User updateUser(long userId, User user) {
        try (var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            User userToUpdate = session.get(User.class, userId);
            if (userToUpdate != null) {
                userToUpdate.setName(user.getName());
                userToUpdate.setPassword(user.getPassword());
                userToUpdate.setEmail(user.getEmail());
                userToUpdate.setRole(user.getRole());
            } else {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }
            transaction.commit();
            return userToUpdate;
        }
    }

    @Transactional
    public void deleteUser(long userId) {
        try (var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            User user = session.get(User.class, userId);
            if (user != null) {
                session.remove(user);
            } else {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }
            transaction.commit();
        }
    }
}
