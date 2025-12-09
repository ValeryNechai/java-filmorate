package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

public interface UserStorage {
    User createUser(User user);

    User updateUser(User newUser);

    Collection<User> getAllUsers();

    User getUser(Long id);

    boolean existsById(Long id);
}
