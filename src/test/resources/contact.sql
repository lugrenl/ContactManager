DROP TABLE IF EXISTS CONTACT;

CREATE TABLE CONTACT (
                         ID serial primary key ,
                         NAME varchar(255),
                         SURNAME varchar(255),
                         EMAIL varchar (255),
                         PHONE_NUMBER varchar (255)
);

INSERT INTO CONTACT(ID, NAME, SURNAME, EMAIL, PHONE_NUMBER)
VALUES (1000, 'Ivan', 'Ivanov', 'iivanov@gmail.com', '1234567');

INSERT INTO CONTACT(ID, NAME, SURNAME, EMAIL, PHONE_NUMBER)
VALUES (2000, 'Maria', 'Ivanova', 'mivanova@gmail.com', '7654321');