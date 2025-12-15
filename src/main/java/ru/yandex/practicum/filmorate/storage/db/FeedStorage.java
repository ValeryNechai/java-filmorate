package ru.yandex.practicum.filmorate.storage.db;

import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Feed;
import ru.yandex.practicum.filmorate.model.Operation;

import java.util.Collection;

public interface FeedStorage {
    Feed createFeed(Long userId, EventType eventType, Operation operation, Long entityId);

    Collection<Feed> getFeedsByUserId(Long userId);
}
