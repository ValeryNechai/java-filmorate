package ru.yandex.practicum.filmorate.storage.db.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Feed;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FeedRowMapper implements RowMapper<Feed> {
    @Override
    public Feed mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Feed feed = new Feed();
        feed.setTimestamp(resultSet.getTimestamp("TIMESTAMP").getTime());
        feed.setUserId(resultSet.getLong("USER_ID"));
        feed.setEventType(EventType.valueOf(resultSet.getString("EVENT_TYPE")));
        feed.setOperation(Operation.valueOf(resultSet.getString("OPERATION")));
        feed.setEventId(resultSet.getLong("EVENT_ID"));
        feed.setEntityId(resultSet.getLong("ENTITY_ID"));

        return feed;
    }
}
