package ru.yandex.practicum.filmorate.storage.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UserDbStorage extends AbstractDbStorage<User> implements UserStorage {
    private final FriendStorage friendStorage;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbc, RowMapper<User> mapper, FriendStorage friendStorage) {
        super(jdbc, mapper);
        this.friendStorage = friendStorage;
    }

    @Override
    public User createUser(User user) {
        String insertUserQuery = "INSERT INTO USERS(EMAIL, LOGIN, USER_NAME, BIRTHDAY) " +
                "VALUES (?, ?, ?, ?)";
        long id = insert(
                insertUserQuery,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday()
        );
        user.setId(id);
        log.debug("Пользователь {} успешно добавлен", user.getName());

        return user;
    }

    @Override
    public User updateUser(User newUser) {
        String updateUserQuery = "UPDATE USERS SET EMAIL = ?, LOGIN = ?, USER_NAME = ?, BIRTHDAY = ? WHERE USER_ID = ?";
        update(
                updateUserQuery,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName(),
                newUser.getBirthday(),
                newUser.getId()
        );
        log.debug("Пользователь {} успешно обновлен", newUser.getName());

        return newUser;
    }

    @Override
    public Collection<User> getAllUsers() {
        String findAllUsersQuery = "SELECT * FROM USERS ORDER BY USER_ID";

        Map<Long, Set<Long>> friends = friendStorage.getFriendsByAllUsers();

        return findMany(findAllUsersQuery).stream()
                .peek(user -> user.setFriends(friends.get(user.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public User getUser(Long id) {
        String findUserQuery = "SELECT * FROM USERS WHERE USER_ID = ?";
        User user = findOne(findUserQuery, id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
        user.setFriends(friendStorage.getAllFriendsIdByUserId(id));

        return user;
    }

    @Override
    public boolean existsById(Long id) {
        String existsByIdQuery = "SELECT COUNT(*) FROM USERS WHERE USER_ID = ?";
        if (id == null) {
            return false;
        }

        Integer count = jdbc.queryForObject(
                existsByIdQuery,
                Integer.class,
                id
        );

        return count != null && count > 0;
    }
}
