package ru.yandex.practicum.filmorate.storage.db;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class GenreDbStorage extends AbstractDbStorage<Genre> implements GenreStorage {
    private static final String FIND_ALL_QUERY = "SELECT * FROM GENRES";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM GENRES WHERE GENRE_ID = ?";
    private static final String FIND_BY_FILM_ID_QUERY = "SELECT fg.FILM_ID, fg.GENRE_ID, g.GENRE_NAME " +
                                                        "FROM FILM_GENRES AS fg " +
                                                        "INNER JOIN GENRES AS g ON fg.GENRE_ID = g.GENRE_ID " +
                                                        "WHERE fg.FILM_ID = ? " +
                                                        "ORDER BY fg.GENRE_ID";
    private static final String FIND_BY_ALL_FILMS_QUERY = "SELECT fg.FILM_ID, fg.GENRE_ID, g.GENRE_NAME " +
                                                        "FROM FILM_GENRES AS fg " +
                                                        "INNER JOIN GENRES AS g ON fg.GENRE_ID = g.GENRE_ID " +
                                                        "ORDER BY fg.GENRE_ID";

    public GenreDbStorage(JdbcTemplate jdbc, RowMapper<Genre> mapper) {
        super(jdbc, mapper);
    }

    @Override
    public Collection<Genre> getAllGenres() {
        return findMany(FIND_ALL_QUERY);
    }

    @Override
    public Genre getGenreById(Integer id) {
        return findOne(FIND_BY_ID_QUERY, id)
                .orElseThrow(() -> new NotFoundException("Жанр с id = " + id + " не найден"));
    }

    @Override
    public Set<Genre> getGenresByFilmId(Long filmId) {
        return new LinkedHashSet<>(findMany(FIND_BY_FILM_ID_QUERY, filmId));
    }

    @Override
    public Map<Long, Set<Genre>> getGenresByAllFilms() {
        return jdbc.query(FIND_BY_ALL_FILMS_QUERY, new ResultSetExtractor<Map<Long, Set<Genre>>>() {
            @Override
            public Map<Long, Set<Genre>> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Long, Set<Genre>> result = new HashMap<>();

                while (rs.next()) {
                    Long filmId = rs.getLong("FILM_ID");
                    Integer genreId = rs.getInt("GENRE_ID");
                    String genreName = rs.getString("GENRE_NAME");

                    Genre genre = new Genre(genreId, genreName);
                    result.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
                }
                return result;
            }
        });
    }

    @Override
    public Map<Long, Set<Genre>> getGenresByFilmIds(Set<Long> filmIds) {
        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String findByFilmIdsQuery = String.format("SELECT fg.FILM_ID, fg.GENRE_ID, g.GENRE_NAME " +
                                        "FROM FILM_GENRES AS fg " +
                                        "INNER JOIN GENRES AS g ON fg.GENRE_ID = g.GENRE_ID " +
                                        "WHERE fg.FILM_ID IN (%s) " +
                                        "ORDER BY fg.GENRE_ID", placeholders);

        return jdbc.query(findByFilmIdsQuery, filmIds.toArray(), new ResultSetExtractor<Map<Long, Set<Genre>>>() {
            @Override
            public Map<Long, Set<Genre>> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Long, Set<Genre>> result = new HashMap<>();

                while (rs.next()) {
                    Long filmId = rs.getLong("FILM_ID");
                    Integer genreId = rs.getInt("GENRE_ID");
                    String genreName = rs.getString("GENRE_NAME");

                    Genre genre = new Genre(genreId, genreName);
                    result.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
                }
                return result;
            }
        });
    }
}
