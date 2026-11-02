package org.example.dao;

import lombok.RequiredArgsConstructor;
import org.example.entity.RevokedToken;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RevokedTokenDao {

    private final SessionFactory sessionFactory;

    public boolean existsByTokenHash(String tokenHash) {
        var session = sessionFactory.getCurrentSession();
        return session.createQuery(
                        "SELECT COUNT(r) FROM RevokedToken r WHERE r.tokenHash = :tokenHash", Long.class)
                .setParameter("tokenHash", tokenHash)
                .uniqueResult() > 0;
    }

    public void save(RevokedToken revokedToken) {
        var session = sessionFactory.getCurrentSession();
        session.save(revokedToken);
        session.flush();
    }

    public int deleteExpired(Instant now) {
        var session = sessionFactory.getCurrentSession();
        return session.createMutationQuery("DELETE FROM RevokedToken r WHERE r.expiresAt < :now")
                .setParameter("now", now)
                .executeUpdate();
    }
}
