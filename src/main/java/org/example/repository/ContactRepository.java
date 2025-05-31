package org.example.repository;

import org.example.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Transactional
    @Modifying
    @Query("update Contact c set c.phoneNumber = :phone where c.id = :contactId")
    void updatePhone(@Param("contactId") long contactId, @Param("phone") String phoneNumber);

    @Transactional
    @Modifying
    @Query("update Contact c set c.email = :email where c.id = :contactId")
    void updateEmail(@Param("contactId") long contactId, @Param("email") String email);
}
