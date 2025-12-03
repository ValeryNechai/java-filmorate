package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.db.LikesDbStorage;
import ru.yandex.practicum.filmorate.storage.db.MpaRatingDbStorage;
import ru.yandex.practicum.filmorate.storage.db.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.db.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.db.mapper.MpaRatingRowMapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({GenreDbStorage.class, GenreRowMapper.class, FilmDbStorage.class, FilmRowMapper.class,
        MpaRatingRowMapper.class, MpaRatingDbStorage.class, LikesDbStorage.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GenreDbStorageTest {
    private final GenreDbStorage genreDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;

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

        Film createdFilm1 = filmDbStorage.createFilm(film1);

        Set<Genre> genresByFilm = genreDbStorage.getGenresByFilmId(createdFilm1.getId());

        assertThat(genresByFilm)
                .hasSize(2)
                .extracting(Genre::getId)
                .contains(6, 2);
    }
}
