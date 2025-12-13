package ru.yandex.practicum.filmorate.storage.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
@Slf4j
public class FilmDbStorage extends AbstractDbStorage<Film> implements FilmStorage {
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;
    private final LikesStorage likesStorage;
    private static final int MIN_MPA_ID = 1;
    private static final int MAX_MPA_ID = 5;
    private static final int MIN_GENRE_ID = 1;
    private static final int MAX_GENRE_ID = 6;

    public FilmDbStorage(JdbcTemplate jdbc, RowMapper<Film> mapper, GenreStorage genreStorage,
                         MpaRatingStorage mpaRatingStorage, LikesStorage likesStorage) {
        super(jdbc, mapper);
        this.genreStorage = genreStorage;
        this.mpaRatingStorage = mpaRatingStorage;
        this.likesStorage = likesStorage;
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
        log.debug("Фильм с id {} успешно обновлен.", newFilm.getId());

        return getFilm(newFilm.getId());
    }

    @Override
    public Collection<Film> getAllFilms() {
        String findAllFilmsQuery = "SELECT f.*, r.RATING_NAME FROM FILMS AS f " +
                "LEFT OUTER JOIN MPA_RATINGS AS r ON f.RATING_ID=r.RATING_ID";

        List<Film> films = findMany(findAllFilmsQuery);
        Map<Long, Set<Genre>> genres = genreStorage.getGenresByAllFilms();
        Map<Long, Set<Long>> likes = likesStorage.getLikesByAllFilms();

        return films.stream()
                .peek(film -> film.setFilmGenres(genres.get(film.getId())))
                .peek(film -> film.setLikes(likes.get(film.getId())))
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

        films.forEach(film -> {
                    film.setLikes(likes.get(film.getId()));
                    film.setFilmGenres(genres.get(film.getId()));
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

        return count != null && count > 0;
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
}
