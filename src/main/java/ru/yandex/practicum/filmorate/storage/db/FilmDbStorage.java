package ru.yandex.practicum.filmorate.storage.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class FilmDbStorage extends AbstractDbStorage<Film> implements FilmStorage {
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;
    private final LikesStorage likesStorage;
    private final ReviewStorage reviewStorage;
    private static final int MIN_MPA_ID = 1;
    private static final int MAX_MPA_ID = 5;
    private static final int MIN_GENRE_ID = 1;
    private static final int MAX_GENRE_ID = 6;
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


    public FilmDbStorage(JdbcTemplate jdbc, RowMapper<Film> mapper, GenreStorage genreStorage,
                         MpaRatingStorage mpaRatingStorage, LikesStorage likesStorage, ReviewStorage reviewStorage) {
        super(jdbc, mapper);
        this.genreStorage = genreStorage;
        this.mpaRatingStorage = mpaRatingStorage;
        this.likesStorage = likesStorage;
        this.reviewStorage = reviewStorage;
    }

    @Override
    public Film createFilm(Film film) {
        String insertFilmQuery = "INSERT INTO FILMS(FILM_NAME, DESCRIPTION, RELEASE_DATE, " +
                "DURATION, RATING_ID) VALUES (?, ?, ?, ?, ?)";

        Integer mpaId = validateMpa(film);

        Set<Genre> preparedGenres = validateGenres(film);
        film.setFilmGenres(new HashSet<>(preparedGenres));

        long id = insert(
                insertFilmQuery,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                mpaId
        );
        film.setId(id);
        saveFilmGenres(film);
        saveFilmDirectors(film);
        log.debug("Фильм {} успешно добавлен", film.getName());

        return getFilm(id);
    }

    @Override
    public Film updateFilm(Film newFilm) {
        String updateQuery = "UPDATE FILMS SET FILM_NAME = ?, DESCRIPTION = ?, RELEASE_DATE = ?, " +
                "DURATION = ?, RATING_ID = ? WHERE FILM_ID = ?";

        Integer mpaId = validateMpa(newFilm);

        Set<Genre> preparedGenres = validateGenres(newFilm);
        newFilm.setFilmGenres(new HashSet<>(preparedGenres));

        update(
                updateQuery,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate(),
                newFilm.getDuration(),
                mpaId,
                newFilm.getId()
        );

        updateFilmGenres(newFilm);
        updateFilmDirectors(newFilm);
        log.debug("Фильм с id {} успешно обновлен.", newFilm.getId());

        return getFilm(newFilm.getId());
    }

    @Override
    public void deleteFilmById(Long filmId) {
        jdbc.update("DELETE FROM LIKES WHERE FILM_ID = ?", filmId);
        jdbc.update("DELETE FROM FILM_GENRES WHERE FILM_ID = ?", filmId);
        jdbc.update("DELETE FROM FILMS WHERE FILM_ID = ?", filmId);
    }

    @Override
    public Collection<Film> getAllFilms() {
        String findAllFilmsQuery = "SELECT f.*, r.RATING_NAME FROM FILMS AS f " +
                "LEFT OUTER JOIN MPA_RATINGS AS r ON f.RATING_ID=r.RATING_ID ORDER BY FILM_ID";

        List<Film> films = findMany(findAllFilmsQuery);
        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Genre>> genres = genreStorage.getGenresByAllFilms();
        Map<Long, Set<Long>> likes = likesStorage.getLikesByAllFilms();
        Map<Long, Set<Long>> reviews = reviewStorage.getReviewsByAllFilms();
        Map<Long, Set<Director>> directors = getDirectorsByFilmIds(filmIds);
        return films.stream()
                .peek(film -> film.setFilmGenres(genres.getOrDefault(film.getId(), Set.of())))
                .peek(film -> film.setLikes(likes.getOrDefault(film.getId(), Set.of())))
                .peek(film -> film.setReviews(reviews.getOrDefault(film.getId(), Set.of())))
                .peek(film -> film.setDirectors(directors.getOrDefault(film.getId(), Set.of())))
                .collect(Collectors.toList());
    }

    @Override
    public Film getFilm(Long id) {
        String findFilmQuery = "SELECT f.*, r.RATING_NAME FROM FILMS AS f " +
                "LEFT OUTER JOIN MPA_RATINGS AS r ON f.RATING_ID=r.RATING_ID " +
                "WHERE f.FILM_ID = ?";

        Film film = findOne(findFilmQuery, id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));

        if (film.getMpaRating() != null && film.getMpaRating().getId() != null) {
            MpaRating fullMpa = mpaRatingStorage.getMpaById(film.getMpaRating().getId());
            film.setMpaRating(fullMpa);
        }

        film.setFilmGenres(new LinkedHashSet<>(genreStorage.getGenresByFilmId(id)));
        film.setLikes(likesStorage.getLikesByFilmId(id));
        film.setDirectors(getDirectorsByFilmId(id));
        return film;
    }

    @Override
    public Collection<Film> getPopularFilms(int count, Integer genreId, Integer year) {

        List<Object> params = new ArrayList<>();
        StringBuilder findPopularFilms = new StringBuilder(
                "SELECT f.*, r.RATING_NAME, " +
                        "(SELECT COUNT(*) " +
                        "FROM PUBLIC.LIKES l " +
                        "WHERE ");
        if (genreId != null) {
            findPopularFilms.append("f.FILM_ID IN (SELECT FILM_ID FROM PUBLIC.FILM_GENRES WHERE GENRE_ID = ?) AND ");
            params.add(genreId);
        }
        if (year != null) {
            findPopularFilms.append("EXTRACT(YEAR FROM CAST(f.RELEASE_DATE AS DATE)) = ? AND ");
            params.add(year);
        }

        findPopularFilms.append("l.FILM_ID = f.FILM_ID) as like_count " +
                "FROM PUBLIC.FILMS f " +
                "LEFT JOIN PUBLIC.MPA_RATINGS r ON f.RATING_ID = r.RATING_ID " +
                "ORDER BY like_count DESC, f.FILM_ID DESC " +
                "LIMIT ?");
        params.add(count);

        List<Film> films = findMany(String.valueOf(findPopularFilms), params.toArray());

        Set<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());

        Map<Long, Set<Genre>> genres = genreStorage.getGenresByFilmIds(filmIds);
        Map<Long, Set<Long>> likes = likesStorage.getLikesByFilmIds(filmIds);
        Map<Long, Set<Long>> reviews = reviewStorage.getReviewsByFilmIds(filmIds);
        Map<Long, Set<Director>> directors = getDirectorsByFilmIds(filmIds);
        films.forEach(film -> {
            film.setLikes(likes.getOrDefault(film.getId(), Set.of()));
            film.setFilmGenres(genres.getOrDefault(film.getId(), Set.of()));
            film.setReviews(reviews.getOrDefault(film.getId(), Set.of()));
            film.setDirectors(directors.getOrDefault(film.getId(), Set.of()));
        });
        return films;
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

        List<Film> films = findMany(findCommonFilms, userId, friendId);
        Set<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());

        Map<Long, Set<Genre>> genres = genreStorage.getGenresByFilmIds(filmIds);
        Map<Long, Set<Long>> likes = likesStorage.getLikesByFilmIds(filmIds);
        Map<Long, Set<Long>> reviews = reviewStorage.getReviewsByFilmIds(filmIds);
        Map<Long, Set<Director>> directors = getDirectorsByFilmIds(filmIds);

        films.forEach(film -> {
            film.setLikes(likes.getOrDefault(film.getId(), Set.of()));
            film.setFilmGenres(genres.getOrDefault(film.getId(), Set.of()));
            film.setReviews(reviews.getOrDefault(film.getId(), Set.of()));
            film.setDirectors(directors.getOrDefault(film.getId(), Set.of()));
        });
        return films;
    }

    @Override
    public Collection<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }

    @Override
    public Genre getGenreById(int id) {
        return genreStorage.getGenreById(id);
    }

    @Override
    public Collection<MpaRating> getAllMpa() {
        return mpaRatingStorage.getAllMpa();
    }

    @Override
    public MpaRating getMpaById(int id) {
        return mpaRatingStorage.getMpaById(id);
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
                "SELECT f.*, r.RATING_NAME " +
                        "FROM FILMS f " +
                        "LEFT JOIN MPA_RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "JOIN LIKES l ON f.FILM_ID = l.FILM_ID " +
                        "WHERE l.USER_ID = ? " +
                        "  AND NOT EXISTS ( " +
                        "      SELECT 1 FROM LIKES l3 " +
                        "      WHERE l3.USER_ID = ? AND l3.FILM_ID = f.FILM_ID " +
                        "  ) " +
                        "ORDER BY " +
                        "  (SELECT COUNT(*) FROM LIKES ll WHERE ll.FILM_ID = f.FILM_ID) DESC, " +
                        "  f.FILM_ID ASC";

        List<Film> films = findMany(recFilmsQuery, similarUserId, userId);
        if (films.isEmpty()) {
            return List.of();
        }
        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Genre>> genres = genreStorage.getGenresByFilmIds(filmIds);
        Map<Long, Set<Long>> likes = likesStorage.getLikesByFilmIds(filmIds);
        Map<Long, Set<Long>> reviews = reviewStorage.getReviewsByFilmIds(filmIds);

        films.forEach(film -> {
            film.setFilmGenres(genres.getOrDefault(film.getId(), Set.of()));
            film.setLikes(likes.getOrDefault(film.getId(), Set.of()));
            film.setReviews(reviews.getOrDefault(film.getId(), Set.of()));
        });

        return films;
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

    private Integer validateMpa(Film film) {
        if (film.getMpaRating() == null || film.getMpaRating().getId() == null) {
            log.debug("MPA не указан, используется значение по умолчанию: 1");
            film.setMpaRating(new MpaRating(1, null));
            return 1;
        }

        Integer mpaId = film.getMpaRating().getId();

        if (mpaId < MIN_MPA_ID || mpaId > MAX_MPA_ID) {
            throw new NotFoundException(
                    String.format("ID MPA рейтинга должен быть от %d до %d. Получено: %d",
                            MIN_MPA_ID, MAX_MPA_ID, mpaId)
            );
        }
        // Проверка существования в БД
        try {
            MpaRating mpa = mpaRatingStorage.getMpaById(mpaId);
            film.setMpaRating(mpa);
            log.debug("MPA рейтинг ID {} валиден: {}", mpaId, mpa.getName());
            return mpaId;
        } catch (NotFoundException e) {
            throw new NotFoundException(
                    String.format("MPA рейтинг с id = %d не найден", mpaId)
            );
        }
    }

    private Set<Genre> validateGenres(Film film) {
        Set<Genre> result = new LinkedHashSet<>();

        if (film.getFilmGenres() == null || film.getFilmGenres().isEmpty()) {
            log.debug("Фильм не имеет жанров");
            return result;
        }

        for (Genre genre : film.getFilmGenres()) {
            if (genre == null || genre.getId() == null) {
                continue;
            }

            Integer genreId = genre.getId();

            // Проверка диапазона
            if (genreId < MIN_GENRE_ID || genreId > MAX_GENRE_ID) {
                throw new NotFoundException(
                        String.format("ID жанра должен быть от %d до %d. Получено: %d",
                                MIN_GENRE_ID, MAX_GENRE_ID, genreId)
                );
            }

            // Проверка существования в БД
            Genre dbGenre = genreStorage.getGenreById(genreId);
            // Добавляем только если еще нет (удаляем дубликаты)
            if (!result.contains(dbGenre)) {
                result.add(dbGenre);
                log.debug("Жанр ID {} валиден: {}", genreId, dbGenre.getName());
            }
        }

        return result;
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

    private Set<Director> getDirectorsByFilmId(Long filmId) {
        return new HashSet<>(jdbc.query(
                FIND_DIRECTORS_BY_FILM_ID,
                (rs, rowNum) -> new Director(
                        rs.getLong("DIRECTOR_ID"),
                        rs.getString("DIRECTOR_NAME")
                ),
                filmId
        ));
    }

    private Map<Long, Set<Director>> getDirectorsByFilmIds(Set<Long> filmIds) {
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
        log.debug("Найдено {} фильмов для режиссёра id={}", films.size(), directorId);

        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Genre>> genres = genreStorage.getGenresByFilmIds(filmIds);
        Map<Long, Set<Long>> likes = likesStorage.getLikesByFilmIds(filmIds);
        Map<Long, Set<Long>> reviews = reviewStorage.getReviewsByFilmIds(filmIds);
        Map<Long, Set<Director>> directors = getDirectorsByFilmIds(filmIds);

        films.forEach(film -> {
            film.setFilmGenres(genres.getOrDefault(film.getId(), Set.of()));
            film.setLikes(likes.getOrDefault(film.getId(), Set.of()));
            film.setReviews(reviews.getOrDefault(film.getId(), Set.of()));
            film.setDirectors(directors.getOrDefault(film.getId(), Set.of()));
        });

        return films;
    }
}
