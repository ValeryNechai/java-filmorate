package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    UserController uc = new UserController();

    @Test
    void testCreateUser() {
        User user1 = new User(1L,
                "jhdh", "Kate", null, LocalDate.of(1997, 10, 10));
        assertThrows(ValidationException.class, () -> uc.createUser(user1), "Email не соответствует требованиям.");
        assertEquals(0, uc.getAllUsers().size(), "Пользователь с неверными данными не будет добален!");

        User user2 = new User(2L,
                "jh@dh", "Kate S", null, LocalDate.of(1997, 10, 10));
        assertThrows(ValidationException.class, () -> uc.createUser(user2), "Login не соответствует требованиям.");

        User user3 = new User(2L,
                "jh@dh", "", null, LocalDate.of(1997, 10, 10));
        assertThrows(ValidationException.class, () -> uc.createUser(user3), "Login не соответствует требованиям.");

        User user4 = new User(2L,
                "jh@dh", "Kate S", "Kate", LocalDate.of(2026, 10, 10));
        assertThrows(ValidationException.class, () -> uc.createUser(user4), "Дата рождения не может быть из будущего.");
    }

    @Test
    void updateUser() {
        User user = new User(2L,
                "jh@dh", "Kate", "Kate", LocalDate.of(1997, 10, 10));
        uc.createUser(user);

        User user1 = new User(user.getId(),
                "jhdh", "Kate", "Kate", LocalDate.of(1997, 10, 10));
        assertThrows(ValidationException.class, () -> uc.updateUser(user1), "Email не соответствует требованиям.");

        User user2 = new User(user.getId(),
                "jh@dh", "Kate S", "Kate", LocalDate.of(1997, 10, 10));
        assertThrows(ValidationException.class, () -> uc.updateUser(user2), "Login не соответствует требованиям.");

        User user3 = new User(user.getId(),
                "jh@dh", "", "Kate", LocalDate.of(1997, 10, 10));
        assertThrows(ValidationException.class, () -> uc.updateUser(user3), "Login не соответствует требованиям.");

        User user4 = new User(user.getId(),
                "jh@dh", "Kate S", "Kate", LocalDate.of(2026, 10, 10));
        assertThrows(ValidationException.class, () -> uc.updateUser(user4), "Дата рождения не может быть из будущего.");
    }

    @Test
    void trstGetAllUsers() {
        User user = new User(2L,
                "jh@dh", "Kate", "Kate", LocalDate.of(1997, 10, 10));
        uc.createUser(user);
        assertEquals(1, uc.getAllUsers().size(), "Пользователь с верными данными должен быть добавлен!");
    }
}