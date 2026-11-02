package org.example.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.exceptions.UserAlreadyExistsException;
import org.example.exceptions.UserNotFoundException;
import org.example.entity.User;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDao {
    private final SessionFactory sessionFactory;

    public List<User> getAllUsers() {
        var session = sessionFactory.getCurrentSession();
        return session.createQuery("from User", User.class).getResultList();
    }

    public User getUser(Long userId) {
        try {
            var session = sessionFactory.getCurrentSession();
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

    public void addUser(User user) {
        try {
            var session = sessionFactory.getCurrentSession();
            session.save(user);
            session.flush();
        } catch (ConstraintViolationException e) {
            throw new UserAlreadyExistsException("User with this username already exists");
        }
    }

    public User updateUser(long userId, User user) {
        try {
            var session = sessionFactory.getCurrentSession();
            User userToUpdate = session.get(User.class, userId);

            if (userToUpdate == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }

            userToUpdate.setName(user.getName());
            userToUpdate.setEmail(user.getEmail());
            if (user.getPassword() != null) {
                userToUpdate.setPassword(user.getPassword());
            }
            if (user.getRole() != null) {
                userToUpdate.setRole(user.getRole());
            }
            session.flush();
            return userToUpdate;
        } catch (ConstraintViolationException e) {
            throw new UserAlreadyExistsException("User with this username already exists");
        }
    }

    public void deleteUser(long userId) {
        var session = sessionFactory.getCurrentSession();
        User user = session.get(User.class, userId);
        if (user == null) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        session.remove(user);
    }

    public Optional<User> findByUsername(String username) {
        var session = sessionFactory.getCurrentSession();
        Query<User> query = session.createQuery("FROM User WHERE name = :username", User.class);
        query.setParameter("username", username);
        return query.uniqueResultOptional();
    }

    public Optional<User> findById(Long id) {
        var session = sessionFactory.getCurrentSession();
        return Optional.ofNullable(session.get(User.class, id));
    }

    public boolean existsById(Long id) {
        var session = sessionFactory.getCurrentSession();
        return session.get(User.class, id) != null;
    }
}