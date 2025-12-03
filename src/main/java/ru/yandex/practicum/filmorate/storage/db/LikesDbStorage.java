package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class LikesDbStorage implements LikesStorage {
    private final JdbcTemplate jdbc;

    @Override
    public Set<Long> getLikesByFilmId(Long filmId) {
        String findLikesByFilmId = "SELECT USER_ID FROM LIKES WHERE FILM_ID = ?";
        List<Long> likes = jdbc.queryForList(findLikesByFilmId, Long.class, filmId);
        return new HashSet<>(likes);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String addLikeQuery = "INSERT INTO LIKES(USER_ID, FILM_ID) VALUES (?, ?)";
        jdbc.update(addLikeQuery, userId, filmId);
        log.debug("Лайк успешно добавлен.");
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        String deleteLikeQuery = "DELETE FROM LIKES WHERE FILM_ID = ? AND USER_ID = ?";
        jdbc.update(deleteLikeQuery, filmId, userId);
        log.debug("Лайк успешно удален.");
    }
}
