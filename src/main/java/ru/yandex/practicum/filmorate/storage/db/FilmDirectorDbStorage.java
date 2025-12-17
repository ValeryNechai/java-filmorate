package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmDirectorDbStorage implements FilmDirectorStorage {

    private final JdbcTemplate jdbc;
    private final RowMapper<Director> directorRowMapper;

    @Override
    public Set<Director> getDirectorsByFilmId(Long filmId) {
        String sql =
                "SELECT d.DIRECTOR_ID, d.DIRECTOR_NAME " +
                        "FROM FILM_DIRECTORS fd " +
                        "JOIN DIRECTORS d ON fd.DIRECTOR_ID = d.DIRECTOR_ID " +
                        "WHERE fd.FILM_ID = ? " +
                        "ORDER BY d.DIRECTOR_ID";

        return new LinkedHashSet<>(jdbc.query(sql, directorRowMapper, filmId));
    }

    @Override
    public Map<Long, Set<Director>> getDirectorsByFilmIds(Set<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = String.format(
                "SELECT fd.FILM_ID, d.DIRECTOR_ID, d.DIRECTOR_NAME " +
                        "FROM FILM_DIRECTORS fd " +
                        "JOIN DIRECTORS d ON fd.DIRECTOR_ID = d.DIRECTOR_ID " +
                        "WHERE fd.FILM_ID IN (%s)", placeholders
        );

        return jdbc.query(sql, filmIds.toArray(), rs -> {
            Map<Long, Set<Director>> result = new HashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("FILM_ID");
                Director director = new Director(
                        rs.getLong("DIRECTOR_ID"),
                        rs.getString("DIRECTOR_NAME")
                );
                result.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(director);
            }
            return result;
        });
    }

    @Override
    public void saveFilmDirectors(Long filmId, Set<Long> directorIds) {
        if (directorIds == null || directorIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO FILM_DIRECTORS (FILM_ID, DIRECTOR_ID) VALUES (?, ?)";
        List<Object[]> batch = directorIds.stream()
                .map(directorId -> new Object[]{filmId, directorId})
                .toList();

        jdbc.batchUpdate(sql, batch);
        log.debug("Режиссёры сохранены для фильма {}", filmId);
    }

    @Override
    public void updateFilmDirectors(Long filmId, Set<Long> directorIds) {
        jdbc.update("DELETE FROM FILM_DIRECTORS WHERE FILM_ID = ?", filmId);
        saveFilmDirectors(filmId, directorIds);
    }
}

