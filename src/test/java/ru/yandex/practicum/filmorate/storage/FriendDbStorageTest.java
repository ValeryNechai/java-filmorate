package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.FeedDbStorage;
import ru.yandex.practicum.filmorate.storage.db.FriendDbStorage;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.db.mapper.FeedRowMapper;
import ru.yandex.practicum.filmorate.storage.db.mapper.UserRowMapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FriendDbStorage.class, UserRowMapper.class, UserDbStorage.class,
        FeedDbStorage.class, FeedRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FriendDbStorageTest {
    private final JdbcTemplate jdbcTemplate;
    private final FriendDbStorage friendDbStorage;
    private final UserDbStorage userDbStorage;
    private User createdUser1;
    private User createdUser2;
    private User createdUser3;

    @BeforeEach
    public void createUsers() {
        User testUser1 = new User();
        testUser1.setEmail("ivan.petrov@mail.ru");
        testUser1.setLogin("ivan_petrov");
        testUser1.setName("Иван Петров");
        testUser1.setBirthday(LocalDate.of(1990, 5, 15));
        createdUser1 = userDbStorage.createUser(testUser1);

        User testUser2 = new User();
        testUser2.setEmail("maria.sidorova@gmail.com");
        testUser2.setLogin("maria_s");
        testUser2.setName("Мария Сидорова");
        testUser2.setBirthday(LocalDate.of(1985, 5, 15));
        createdUser2 = userDbStorage.createUser(testUser2);

        User testUser3 = new User();
        testUser3.setEmail("sergey.ivanov@yandex.ru");
        testUser3.setLogin("sergey_i");
        testUser3.setName("Сергей Иванов");
        testUser3.setBirthday(LocalDate.of(1995, 8, 22));
        createdUser3 = userDbStorage.createUser(testUser3);
    }

    @Test
    public void shouldAddFriend() {
        long id1 = createdUser1.getId();
        long id2 = createdUser2.getId();

        friendDbStorage.addFriend(id1, id2);

        Set<Long> friends = friendDbStorage.getAllFriendsIdByUserId(id1);

        assertThat(friends)
                .isNotNull()
                .hasSize(1)
                .containsExactly(id2);
    }

    @Test
    public void shouldDeleteFriend() {
        long id1 = createdUser1.getId();
        long id2 = createdUser2.getId();

        friendDbStorage.addFriend(id1, id2);
        friendDbStorage.deleteFriend(id1, id2);

        Set<Long> friends = friendDbStorage.getAllFriendsIdByUserId(id1);

        assertThat(friends)
                .isNotNull()
                .hasSize(0);
    }

    @Test
    public void shouldFindAllFriendsByUserId() {
        long id1 = createdUser1.getId();
        long id2 = createdUser2.getId();
        long id3 = createdUser3.getId();

        friendDbStorage.addFriend(id1, id2);
        friendDbStorage.addFriend(id1, id3);

        Set<Long> friendsIds = friendDbStorage.getAllFriendsIdByUserId(id1);

        assertThat(friendsIds)
                .hasSize(2)
                .contains(id2, id3);

        Set<User> friends = friendDbStorage.getAllFriendsByUserId(id1);

        assertThat(friends)
                .hasSize(2)
                .contains(createdUser2, createdUser3);
    }

    @Test
    public void shouldFindCommonFriends() {
        long id1 = createdUser1.getId();
        long id2 = createdUser2.getId();
        long id3 = createdUser3.getId();

        friendDbStorage.addFriend(id1, id2);
        friendDbStorage.addFriend(id1, id3);
        friendDbStorage.addFriend(id2, id3);

        Collection<User> commonFriends = friendDbStorage.getCommonFriends(id1, id2);

        assertThat(commonFriends)
                .hasSize(1)
                .contains(createdUser3);
    }

    @Test
    public void shouldFindFriendsByAllUsers() {
        long id1 = createdUser1.getId();
        long id2 = createdUser2.getId();
        long id3 = createdUser3.getId();

        friendDbStorage.addFriend(id1, id2);
        friendDbStorage.addFriend(id2, id3);
        friendDbStorage.addFriend(id1, id3);
        friendDbStorage.addFriend(id3, id1);

        Map<Long, Set<Long>> friends = friendDbStorage.getFriendsByAllUsers();

        assertThat(friends)
                .isNotEmpty()
                .hasSize(3)
                .containsEntry(createdUser1.getId(), friendDbStorage.getAllFriendsIdByUserId(id1))
                .containsEntry(createdUser2.getId(), friendDbStorage.getAllFriendsIdByUserId(id2))
                .containsEntry(createdUser3.getId(), friendDbStorage.getAllFriendsIdByUserId(id3));
    }

    @AfterEach
    public void clean() {
        jdbcTemplate.execute("DELETE FROM REVIEW_RATINGS");
        jdbcTemplate.execute("DELETE FROM REVIEWS");
        jdbcTemplate.execute("DELETE FROM LIKES");
        jdbcTemplate.execute("DELETE FROM FILM_GENRES");
        jdbcTemplate.execute("DELETE FROM FRIENDSHIPS");
        jdbcTemplate.execute("DELETE FROM FEEDS");
        jdbcTemplate.execute("DELETE FROM FILMS");
        jdbcTemplate.execute("DELETE FROM USERS");
    }
}
