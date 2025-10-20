package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final Map<Long, User> users = new HashMap<>();

    @PostMapping
    public User createUser(@RequestBody User user) {

        if (user.getName() == null) {
            user.setName(user.getLogin());
        }
        verificationNewUser(user);
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.debug("Пользователь {} успешно добавлен", user.getName());
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User newUser) {

        if (users.containsKey(newUser.getId())) {
            verificationUpdateUser(newUser);
            User oldUser = users.get(newUser.getId());
            if (newUser.getName() != null) {
                oldUser.setName(newUser.getName());
                log.debug("Имя пользователя {} успешно обновлено.", oldUser.getName());
            }
            if (newUser.getEmail() != null) {
                oldUser.setEmail(newUser.getEmail());
                log.debug("Email пользователя {} успешно обновлен.", oldUser.getName());
            }
            if (newUser.getLogin() != null) {
                oldUser.setLogin(newUser.getLogin());
                log.debug("Логин пользователя {} успешно обновлен.", oldUser.getName());
            }
            if (newUser.getBirthday() != null) {
                oldUser.setBirthday(newUser.getBirthday());
                log.debug("Дата рождения пользователя {} успешно обновлена.", oldUser.getName());
            }
            return oldUser;
        }

        log.warn("Пользователь с id = {} не найден", newUser.getId());
        throw new ValidationException("Пользователь с id = " + newUser.getId() + " не найден");
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return users.values();
    }

    public void verificationNewUser(User user) {

        log.debug("Начало проверки соответствия данных пользователя {} всем критериям.", user.getName());

        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            log.warn("Email пользователя {} не соответствует требованиям.", user.getName());
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @.");
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Логин пользователя {} не соответствует требованиям.", user.getName());
            throw new ValidationException("Логин не может быть пустым и содержать пробелы.");
        }

        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения пользователя {} не соответствует требованиям.", user.getName());
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }

        log.debug("Проверка данных пользователя {} прошла успешно.", user.getName());
    }

    public void verificationUpdateUser(User user) {

        log.debug("Начало проверки соответствия данных пользователя {} всем критериям.", user.getName());

        if (user.getEmail() != null && !user.getEmail().contains("@")) {
            log.warn("Email пользователя {} не соответствует требованиям.", user.getName());
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @.");
        }

        if (user.getLogin() != null && user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Логин пользователя {} не соответствует требованиям.", user.getName());
            throw new ValidationException("Логин не может быть пустым и содержать пробелы.");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения пользователя {} не соответствует требованиям.", user.getName());
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }

        log.debug("Проверка данных пользователя {} прошла успешно.", user.getName());
    }

    public Long getNextId() {
        long maxId = users.keySet().stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++maxId;
    }
}
