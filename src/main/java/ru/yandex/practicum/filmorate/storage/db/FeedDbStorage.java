package ru.yandex.practicum.filmorate.storage.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Feed;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;

@Repository
@Slf4j
public class FeedDbStorage extends AbstractDbStorage<Feed> implements FeedStorage {
    private static final String INSERT_FEED_QUERY =
            "INSERT INTO FEEDS (TIMESTAMP, USER_ID, EVENT_TYPE, OPERATION, ENTITY_ID) VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_FEEDS_BY_USER_ID =
            "SELECT * FROM FEEDS WHERE USER_ID = ? ORDER BY TIMESTAMP ASC";

    public FeedDbStorage(JdbcTemplate jdbc, RowMapper<Feed> mapper) {
        super(jdbc, mapper);
    }

    @Override
    public Feed createFeed(Long userId, EventType eventType, Operation operation, Long entityId) {
        Feed feed = new Feed();
        long millis = System.currentTimeMillis();
        Instant now = Instant.ofEpochMilli(millis);
        feed.setTimestamp(millis);
        feed.setUserId(userId);
        feed.setEventType(eventType);
        feed.setOperation(operation);
        feed.setEntityId(entityId);

        long id = insert(
                INSERT_FEED_QUERY,
                Timestamp.from(now),
                userId,
                eventType.name(),
                operation.name(),
                entityId
        );
        feed.setEventId(id);
        log.debug(
                "Событие типа {} {} успешно добавлено в Ленту событий пользователя с id = {}.",
                operation,
                eventType,
                userId
        );

        return feed;
    }

    @Override
    public Collection<Feed> getFeedsByUserId(Long userId) {
        if (!existsFeed(userId)) {
            throw new NotFoundException("Отзывы данного пользователя не найдены");
        }
        return  findMany(FIND_FEEDS_BY_USER_ID, userId);
    }

    private boolean existsFeed(Long userId) {
        String existsFeedQuery = "SELECT COUNT(*) FROM FEEDS WHERE USER_ID = ?";
        Integer count = jdbc.queryForObject(existsFeedQuery, Integer.class, userId);
        return count != null && count > 0;
    }
}
