package ru.yandex.practicum.filmorate.storage.db;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FriendStorage {

    void addFriend(Long id, Long friendId);

    void deleteFriend(Long id, Long friendId);

    Set<Long> getAllFriendsIdByUserId(Long id);

    Set<User> getAllFriendsByUserId(Long id);

    Collection<User> getCommonFriends(Long id, Long otherId);

    Map<Long, Set<Long>> getFriendsByAllUsers();
}
