package ru.yandex.practicum.filmorate.service.inMemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InMemoryUserService implements UserService {
    private final UserStorage userStorage;

    @Autowired
    public InMemoryUserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Override
    public User createUser(User user) {
        validateUser(user);
        return userStorage.createUser(user);
    }

    @Override
    public User updateUser(User newUser) {
        validateUser(newUser);
        return userStorage.updateUser(newUser);
    }

    @Override
    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    @Override
    public User getUser(Long id) {
        return userStorage.getUser(id);
    }

    @Override
    public void addFriend(Long id, Long friendId) {
        User user = userStorage.getUser(id);
        User friend = userStorage.getUser(friendId);
        validateFilm(id, friendId);
        if (id.equals(friendId)) {
            log.warn("Пользователь пытается добавить в друзья самого себя");
            throw new ValidationException("Пользователь не может добавить в друзья самого себя");
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(id);
    }

    @Override
    public void deleteFriend(Long id, Long friendId) {
        User user = userStorage.getUser(id);
        User friend = userStorage.getUser(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(id);
    }

    @Override
    public Set<User> getAllFriends(Long id) {
        if (userStorage.getUser(id) == null) {
            log.warn("Пользователь с id = {}, не найден!", id);
            throw new NotFoundException("Пользователь с id = " + id + ", не найден!");
        }

        return userStorage.getAllUsers()
                .stream()
                .filter(user -> user.getFriends().contains(id))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<User> getCommonFriends(Long id, Long otherId) {
        return userStorage.getAllUsers()
                .stream()
                .filter(user -> user.getFriends().contains(id) && user.getFriends().contains(otherId))
                .collect(Collectors.toList());
    }

    public void validateUser(User user) {
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

    public void validateFilm(Long id, Long friendId) {
        User user = userStorage.getUser(id);
        User friend = userStorage.getUser(friendId);
        if (user == null) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        } else if (friend == null) {
            log.warn("Пользователь с friendId = {} не найден", friendId);
            throw new NotFoundException(
                    "Пользователь, которого добавляете в друзья, с id = " + friendId + " не найден"
            );
        }
    }
}