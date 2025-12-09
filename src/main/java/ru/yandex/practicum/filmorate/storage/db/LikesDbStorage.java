package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class LikesDbStorage implements LikesStorage {
    private final JdbcTemplate jdbc;
    private static final String FIND_LIKES_BY_FILM_ID_QUERY = "SELECT USER_ID FROM LIKES WHERE FILM_ID = ?";
    private static final String ADD_LIKE_QUERY = "INSERT INTO LIKES(USER_ID, FILM_ID) VALUES (?, ?)";
    private static final String DELETE_LIKE_QUERY = "DELETE FROM LIKES WHERE FILM_ID = ? AND USER_ID = ?";
    private static final String FIND_LIKES_BY_ALL_FILMS_QUERY = "SELECT FILM_ID, USER_ID FROM LIKES";

    @Override
    public Set<Long> getLikesByFilmId(Long filmId) {
        List<Long> likes = jdbc.queryForList(FIND_LIKES_BY_FILM_ID_QUERY, Long.class, filmId);
        return new HashSet<>(likes);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbc.update(ADD_LIKE_QUERY, userId, filmId);
        log.debug("Лайк успешно добавлен.");
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        jdbc.update(DELETE_LIKE_QUERY, filmId, userId);
        log.debug("Лайк успешно удален.");
    }

    @Override
    public Map<Long, Set<Long>> getLikesByAllFilms() {
        return jdbc.query(FIND_LIKES_BY_ALL_FILMS_QUERY, new ResultSetExtractor<Map<Long, Set<Long>>>() {
            @Override
            public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Long, Set<Long>> result = new HashMap<>();

                while (rs.next()) {
                    Long userId = rs.getLong("USER_ID");
                    Long filmId = rs.getLong("FILM_ID");
                    result.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
                }
                return result;
            }
        });
    }

    @Override
    public Map<Long, Set<Long>> getLikesByFilmIds(Set<Long> filmIds) {
        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String findLikesByFilmIdsQuery = String.format("SELECT FILM_ID, USER_ID FROM LIKES " +
                                                        "WHERE FILM_ID IN (%s)", placeholders);

        return jdbc.query(findLikesByFilmIdsQuery, filmIds.toArray(), new ResultSetExtractor<Map<Long, Set<Long>>>() {
            @Override
            public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Long, Set<Long>> result = new HashMap<>();

                while (rs.next()) {
                    Long userId = rs.getLong("USER_ID");
                    Long filmId = rs.getLong("FILM_ID");
                    result.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
                }
                return result;
            }
        });
    }
}
