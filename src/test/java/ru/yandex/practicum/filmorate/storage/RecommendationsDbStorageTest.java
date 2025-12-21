package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.*;
import ru.yandex.practicum.filmorate.storage.db.mapper.*;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        FilmDbStorage.class, FilmRowMapper.class,
        GenreDbStorage.class, GenreRowMapper.class,
        MpaRatingDbStorage.class, MpaRatingRowMapper.class,
        LikesDbStorage.class,
        UserDbStorage.class, UserRowMapper.class,
        FriendDbStorage.class,
        ReviewDbStorage.class, ReviewRowMapper.class,
        ReviewRatingsDbStorage.class,
        FeedDbStorage.class, FeedRowMapper.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RecommendationsDbStorageTest {

    private final FilmDbStorage filmDbStorage;
    private final LikesDbStorage likesDbStorage;
    private final UserDbStorage userDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final JdbcTemplate jdbc;

    @BeforeEach
    void setUpReferenceData() {
        jdbc.update("MERGE INTO MPA_RATINGS (RATING_ID, RATING_NAME) KEY (RATING_ID) VALUES (1, 'G')");
        jdbc.update("MERGE INTO MPA_RATINGS (RATING_ID, RATING_NAME) KEY (RATING_ID) VALUES (2, 'PG')");
        jdbc.update("MERGE INTO MPA_RATINGS (RATING_ID, RATING_NAME) KEY (RATING_ID) VALUES (3, 'PG-13')");
        jdbc.update("MERGE INTO MPA_RATINGS (RATING_ID, RATING_NAME) KEY (RATING_ID) VALUES (4, 'R')");
        jdbc.update("MERGE INTO MPA_RATINGS (RATING_ID, RATING_NAME) KEY (RATING_ID) VALUES (5, 'NC-17')");
    }

    @Test
    void shouldReturnRecommendationsFromMostSimilarUser() {
        User u1 = createUser("u1@mail.ru", "u1", "User1");
        User u2 = createUser("u2@mail.ru", "u2", "User2");

        Film film1 = new Film();
        film1.setName("Матрица");
        film1.setDescription("Хакер Нео узнает, что его мир - виртуальная реальность");
        film1.setReleaseDate(LocalDate.of(1999, 3, 31));
        film1.setDuration(136);
        film1.setMpaRating(mpaRatingDbStorage.getMpaById(2));
        Film f1 = filmDbStorage.createFilm(film1);

        Film film2 = new Film();
        film2.setName("Форрест Гамп");
        film2.setDescription("История человека с низким IQ, который стал свидетелем ключевых событий истории США");
        film2.setReleaseDate(LocalDate.of(1994, 6, 23));
        film2.setDuration(142);
        film2.setMpaRating(mpaRatingDbStorage.getMpaById(3));
        Film f2 = filmDbStorage.createFilm(film2);

        likesDbStorage.addLike(f1.getId(), u1.getId());
        likesDbStorage.addLike(f1.getId(), u2.getId());
        likesDbStorage.addLike(f2.getId(), u2.getId());
        f1.setLikes(likesDbStorage.getLikesByFilmId(f1.getId()));
        f2.setLikes(likesDbStorage.getLikesByFilmId(f2.getId()));

        Collection<Film> rec = filmDbStorage.getRecommendations(u1.getId());

        assertThat(rec)
                .isNotNull()
                .extracting(Film::getId)
                .containsExactly(f2.getId());
    }

    @Test
    void shouldReturnEmptyWhenNoSimilarUsers() {
        User u1 = createUser("u1@mail.ru", "u1", "User1");
        createUser("u2@mail.ru", "u2", "User2");

        Film film1 = new Film();
        film1.setName("Матрица");
        film1.setDescription("Хакер Нео узнает, что его мир - виртуальная реальность");
        film1.setReleaseDate(LocalDate.of(1999, 3, 31));
        film1.setDuration(136);
        film1.setMpaRating(mpaRatingDbStorage.getMpaById(2));
        Film f1 = filmDbStorage.createFilm(film1);
        likesDbStorage.addLike(f1.getId(), u1.getId());
        f1.setLikes(likesDbStorage.getLikesByFilmId(f1.getId()));

        Collection<Film> rec = filmDbStorage.getRecommendations(u1.getId());

        assertThat(rec).isNotNull().isEmpty();
    }

    @Test
    void shouldBeDeterministicWhenTieOnIntersection() {
        User u1 = createUser("u1@mail.ru", "u1", "User1");
        User u2 = createUser("u2@mail.ru", "u2", "User2");
        User u3 = createUser("u3@mail.ru", "u3", "User3");

        Film film1 = new Film();
        film1.setName("Матрица");
        film1.setDescription("Хакер Нео узнает, что его мир - виртуальная реальность");
        film1.setReleaseDate(LocalDate.of(1999, 3, 31));
        film1.setDuration(136);
        film1.setMpaRating(mpaRatingDbStorage.getMpaById(2));
        Film f1 = filmDbStorage.createFilm(film1);

        Film film2 = new Film();
        film2.setName("Форрест Гамп");
        film2.setDescription("История человека с низким IQ, который стал свидетелем ключевых событий истории США");
        film2.setReleaseDate(LocalDate.of(1994, 6, 23));
        film2.setDuration(142);
        film2.setMpaRating(mpaRatingDbStorage.getMpaById(3));
        Film f2 = filmDbStorage.createFilm(film2);

        Film film3 = new Film();
        film3.setName("Криминальное чтиво");
        film3.setDescription("Истории нескольких преступников в Лос-Анджелесе, " +
                "переплетающиеся в нелинейном повествовании");
        film3.setReleaseDate(LocalDate.of(1994, 10, 14));
        film3.setDuration(154);
        film3.setMpaRating(mpaRatingDbStorage.getMpaById(4));
        Film f3 = filmDbStorage.createFilm(film3);

        likesDbStorage.addLike(f1.getId(), u1.getId());
        likesDbStorage.addLike(f1.getId(), u2.getId());
        likesDbStorage.addLike(f2.getId(), u2.getId());
        likesDbStorage.addLike(f1.getId(), u3.getId());
        likesDbStorage.addLike(f3.getId(), u3.getId());
        f1.setLikes(likesDbStorage.getLikesByFilmId(f1.getId()));
        f2.setLikes(likesDbStorage.getLikesByFilmId(f2.getId()));
        f3.setLikes(likesDbStorage.getLikesByFilmId(f3.getId()));

        Collection<Film> rec = filmDbStorage.getRecommendations(u1.getId());

        assertThat(rec)
                .isNotNull()
                .extracting(Film::getId)
                .containsExactly(f2.getId());
    }

    private User createUser(String email, String login, String name) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userDbStorage.createUser(user);
    }
}