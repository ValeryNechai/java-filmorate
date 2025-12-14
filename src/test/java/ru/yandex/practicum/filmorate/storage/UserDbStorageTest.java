package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({UserDbStorage.class, UserRowMapper.class, FriendDbStorage.class,
        FeedDbStorage.class, FeedRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserDbStorageTest {
    private final UserDbStorage userDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @Test
    public void shouldCreateUser() {
        User testUser2 = new User();
        testUser2.setEmail("maria.sidorova@gmail.com");
        testUser2.setLogin("maria_s");
        testUser2.setName("Мария Сидорова");
        testUser2.setBirthday(LocalDate.of(1985, 5, 15));

        User createdUser2 = userDbStorage.createUser(testUser2);

        assertThat(createdUser2)
                .isNotNull()
                .hasFieldOrProperty("id")
                .hasFieldOrPropertyWithValue("email", "maria.sidorova@gmail.com")
                .hasFieldOrPropertyWithValue("name", "Мария Сидорова");
        assertThat(createdUser2.getBirthday()).isEqualTo(LocalDate.of(1985, 5, 15));
    }

    @Test
    public void shouldFindUserById() {
        User testUser1 = new User();
        testUser1.setEmail("ivan.petrov@mail.ru");
        testUser1.setLogin("ivan_petrov");
        testUser1.setName("Иван Петров");
        testUser1.setBirthday(LocalDate.of(1990, 5, 15));

        User createdUser1 = userDbStorage.createUser(testUser1);
        long createdUserId = createdUser1.getId();
        User foundUser = userDbStorage.getUser(createdUserId);
        long foundUserId = foundUser.getId();

        assertThat(foundUser)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", foundUserId)
                .hasFieldOrPropertyWithValue("email", "ivan.petrov@mail.ru")
                .hasFieldOrPropertyWithValue("name", "Иван Петров");
    }

    @Test
    public void shouldUpdateUser() {
        User testUser1 = new User();
        testUser1.setEmail("ivan.petrov@mail.ru");
        testUser1.setLogin("ivan_petrov");
        testUser1.setName("Иван Петров");
        testUser1.setBirthday(LocalDate.of(1990, 5, 15));
        User createdUser1 = userDbStorage.createUser(testUser1);

        testUser1.setName("Ванечка Петров");

        userDbStorage.updateUser(testUser1);

        assertThat(createdUser1)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "Ванечка Петров");
    }

    @Test
    public void shouldFindAllUsers() {
        User testUser1 = new User();
        testUser1.setEmail("ivan.petrov@mail.ru");
        testUser1.setLogin("ivan_petrov");
        testUser1.setName("Иван Петров");
        testUser1.setBirthday(LocalDate.of(1990, 5, 15));
        User createdUser1 = userDbStorage.createUser(testUser1);

        User testUser2 = new User();
        testUser2.setEmail("maria.sidorova@gmail.com");
        testUser2.setLogin("maria_s");
        testUser2.setName("Мария Сидорова");
        testUser2.setBirthday(LocalDate.of(1985, 5, 15));
        User createdUser2 = userDbStorage.createUser(testUser2);

        Collection<User> allUsers = userDbStorage.getAllUsers();

        assertThat(allUsers)
                .isNotNull()
                .hasSize(2)
                .extracting(User::getName)
                .contains("Иван Петров", "Мария Сидорова");
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
