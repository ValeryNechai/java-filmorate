package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Feed;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Set;

public interface UserService {
    User createUser(User user);

    User updateUser(User newUser);

    Collection<User> getAllUsers();

    User getUser(Long id);

    void addFriend(Long id, Long friendId);

    void deleteFriend(Long id, Long friendId);

    Set<User> getAllFriends(Long id);

    Collection<User> getCommonFriends(Long id, Long otherId);

    Collection<Film> getRecommendations(Long id);

    Collection<Feed> getFeedsByUserId(Long id);
}
