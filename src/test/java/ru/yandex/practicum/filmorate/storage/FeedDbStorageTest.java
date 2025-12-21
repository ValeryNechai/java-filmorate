package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Feed;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.FeedDbStorage;
import ru.yandex.practicum.filmorate.storage.db.FriendDbStorage;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.db.mapper.FeedRowMapper;
import ru.yandex.practicum.filmorate.storage.db.mapper.UserRowMapper;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FriendDbStorage.class, UserRowMapper.class, UserDbStorage.class,
        FeedDbStorage.class, FeedRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FeedDbStorageTest {
    private final FriendDbStorage friendDbStorage;
    private final UserDbStorage userDbStorage;
    private final FeedDbStorage feedDbStorage;
    private User createdUser1;
    private User createdUser2;

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
    }

    @Test
    public void shouldCreateFeedAndGetFeeds() {
        long id1 = createdUser1.getId();
        long id2 = createdUser2.getId();

        feedDbStorage.createFeed(id1, EventType.FRIEND, Operation.ADD, id2);

        Collection<Feed> feedsByUser = feedDbStorage.getFeedsByUserId(id1);

        assertThat(feedsByUser)
                .isNotNull()
                .hasSize(1)
                .extracting(Feed::getEntityId)
                .contains(id2);
    }
}