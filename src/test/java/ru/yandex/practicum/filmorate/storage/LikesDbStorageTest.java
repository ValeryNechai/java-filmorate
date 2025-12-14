package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.*;
import ru.yandex.practicum.filmorate.storage.db.mapper.*;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({GenreDbStorage.class, GenreRowMapper.class, FilmDbStorage.class, FilmRowMapper.class,
        MpaRatingRowMapper.class, MpaRatingDbStorage.class, LikesDbStorage.class, UserRowMapper.class,
        UserDbStorage.class, FriendDbStorage.class, ReviewDbStorage.class, ReviewRowMapper.class,
        FeedDbStorage.class, FeedRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LikesDbStorageTest {
    private final LikesDbStorage likesDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final UserDbStorage userDbStorage;

    private Film createdFilm1;
    private User createdUser1;

    @BeforeEach
    public void createFilmsAndUsers() {
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

        User testUser1 = new User();
        testUser1.setEmail("ivan.petrov@mail.ru");
        testUser1.setLogin("ivan_petrov");
        testUser1.setName("Иван Петров");
        testUser1.setBirthday(LocalDate.of(1990, 5, 15));

        createdUser1 = userDbStorage.createUser(testUser1);
    }

    @Test
    public void shouldAddLike() {
        likesDbStorage.addLike(createdFilm1.getId(), createdUser1.getId());

        assertThat(filmDbStorage.getFilm(createdFilm1.getId()).getLikes())
                .isNotNull()
                .hasSize(1);
    }

    @Test
    public void shouldDeleteLike() {
        likesDbStorage.addLike(createdFilm1.getId(), createdUser1.getId());
        likesDbStorage.deleteLike(createdFilm1.getId(), createdUser1.getId());

        assertThat(filmDbStorage.getFilm(createdFilm1.getId()).getLikes())
                .isNotNull()
                .hasSize(0);
    }

    @Test
    public void shouldFindLikesByFilmId() {
        likesDbStorage.addLike(createdFilm1.getId(), createdUser1.getId());
        Set<Long> likes = likesDbStorage.getLikesByFilmId(createdFilm1.getId());

        assertThat(likes)
                .isNotNull()
                .hasSize(1)
                .contains(createdUser1.getId());
    }
}
