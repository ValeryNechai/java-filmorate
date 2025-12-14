package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.db.*;
import ru.yandex.practicum.filmorate.storage.db.mapper.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({GenreDbStorage.class, GenreRowMapper.class, FilmDbStorage.class, FilmRowMapper.class,
        MpaRatingRowMapper.class, MpaRatingDbStorage.class, LikesDbStorage.class,
        LikesDbStorage.class, ReviewDbStorage.class, ReviewRowMapper.class,
        FeedDbStorage.class, FeedRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilmDbStorageTest {
    private final LikesDbStorage likesDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;

    private Film createdFilm1;
    private Film createdFilm2;

    @BeforeEach
    public void createFilmsAndUsers() {
        Film film1 = new Film();
        film1.setName("Матрица");
        film1.setDescription("Хакер Нео узнает, что его мир - виртуальная реальность");
        film1.setReleaseDate(LocalDate.of(1999, 3, 31));
        film1.setDuration(136);
        film1.setMpaRating(mpaRatingDbStorage.getMpaById(2));
        film1.setLikes(Set.of(1L, 2L, 3L));

        Set<Genre> genres1 = new HashSet<>();
        genres1.add(genreDbStorage.getGenreById(6));
        genres1.add(genreDbStorage.getGenreById(2));
        film1.setFilmGenres(genres1);

        createdFilm1 = filmDbStorage.createFilm(film1);

        Film film2 = new Film();
        film2.setName("Форрест Гамп");
        film2.setDescription("История человека с низким IQ, который стал свидетелем ключевых событий истории США");
        film2.setReleaseDate(LocalDate.of(1994, 6, 23));
        film2.setDuration(142);
        film2.setMpaRating(mpaRatingDbStorage.getMpaById(3));
        film2.setLikes(Set.of(2L, 3L));

        Set<Genre> genres2 = new HashSet<>();
        genres2.add(genreDbStorage.getGenreById(2));
        genres2.add(genreDbStorage.getGenreById(1));
        film2.setFilmGenres(genres2);

        createdFilm2 = filmDbStorage.createFilm(film2);
    }

    @Test
    public void shouldCreateFilm() {
        Film film3 = new Film();
        film3.setName("Король Лев");
        film3.setDescription("Молодой лев Симба борется за свое право на трон после убийства отца");
        film3.setReleaseDate(LocalDate.of(1994, 6, 15));
        film3.setDuration(88);
        film3.setMpaRating(mpaRatingDbStorage.getMpaById(1));

        Set<Genre> genres3 = new HashSet<>();
        genres3.add(genreDbStorage.getGenreById(3));
        genres3.add(genreDbStorage.getGenreById(2));
        film3.setFilmGenres(genres3);

        Film createdFilm3 = filmDbStorage.createFilm(film3);

        assertThat(createdFilm3)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "Король Лев");
    }

    @Test
    public void shouldUpdateFilm() {
        createdFilm1.setName("Матрица 2.0");

        Film updatingFilm = filmDbStorage.updateFilm(createdFilm1);

        assertThat(updatingFilm)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "Матрица 2.0");
    }

    @Test
    public void shouldFindAllFilms() {
        Collection<Film> films = filmDbStorage.getAllFilms();

        assertThat(films)
                .isNotNull()
                .hasSize(2)
                .extracting(Film::getName)
                .contains("Матрица", "Форрест Гамп");
    }

    @Test
    public void shouldFindFilm() {
        Film film = filmDbStorage.getFilm(createdFilm1.getId());

        assertThat(film)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "Матрица");
    }

    @Test
    public void shouldFindPopularFilm() {
        Collection<Film> films = filmDbStorage.getPopularFilms(1, 3, 1994);

        assertThat(films)
                .isNotNull()
                .hasSize(1)
                .extracting(Film::getName)
                .contains("Форрест Гамп");
    }

    @Test
    public void shouldVerificationOfExistence() {
        boolean verification1 = filmDbStorage.existsById(createdFilm2.getId());
        boolean verification2 = filmDbStorage.existsById(88L);

        assertThat(verification1).isTrue();
        assertThat(verification2).isFalse();
    }
}
