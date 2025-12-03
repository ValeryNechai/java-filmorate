package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FriendDbStorage implements FriendStorage {
    private final JdbcTemplate jdbc;
    private final RowMapper<User> mapper;

    @Override
    public void addFriend(Long id, Long friendId) {
        String addFriendQuery = "INSERT INTO FRIENDSHIPS(USER_ID, FRIEND_ID) VALUES (?, ?)";
        jdbc.update(addFriendQuery, id, friendId);
        log.debug("Друг успешно добавлен.");
    }

    @Override
    public void deleteFriend(Long id, Long friendId) {
        String deleteFriendQuery = "DELETE FROM FRIENDSHIPS WHERE USER_ID = ? AND FRIEND_ID = ?";
        jdbc.update(deleteFriendQuery, id, friendId);
        log.debug("Друг успешно удален.");
    }

    @Override
    public Set<Long> getAllFriendsIdByUserId(Long id) {
        String findAllFriendsQuery = "SELECT FRIEND_ID FROM FRIENDSHIPS WHERE USER_ID = ?";
        List<Long> friends = jdbc.queryForList(findAllFriendsQuery, Long.class, id);

        return new HashSet<>(friends);
    }

    @Override
    public Set<User> getAllFriendsByUserId(Long id) {
        String findAllFriendsQuery = "SELECT u.USER_ID, u.EMAIL, u.LOGIN, u.USER_NAME, u.BIRTHDAY " +
                                    "FROM FRIENDSHIPS f " +
                                    "LEFT JOIN USERS u ON f.FRIEND_ID=u.USER_ID " +
                                    "WHERE f.USER_ID = ?";
        List<User> friends = jdbc.query(findAllFriendsQuery, mapper, id);

        return new HashSet<>(friends);
    }

    @Override
    public Collection<User> getCommonFriends(Long id, Long otherId) {
        String findCommonFriends =
                        "SELECT u.* " +
                        "FROM FRIENDSHIPS f1 " +
                        "INNER JOIN FRIENDSHIPS f2 ON f1.FRIEND_ID = f2.FRIEND_ID " +
                        "INNER JOIN USERS u ON f1.FRIEND_ID = u.USER_ID " +
                        "WHERE f1.USER_ID = ? " +
                        "  AND f2.USER_ID = ? " +
                        "  AND f1.FRIEND_ID != f1.USER_ID " +
                        "  AND f2.FRIEND_ID != f2.USER_ID ";

        List<User> commonFriends = jdbc.query(findCommonFriends, mapper, id, otherId);
        commonFriends.forEach(user -> {
            Set<Long> friends = getAllFriendsIdByUserId(user.getId());
            user.setFriends(friends);
        });

        return new HashSet<>(commonFriends);
    }
}
