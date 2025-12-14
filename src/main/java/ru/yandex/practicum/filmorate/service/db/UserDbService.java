package ru.yandex.practicum.filmorate.service.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Feed;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.db.FeedStorage;
import ru.yandex.practicum.filmorate.storage.db.FriendStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

@Service
@Slf4j
public class UserDbService implements UserService {
    private final UserStorage userStorage;
    private final FriendStorage friendStorage;
    private final FeedStorage feedStorage;

    @Autowired
    public UserDbService(UserStorage userStorage, FriendStorage friendStorage, FeedStorage feedStorage) {
        this.userStorage = userStorage;
        this.friendStorage = friendStorage;
        this.feedStorage = feedStorage;
    }

    @Override
    public User createUser(User user) {
        validateUser(user);
        return userStorage.createUser(user);
    }

    @Override
    public User updateUser(User newUser) {
        if (!userStorage.existsById(newUser.getId())) {
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден.");
        }
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
        validateFilm(id, friendId);
        if (id.equals(friendId)) {
            log.warn("Пользователь пытается добавить в друзья самого себя");
            throw new ValidationException("Пользователь не может добавить в друзья самого себя");
        }

        friendStorage.addFriend(id, friendId);
    }

    @Override
    public void deleteFriend(Long id, Long friendId) {
        validateFriend(id, friendId);

        friendStorage.deleteFriend(id, friendId);
    }

    @Override
    public Set<User> getAllFriends(Long id) {
        if (userStorage.getUser(id) == null) {
            log.warn("Пользователь с id = {}, не найден!", id);
            throw new NotFoundException("Пользователь с id = " + id + ", не найден!");
        }

        return friendStorage.getAllFriendsByUserId(id);
    }

    @Override
    public Collection<User> getCommonFriends(Long id, Long otherId) {
        validateFriend(id, otherId);
        return friendStorage.getCommonFriends(id, otherId);
    }

    @Override
    public Collection<Feed> getFeedsByUserId(Long id) {
        return feedStorage.getFeedsByUserId(id);
    }

    private void validateUser(User user) {
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

    private void validateFilm(Long id, Long friendId) {
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

    private void validateFriend(Long id, Long friendId) {
        if (id == null || friendId == null) {
            throw new ValidationException("ID пользователей не могут быть null");
        }

        if (id.equals(friendId)) {
            throw new ValidationException("Пользователь не может выполнять операции сам с собой");
        }

        if (!userStorage.existsById(id) || !userStorage.existsById(friendId)) {
            throw new NotFoundException("Пользователь не найден.");
        }
    }
}
