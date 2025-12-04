package ru.yandex.practicum.filmorate.storage.db;

import java.util.Map;
import java.util.Set;

public interface LikesStorage {

    Set<Long> getLikesByFilmId(Long filmId);

    void addLike(Long filmId, Long userId);

    void deleteLike(Long filmId, Long userId);

    Map<Long, Set<Long>> getLikesByAllFilms();
}
