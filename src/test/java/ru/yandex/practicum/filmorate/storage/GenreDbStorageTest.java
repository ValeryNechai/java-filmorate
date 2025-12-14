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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({GenreDbStorage.class, GenreRowMapper.class, FilmDbStorage.class, FilmRowMapper.class,
        MpaRatingRowMapper.class, MpaRatingDbStorage.class, LikesDbStorage.class,
        ReviewDbStorage.class, ReviewRowMapper.class, FeedDbStorage.class, FeedRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GenreDbStorageTest {
    private final GenreDbStorage genreDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private Film createdFilm1;
    private Film createdFilm2;

    @BeforeEach
    public void createFilms() {
        Film film1 = new Film();
        film1.setName("Матрица");
        film1.setDescription("Хакер Нео узнает, что его мир - виртуальная реальность");
        film1.setReleaseDate(LocalDate.of(1999, 3, 31));
        film1.setDuration(136);
        film1.setMpaRating(mpaRatingDbStorage.getMpaById(2));

        Set<Genre> genres1 = new HashSet<>();
        genres1.add(genreDbStorage.getGenreById(6));
        genres1.add(genreDbStorage.getGenreById(2));
        film1.setFilmGenres(genres1);

        createdFilm1 = filmDbStorage.createFilm(film1);

        Film film2 = new Film();
        film2.setName("Звездные войны: Империя наносит ответный удар");
        film2.setDescription("Повстанцы скрываются на ледяной планете Хот, " +
                "пока Дарт Вейдер охотится за Люком Скайуокером");
        film2.setReleaseDate(LocalDate.of(1980, 5, 21));
        film2.setDuration(124);
        film2.setMpaRating(mpaRatingDbStorage.getMpaById(2));

        Set<Genre> genres2 = new HashSet<>();
        genres2.add(genreDbStorage.getGenreById(6));
        genres2.add(genreDbStorage.getGenreById(2));
        film2.setFilmGenres(genres2);

        createdFilm2 = filmDbStorage.createFilm(film2);
    }

    @Test
    public void shouldFindGenreById() {
        Genre genre = genreDbStorage.getGenreById(1);

        assertThat(genre)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", 1)
                .hasFieldOrPropertyWithValue("name", "Комедия");
    }

    @Test
    public void shouldFindAllGenre() {
        Collection<Genre> genres = genreDbStorage.getAllGenres();

        assertThat(genres).isNotNull()
                .hasSize(6)
                .extracting(Genre::getName)
                .contains("Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик");
    }

    @Test
    public void shouldFindGenreByFilmId() {
        Set<Genre> genresByFilm = genreDbStorage.getGenresByFilmId(createdFilm1.getId());

        assertThat(genresByFilm)
                .hasSize(2)
                .extracting(Genre::getId)
                .contains(6, 2);
    }

    @Test
    public void shouldFindGenresByAllFilms() {
        Map<Long, Set<Genre>> genres = genreDbStorage.getGenresByAllFilms();

        assertThat(genres)
                .isNotEmpty()
                .hasSize(2)
                .containsEntry(createdFilm1.getId(), createdFilm1.getFilmGenres())
                .containsEntry(createdFilm2.getId(), createdFilm2.getFilmGenres());
    }
}
