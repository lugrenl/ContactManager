package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Contact;
import org.example.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class ContactService {

    private final ContactReader contactReader;
    private final ContactRepository contactRepository;

    @Autowired
    public ContactService(ContactRepository contactRepository, ContactReader contactReader) {
        this.contactRepository = contactRepository;
        this.contactReader = contactReader;
    }

    public void saveContacts(Path filePath) {
        log.info("Сохраняем список контактов. Путь к файлу = {}", filePath);
        var contacts = contactReader.readFromFile(filePath);
        log.info("Найдено пользователей = {}", contacts.size());
        contactRepository.saveAll(contacts);
        log.info("Все контакты успешно сохранены");
    }

    public List<Contact> getContacts() {
        log.info("Получаем список контактов");
        List<Contact> contacts = contactRepository.findAll();
        log.info("Найдено контактов = {}", contacts.size());
        return contacts;
    }
}