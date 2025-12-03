package ru.yandex.practicum.filmorate.controller.inMemory;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.service.inMemory.InMemoryUserService;
import ru.yandex.practicum.filmorate.storage.inMemory.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {
    UserStorage userStorage = new InMemoryUserStorage();
    UserService userService = new InMemoryUserService(userStorage);
    UserController uc = new UserController(userService);

    @Test
    void shouldThrowException_WhenEmailNotValid() {
        User user1 = new User(1L,
                "jhdh", "Kate", null, LocalDate.of(1997, 10, 10), null);
        assertThrows(ValidationException.class, () -> uc.createUser(user1), "Email не соответствует требованиям.");
    }

    @Test
    void shouldThrowException_WhenLoginNotValid() {
        User user2 = new User(2L,
                "jh@dh", "Kate S", null, LocalDate.of(1997, 10, 10), null);
        assertThrows(ValidationException.class, () -> uc.createUser(user2), "Login не соответствует требованиям.");
    }

    @Test
    void shouldThrowException_WhenLoginIsEmpty() {
        User user3 = new User(2L,
                "jh@dh", "", null, LocalDate.of(1997, 10, 10), null);
        assertThrows(ValidationException.class, () -> uc.createUser(user3), "Login не соответствует требованиям.");
    }

    @Test
    void shouldThrowException_WhenBirthdayInFuture() {
        User user4 = new User(2L,
                "jh@dh", "Kate S", "Kate", LocalDate.of(2026, 10, 10), null);
        assertThrows(ValidationException.class, () -> uc.createUser(user4), "Дата рождения не может быть из будущего.");
    }

    @Test
    void shouldGetAllUsers() {
        User user = new User(2L,
                "jh@dh", "Kate", "Kate", LocalDate.of(1997, 10, 10), null);
        uc.createUser(user);
        assertEquals(1, uc.getAllUsers().size(), "Пользователь с верными данными должен быть добавлен!");
    }
}