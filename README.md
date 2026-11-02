# Contact Manager API

REST-бэкенд для управления личными контактами с регистрацией пользователей, JWT-аутентификацией, административным API и импортом контактов из CSV. Пользовательский веб-интерфейс в проект не входит; для ручной проверки API предназначен Swagger UI.

## Возможности

- регистрация пользователей и вход с выдачей JWT;
- logout с серверным отзывом конкретного JWT;
- CRUD личных контактов текущего пользователя;
- дедупликация контактов только внутри адресной книги владельца;
- импорт контактов из CSV-файла на сервере;
- административный просмотр всех контактов;
- административное управление пользователями и ролями.

## Технологии

- Java 17+ (проект проверялся на OpenJDK 21; Java toolchain в сборке не закреплён);
- Gradle Wrapper 8.13, плагин `java`;
- Spring Boot 3.5.x (Web, Security, Validation, Test);
- Hibernate ORM 6.6 и Spring ORM;
- PostgreSQL;
- Auth0 Java JWT;
- springdoc-openapi 2.8.9 (Swagger UI);
- Apache Commons CSV;
- Lombok, ModelMapper.

Доступ к данным реализован через собственный `SessionFactory`, DAO и `HibernateTransactionManager`. Spring Data-репозитории не используются.

## Требования

- JDK 17 или новее (рекомендуется 21);
- PostgreSQL 14+ (или совместимая версия);
- сетевой доступ к Maven Central при первой сборке.

## Сборка и проверки

Используется Gradle Wrapper из репозитория. Команды для Windows PowerShell:

```powershell
.\gradlew.bat classes
.\gradlew.bat compileJava --console=plain
.\gradlew.bat build --console=plain
```

На Unix/macOS замените `.\gradlew.bat` на `./gradlew`.

Безопасные модульные тесты (без подключения к PostgreSQL):

```powershell
.\gradlew.bat test --tests 'org.example.service.*Test' --tests 'org.example.dao.*Test' --tests 'org.example.util.*Test' --tests 'org.example.config.*Test'
```

Интеграционные тесты используют изолированную H2:

```powershell
.\gradlew.bat test --tests 'org.example.controller.*IT'
```

Полная сборка `build` включает все тесты. Отдельной задачи `bootRun` или исполняемого Spring Boot JAR в проекте нет: приложение запускается через главный класс `org.example.ContactManagerMain` из IDE или с runtime classpath.

## Запуск

1. Подготовьте PostgreSQL и схему базы данных (см. раздел «База данных и миграции»).
2. Задайте параметры подключения и обязательные секреты (см. «Конфигурация»).
3. Запустите `org.example.ContactManagerMain`.
4. Приложение слушает `http://localhost:8080`.

При старте Hibernate работает с `hibernate.hbm2ddl.auto=validate`: рабочая схема не создаётся и не пересоздаётся автоматически.

## Конфигурация

Параметры подключения к БД находятся в `src/main/resources/jdbc.properties`:

| Параметр | Назначение |
|---|---|
| `spring.datasource.url` | JDBC URL PostgreSQL |
| `spring.datasource.username` | Пользователь БД |
| `spring.datasource.password` | Пароль БД |
| `spring.datasource.driver-class-name` | `org.postgresql.Driver` |
| `spring.jpa.database-platform` | `org.hibernate.dialect.PostgreSQLDialect` |

Не используйте значения из репозитория как production-учётные данные: задайте собственные.

JWT и начальный администратор настраиваются в `application.properties` через переменные окружения или одноимённые свойства:

| Переменная окружения | Свойство | Обязательность | Назначение |
|---|---|---|---|
| `JWT_SECRET` | `jwt_secret` | Да для обычного запуска | Секрет подписи HMAC. Минимум 32 символа. Отсутствие, короткое значение или известное `SECRET` приводят к остановке с диагностикой. |
| — | `jwt_expiration` | Нет, по умолчанию 60 | Срок действия JWT **в минутах**. |
| `ADMIN_USERNAME` | `admin.username` | Только для создания начального администратора | Имя администратора. |
| `ADMIN_PASSWORD` | `admin.password` | Только для создания начального администратора | Пароль администратора; хешируется BCrypt перед сохранением. |
| `ADMIN_EMAIL` | `admin.email` | Нет | Email администратора, по умолчанию `admin@example.com`. |

Начальный администратор создаётся только при явно заданных `ADMIN_USERNAME` и `ADMIN_PASSWORD`. Если не задан ни один из них, приложение запускается без автоматического создания администратора. Если задан только один параметр, запуск завершается с ошибкой. Существующий администратор не изменяется.

Пример (значения замените собственными, секрет не коммитьте):

```powershell
$env:JWT_SECRET = "<случайная строка не короче 32 символов>"
$env:ADMIN_USERNAME = "<имя администратора>"
$env:ADMIN_PASSWORD = "<надёжный пароль>"
.\gradlew.bat build --console=plain
```

## База данных и миграции

- Приложение не создаёт и не пересоздаёт рабочую схему: используется `validate`.
- Схема должна соответствовать Hibernate mappings (`User`, `Contact`, `RevokedToken`). Контакты персональные: `User 1—N Contact` через `contacts.user_id`; таблицы `user_contacts` в текущей модели нет.
- Механизма автоматического применения миграций (Flyway/Liquibase) нет. SQL-файлы в `src/main/resources/db/migration` выполняются вручную владельцем БД.
- `Basic_schema.sql` — исторический файл начальной схемы; он не применяется автоматически, описывает старую схему с `user_contacts` и не должен использоваться как готовая production-миграция без проверки.
- `Add_unique_users_name.sql` добавляет уникальное ограничение на имя пользователя. Перед применением проверьте дубликаты:

```sql
SELECT name, COUNT(*) FROM users GROUP BY name HAVING COUNT(*) > 1;
```

- `Add_revoked_tokens.sql` создаёт таблицу `revoked_tokens`, используемую для серверного отзыва JWT. Её необходимо применить до запуска версии с отзывом токенов.
- `Diagnose_contacts_migration.sql` — read-only диагностика существующей many-to-many схемы: orphan-контакты, общие контакты, потенциальные дубликаты после расширения и фактические типы ID.
- `Migrate_contacts_to_personal_owner.sql` — ручная миграция many-to-many → one-to-many. Она сохраняет исходный ID за одним владельцем (минимальный `user_id`), создаёт отдельную копию с новым ID каждому остальному пользователю, добавляет `contacts.user_id NOT NULL`, FK и индексы, затем удаляет `user_contacts`. Миграция останавливается до разрушительных изменений при orphan-контактах, NULL в обязательных полях или дубликатах в адресной книге одного пользователя. Старая таблица сохраняется как `contacts_legacy_backup`; полный откат требует резервной копии БД.
- Порядок для существующей PostgreSQL: остановить старую версию, выполнить диагностику, сделать резервную копию, применить миграцию, затем запустить версию с новой моделью. Миграция не применяется при старте приложения.

## API

Базовый URL: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Публичные операции: регистрация и вход. Остальные операции требуют JWT в заголовке:

```
Authorization: Bearer <jwt-token>
```

### Аутентификация

| Метод | URL | Доступ | Успешный ответ |
|---|---|---|---|
| `POST` | `/api/auth/register` | Публичный | `200 {"jwt-token":"..."}` |
| `POST` | `/api/auth/login` | Публичный | `200 {"jwt-token":"..."}` |
| `POST` | `/api/auth/logout` | JWT | `200`, `text/plain`: `Logged out successfully` |

Регистрация:

```json
{
  "name": "johndoe",
  "password": "securePassword123",
  "email": "john.doe@example.com"
}
```

Вход:

```json
{
  "name": "johndoe",
  "password": "securePassword123"
}
```

После logout предъявленный токен отзывается и больше не даёт доступа; остальные токены пользователя продолжают работать.

### Контакты текущего пользователя

| Метод | URL | Доступ | Успешный ответ |
|---|---|---|---|
| `POST` | `/api/contacts` | USER/ADMIN | `201`, DTO контакта |
| `GET` | `/api/contacts` | USER/ADMIN | `200`, массив личных контактов |
| `GET` | `/api/contacts/{contactId}` | USER/ADMIN + владение | `200`, DTO контакта |
| `PUT` | `/api/contacts/{contactId}` | USER/ADMIN + владение | `200`, DTO **с прежним ID** |
| `DELETE` | `/api/contacts/{contactId}` | USER/ADMIN + владение | `200`, пустое тело |

Тело создания/изменения контакта:

```json
{
  "name": "John",
  "surname": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890"
}
```

Ответ:

```json
{
  "id": 1,
  "name": "John",
  "surname": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890"
}
```

`email` и `phoneNumber` необязательны. Контакты персональные: создание и импорт дедуплицируют данные только в адресной книге текущего пользователя. Изменение обновляет существующую запись и сохраняет её `id`. Если новые данные совпадают с другим контактом того же пользователя, возвращается `409`; запись не создаётся и не сливается. Удаление удаляет личный контакт; при удалении пользователя его контакты удаляются вместе с ним.

### Административные операции

| Метод | URL | Доступ | Успешный ответ |
|---|---|---|---|
| `GET` | `/api/contacts/admin` | ADMIN | `200`, массив всех контактов с `ownerId` |
| `POST` | `/api/contacts/import` (`multipart/form-data`, part `file`) | USER/ADMIN | `200`, пустое тело |
| `GET` | `/api/users` | ADMIN | `200`, массив пользователей |
| `GET` | `/api/users/{userId}` | ADMIN | `200`, DTO пользователя |
| `PUT` | `/api/users/{userId}` | ADMIN | `200`, DTO пользователя |
| `DELETE` | `/api/users/{userId}` | ADMIN | `204` |

DTO пользователя:

```json
{
  "id": 1,
  "name": "johndoe",
  "email": "john.doe@example.com",
  "role": "ROLE_USER",
  "contactIds": [1, 2]
}
```

Тело обновления пользователя: `name`, `password`, `email` обязательны; `role` необязателен, допустимы `ROLE_USER` и `ROLE_ADMIN`, при отсутствии роль не меняется. Пароль сохраняется только в виде BCrypt-хеша.

Импорт CSV принимает файл в `multipart/form-data` в части `file`; серверные пути не читаются. Ограничения: 1 MB на файл и 2 MB на запрос (превышение — `413`). Формат строки: `name,surname,email,phoneNumber` без заголовка; поддерживаются кавычки, запятые внутри кавычек и экранированные кавычки. Пустой или некорректный файл приводит к `400` без частичного импорта.

Административный список контактов использует `AdminContactDto`: поля контакта плюс `ownerId`. Одинаковые сведения разных пользователей — это разные записи с разными `id`; `ownerId` позволяет их различить.

## Ошибки

Прикладные ошибки возвращаются в формате:

```json
{
  "message": "User not found",
  "timestamp": 1700000000000
}
```

- `400` — неверные входные данные, ошибка CSV или отсутствующий файл;
- `401` — отсутствующий/недействительный/отозванный JWT либо недостаточная роль;
- `403` — попытка обратиться к контакту другого пользователя;
- `404` — пользователь или контакт не найден;
- `409` — занятое имя пользователя или конфликт контактных данных в адресной книге.

Ошибки Bean Validation возвращают `ValidationErrorResponse` (`message`, `timestamp`, `details`); часть ошибок привязки параметров возвращается в формате `ProblemDetail`.

## Тесты и изоляция БД

Интеграционные тесты работают только с изолированной H2 in-memory БД. Это защищено `TestDatabaseGuard` и проверками в `HibernateConfig`: при активном профиле `test` приложение отказывается стартовать с не-H2 подключением. Не изменяйте тестовые настройки так, чтобы они указывали на рабочую PostgreSQL: тесты создают и удаляют схему.

Production-приложение и реальная PostgreSQL в рамках проверок не запускались.

## Структура проекта

```text
src/main/java/org/example/
├── config/      Spring Security, JWT-фильтр, Hibernate, OpenAPI
├── controller/  REST-контроллеры и обработчик ошибок
├── dao/         DAO на Hibernate SessionFactory
├── dto/         входные/выходные DTO
├── entity/      User, Contact, RevokedToken
├── service/     бизнес-логика, JWT, отзыв токенов
└── util/        CSV-парсер и проверки
src/main/resources/
├── application.properties
├── jdbc.properties
├── hibernate.cfg.xml
├── logback-spring.xml
└── db/migration/
```

Логирование настраивается в `logback-spring.xml`: консоль и ротация в каталог `logs/`. Значения bind-параметров SQL на уровне TRACE намеренно не включены, чтобы не писать в журналы чувствительные данные.

## Ограничения

- пагинация API отсутствует;
- формат `Basic_schema.sql` может не совпадать с текущими Hibernate mappings, файл не применяется автоматически;
- отзыв JWT хранится в PostgreSQL и очищается от просроченных записей при следующем logout;
- отдельного веб-клиента, Docker-конфигурации и облачного развёртывания в проекте нет.
