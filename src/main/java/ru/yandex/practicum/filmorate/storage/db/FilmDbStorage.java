package ru.yandex.practicum.filmorate.storage.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class FilmDbStorage extends AbstractDbStorage<Film> implements FilmStorage {
    private static final String INSERT_FILM_DIRECTOR =
            "INSERT INTO FILM_DIRECTORS (FILM_ID, DIRECTOR_ID) VALUES (?, ?)";

    private static final String DELETE_FILM_DIRECTORS =
            "DELETE FROM FILM_DIRECTORS WHERE FILM_ID = ?";

    private static final String FIND_DIRECTORS_BY_FILM_ID =
            """
                    SELECT d.DIRECTOR_ID, d.DIRECTOR_NAME
                    FROM DIRECTORS d
                    JOIN FILM_DIRECTORS fd ON d.DIRECTOR_ID = fd.DIRECTOR_ID
                    WHERE fd.FILM_ID = ?
                    """;

    private static final String FIND_DIRECTORS_BY_FILM_IDS =
            """
                    SELECT fd.FILM_ID, d.DIRECTOR_ID, d.DIRECTOR_NAME
                    FROM FILM_DIRECTORS fd
                    JOIN DIRECTORS d ON d.DIRECTOR_ID = fd.DIRECTOR_ID
                    WHERE fd.FILM_ID IN (%s)
                    """;

    private static final String FIND_FILMS_BY_DIRECTOR_SORT_BY_YEAR =
            """
                    SELECT f.*, r.RATING_NAME
                    FROM FILMS f
                    JOIN FILM_DIRECTORS fd ON f.FILM_ID = fd.FILM_ID
                    LEFT JOIN MPA_RATINGS r ON f.RATING_ID = r.RATING_ID
                    WHERE fd.DIRECTOR_ID = ?
                    ORDER BY EXTRACT(YEAR FROM f.RELEASE_DATE), f.FILM_ID
                    """;

    private static final String FIND_FILMS_BY_DIRECTOR_SORT_BY_LIKES =
            """
                    SELECT f.*, r.RATING_NAME,
                           (SELECT COUNT(*) FROM LIKES l WHERE l.FILM_ID = f.FILM_ID) AS like_count
                    FROM FILMS f
                    JOIN FILM_DIRECTORS fd ON f.FILM_ID = fd.FILM_ID
                    LEFT JOIN MPA_RATINGS r ON f.RATING_ID = r.RATING_ID
                    WHERE fd.DIRECTOR_ID = ?
                    ORDER BY like_count DESC, f.FILM_ID
                    """;


    public FilmDbStorage(JdbcTemplate jdbc, RowMapper<Film> mapper) {
        super(jdbc, mapper);
    }

    @Override
    public Film createFilm(Film film) {
        String insertFilmQuery = "INSERT INTO FILMS(FILM_NAME, DESCRIPTION, RELEASE_DATE, " +
                "DURATION, RATING_ID) VALUES (?, ?, ?, ?, ?)";

        long id = insert(
                insertFilmQuery,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpaRating().getId()
        );
        film.setId(id);
        saveFilmGenres(film);
        saveFilmDirectors(film);
        log.debug("Фильм {} успешно добавлен", film.getName());

        return film;
    }

    @Override
    public Film updateFilm(Film newFilm) {
        String updateQuery = "UPDATE FILMS SET FILM_NAME = ?, DESCRIPTION = ?, RELEASE_DATE = ?, " +
                "DURATION = ?, RATING_ID = ? WHERE FILM_ID = ?";

        update(
                updateQuery,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate(),
                newFilm.getDuration(),
                newFilm.getMpaRating().getId(),
                newFilm.getId()
        );

        updateFilmGenres(newFilm);
        updateFilmDirectors(newFilm);
        log.debug("Фильм с id {} успешно обновлен.", newFilm.getId());

        return newFilm;
    }

    @Override
    public void deleteFilmById(Long filmId) {
        jdbc.update("DELETE FROM REVIEWS WHERE FILM_ID = ?", filmId);
        jdbc.update("DELETE FROM LIKES WHERE FILM_ID = ?", filmId);
        jdbc.update("DELETE FROM FILM_GENRES WHERE FILM_ID = ?", filmId);
        jdbc.update("DELETE FROM FILM_DIRECTORS WHERE FILM_ID = ?", filmId);
        jdbc.update("DELETE FROM FILMS WHERE FILM_ID = ?", filmId);

    }

    @Override
    public Collection<Film> getAllFilms() {
        String findAllFilmsQuery = "SELECT f.*, r.RATING_NAME FROM FILMS AS f " +
                "LEFT OUTER JOIN MPA_RATINGS AS r ON f.RATING_ID=r.RATING_ID ORDER BY FILM_ID";

        return findMany(findAllFilmsQuery);
    }

    @Override
    public Film getFilm(Long id) {
        String findFilmQuery = "SELECT f.*, r.RATING_NAME FROM FILMS AS f " +
                "LEFT OUTER JOIN MPA_RATINGS AS r ON f.RATING_ID=r.RATING_ID " +
                "WHERE f.FILM_ID = ?";

        return findOne(findFilmQuery, id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    @Override
    public Collection<Film> getPopularFilms(int count, Integer genreId, Integer year) {

        List<Object> params = new ArrayList<>();
        StringBuilder findPopularFilms = new StringBuilder(
                "SELECT f.*, r.RATING_NAME, like_counts.total_likes " +
                        "FROM PUBLIC.FILMS f " +
                        "LEFT JOIN PUBLIC.MPA_RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "LEFT JOIN ( " +
                        "    SELECT FILM_ID, COUNT(*) as total_likes " +
                        "    FROM LIKES " +
                        "    GROUP BY FILM_ID " +
                        ") like_counts ON f.FILM_ID = like_counts.FILM_ID "
        );

        List<String> whereConditions = new ArrayList<>();

        if (genreId != null) {
            findPopularFilms.append("INNER JOIN PUBLIC.FILM_GENRES fg ON f.FILM_ID = fg.FILM_ID ");
            whereConditions.add("fg.GENRE_ID = ?");
            params.add(genreId);
        }

        if (year != null) {
            whereConditions.add("EXTRACT(YEAR FROM CAST(f.RELEASE_DATE AS DATE)) = ?");
            params.add(year);
        }

        if (!whereConditions.isEmpty()) {
            findPopularFilms.append("WHERE ");
            findPopularFilms.append(String.join(" AND ", whereConditions));
        }

        if (genreId != null) {
            findPopularFilms.append("GROUP BY f.FILM_ID, r.RATING_NAME, like_counts.total_likes ");
        }

        findPopularFilms.append("ORDER BY COALESCE(like_counts.total_likes, 0) DESC, f.FILM_ID ASC LIMIT ?");
        params.add(count);


        return findMany(findPopularFilms.toString(), params.toArray());
    }

    @Override
    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        String findCommonFilms =
                "SELECT DISTINCT f.*, r.RATING_NAME  " +
                        "FROM PUBLIC.FILMS AS f " +
                        "INNER JOIN PUBLIC.LIKES AS l1 ON f.FILM_ID = l1.FILM_ID " +
                        "INNER JOIN PUBLIC.LIKES AS l2 ON f.FILM_ID = l2.FILM_ID " +
                        "LEFT JOIN MPA_RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "WHERE l1.USER_ID = ? " +
                        "AND l2.USER_ID = ? " +
                        "ORDER BY f.FILM_ID ";

        return findMany(findCommonFilms, userId, friendId);
    }

    @Override
    public boolean existsById(Long id) {
        String existsByIdQuery = "SELECT COUNT(*) FROM FILMS WHERE FILM_ID = ?";
        if (id == null) {

            return false;
        }

        Integer count = jdbc.queryForObject(
                existsByIdQuery,
                Integer.class,
                id
        );

        return count > 0;
    }

    @Override
    public Collection<Film> getRecommendations(Long userId) {
        String similarUserQuery =
                "SELECT l2.USER_ID " +
                        "FROM LIKES l1 " +
                        "JOIN LIKES l2 ON l1.FILM_ID = l2.FILM_ID " +
                        "WHERE l1.USER_ID = ? AND l2.USER_ID <> ? " +
                        "GROUP BY l2.USER_ID " +
                        "ORDER BY COUNT(*) DESC, l2.USER_ID ASC " +
                        "LIMIT 1";

        Long similarUserId = jdbc.query(
                similarUserQuery,
                rs -> rs.next() ? rs.getLong("USER_ID") : null,
                userId,
                userId
        );

        if (similarUserId == null) {
            return List.of();
        }
        String recFilmsQuery =
                "SELECT f.*, r.RATING_NAME, like_counts.total_likes " +
                        "FROM FILMS f " +
                        "LEFT JOIN MPA_RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "LEFT JOIN ( " +
                        "    SELECT FILM_ID, COUNT(*) as total_likes " +
                        "    FROM LIKES " +
                        "    GROUP BY FILM_ID " +
                        ") like_counts ON f.FILM_ID = like_counts.FILM_ID " +
                        "WHERE f.FILM_ID IN ( " +
                        "    SELECT FILM_ID FROM LIKES WHERE USER_ID = ? " +
                        ") " +
                        "AND f.FILM_ID NOT IN ( " +
                        "    SELECT FILM_ID FROM LIKES WHERE USER_ID = ? " +
                        ") " +
                        "ORDER BY like_counts.total_likes DESC NULLS LAST, f.FILM_ID ASC ";

        return findMany(recFilmsQuery, similarUserId, userId);
    }

    private void saveFilmGenres(Film film) {
        String saveQuery = "INSERT INTO FILM_GENRES (FILM_ID, GENRE_ID) VALUES (?, ?)";
        List<Object[]> batchArgs = film.getFilmGenres().stream()
                .map(genre -> new Object[]{film.getId(), genre.getId()})
                .collect(Collectors.toList());

        jdbc.batchUpdate(saveQuery, batchArgs);
        log.debug("Жанры успешно добавлены.");
    }

    private void updateFilmGenres(Film film) {
        String deleteQuery = "DELETE FROM FILM_GENRES WHERE FILM_ID = ?";
        jdbc.update(deleteQuery, film.getId());

        saveFilmGenres(film);
        log.debug("Жанры успешно обновлены.");
    }

    @Override
    public List<Film> searchFilms(String query, String by) {
        log.debug("Выполнение поиска фильмов. Запрос: {}, by: {}", query, by);

        String trimmedQuery = query.trim();
        String trimmedBy = (by == null || by.trim().isEmpty()) ? "title" : by.trim().toLowerCase();

        boolean searchByTitle = trimmedBy.contains("title");
        boolean searchByDirector = trimmedBy.contains("director");

        String searchPattern = "%" + query.trim().toLowerCase() + "%";

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT f.*, r.RATING_NAME, COALESCE(lc.like_count, 0) as like_count ");
        sql.append("FROM FILMS f ");
        sql.append("LEFT JOIN MPA_RATINGS r ON f.RATING_ID = r.RATING_ID ");
        sql.append("LEFT JOIN ( ");
        sql.append("    SELECT FILM_ID, COUNT(*) as like_count ");
        sql.append("    FROM LIKES ");
        sql.append("    GROUP BY FILM_ID ");
        sql.append(") lc ON f.FILM_ID = lc.FILM_ID ");

        if (searchByTitle && searchByDirector) {
            sql.append("WHERE (LOWER(f.FILM_NAME) LIKE ? ");
            params.add(searchPattern);

            sql.append("OR EXISTS (SELECT 1 FROM FILM_DIRECTORS fd ");
            sql.append("JOIN DIRECTORS d ON fd.DIRECTOR_ID = d.DIRECTOR_ID ");
            sql.append("WHERE fd.FILM_ID = f.FILM_ID AND LOWER(d.DIRECTOR_NAME) LIKE ?)) ");
            params.add(searchPattern);

        } else if (searchByTitle) {
            sql.append("WHERE LOWER(f.FILM_NAME) LIKE ? ");
            params.add(searchPattern);
        } else if (searchByDirector) {
            sql.append("WHERE EXISTS (SELECT 1 FROM FILM_DIRECTORS fd ");
            sql.append("JOIN DIRECTORS d ON fd.DIRECTOR_ID = d.DIRECTOR_ID ");
            sql.append("WHERE fd.FILM_ID = f.FILM_ID AND LOWER(d.DIRECTOR_NAME) LIKE ?) ");
            params.add(searchPattern);
        }

        sql.append("ORDER BY COALESCE(lc.like_count, 0) DESC NULLS LAST");

        String searchQuery = sql.toString();
        log.debug("SQL запрос поиска: {}", searchQuery);
        log.debug("Параметры: {}", params);

        List<Film> films;
        try {

            films = findMany(searchQuery, params.toArray());
            log.debug("Найдено фильмов: {}", films.size());
        } catch (Exception e) {
            log.error("Ошибка при выполнении поиска: {}", e.getMessage(), e);
            log.error("SQL: {}", searchQuery);
            log.error("Params: {}", params);
            throw new InternalServerException("Ошибка при выполнении поиска фильмов: " + e.getMessage());
        }

        return films;
    }

    private void saveFilmDirectors(Film film) {
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            log.debug("Фильм id={} сохранён без режиссёров", film.getId());
            return;
        }

        for (Director director : film.getDirectors()) {
            jdbc.update(INSERT_FILM_DIRECTOR, film.getId(), director.getId());
            log.debug("Привязка фильма id={} к режиссёру id={}", film.getId(), director.getId());
        }
    }

    private void updateFilmDirectors(Film film) {
        jdbc.update(DELETE_FILM_DIRECTORS, film.getId());
        log.debug("Удалены связи фильм ↔ режиссёр для фильма id={}", film.getId());
        saveFilmDirectors(film);
    }

    @Override
    public Set<Director> getDirectorsByFilmId(Long filmId) {
        return new HashSet<>(jdbc.query(
                FIND_DIRECTORS_BY_FILM_ID,
                (rs, rowNum) -> new Director(
                        rs.getLong("DIRECTOR_ID"),
                        rs.getString("DIRECTOR_NAME")
                ),
                filmId
        ));
    }

    @Override
    public Map<Long, Set<Director>> getDirectorsByFilmIds(Set<Long> filmIds) {
        Map<Long, Set<Director>> result = new HashMap<>();
        if (filmIds.isEmpty()) {
            log.debug("Запрос режиссёров: список фильмов пуст");
            return result;
        }

        log.debug("Загрузка режиссёров для {} фильмов", filmIds.size());

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = FIND_DIRECTORS_BY_FILM_IDS.formatted(placeholders);

        jdbc.query(sql, rs -> {
            long filmId = rs.getLong("FILM_ID");
            Director director = new Director(
                    rs.getLong("DIRECTOR_ID"),
                    rs.getString("DIRECTOR_NAME")
            );
            result.computeIfAbsent(filmId, k -> new HashSet<>()).add(director);
        }, filmIds.toArray());

        return result;
    }

    @Override
    public Collection<Film> getFilmsByDirector(Long directorId, String sortBy) {
        log.debug("Запрос фильмов режиссёра id={}, сортировка={}", directorId, sortBy);

        String sql;

        if ("year".equalsIgnoreCase(sortBy)) {
            sql = FIND_FILMS_BY_DIRECTOR_SORT_BY_YEAR;
        } else if ("likes".equalsIgnoreCase(sortBy)) {
            sql = FIND_FILMS_BY_DIRECTOR_SORT_BY_LIKES;
        } else {
            throw new ValidationException("Некорректный параметр sortBy: " + sortBy);
        }
        List<Film> films = findMany(sql, directorId);

        return films;
    }
}
