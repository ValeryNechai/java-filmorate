package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long id, Long friendId) {
        User user = userStorage.getUser(id);
        User friend = userStorage.getUser(friendId);
        if (user != null && friend != null) {
            if (user.getFriends().contains(friendId)) {
                log.warn("Попытка повторно добавить пользователя в друзья! Пользователь уже в друзьях");
                throw new ValidationException("Пользователь уже в друзьях");
            }

            if (id.equals(friendId)) {
                log.warn("Пользователь пытается добавить в друзья самого себя");
                throw new ValidationException("Пользователь не может добавить в друзья самого себя");
            }

            user.getFriends().add(friendId);
            friend.getFriends().add(id);
        } else if (user == null) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        } else {
            log.warn("Пользователь с friendId = {} не найден", friendId);
            throw new NotFoundException(
                    "Пользователь, которого добавляете в друзья, с id = " + friendId + " не найден"
            );
        }
    }

    public void deleteFriend(Long id, Long friendId) {
        User user = userStorage.getUser(id);
        User friend = userStorage.getUser(friendId);
        if (user != null && friend != null) {
            if (!user.getFriends().contains(friendId)) {
                log.warn("Пользователь пытается удалить пользователя, который отсутствует у него в друзьях");
                throw new ValidationException("Пользователь не может быть удален, так как не в друзьях");
            }

            user.getFriends().remove(friendId);
            friend.getFriends().remove(id);
        } else if (user == null) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        } else {
            log.warn("Пользователь с friendId = {} не найден", friendId);
            throw new NotFoundException("Пользователь с id = " + friendId + " не найден");
        }
    }

    public Collection<User> getAllFriends(Long id) {
        if (userStorage.getUser(id) == null) {
            log.warn("Пользователь с id = {}, не найден!", id);
            throw new NotFoundException("Пользователь с id = " + id + ", не найден!");
        }

        return userStorage.getAllUsers()
                .stream()
                .filter(user -> user.getFriends().contains(id))
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long id, Long otherId) {
        return userStorage.getAllUsers()
                .stream()
                .filter(user -> user.getFriends().contains(id) && user.getFriends().contains(otherId))
                .collect(Collectors.toList());
    }
}
