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
        ReviewRatingsDbStorage.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RecommendationsDbStorageTest {

    private final FilmDbStorage filmDbStorage;
    private final LikesDbStorage likesDbStorage;
    private final UserDbStorage userDbStorage;
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

        Film f1 = createFilm("Film1");
        Film f2 = createFilm("Film2");

        likesDbStorage.addLike(f1.getId(), u1.getId());
        likesDbStorage.addLike(f1.getId(), u2.getId());
        likesDbStorage.addLike(f2.getId(), u2.getId());

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

        Film f1 = createFilm("Film1");
        likesDbStorage.addLike(f1.getId(), u1.getId());

        Collection<Film> rec = filmDbStorage.getRecommendations(u1.getId());

        assertThat(rec).isNotNull().isEmpty();
    }

    @Test
    void shouldBeDeterministicWhenTieOnIntersection() {
        User u1 = createUser("u1@mail.ru", "u1", "User1");
        User u2 = createUser("u2@mail.ru", "u2", "User2");
        User u3 = createUser("u3@mail.ru", "u3", "User3");

        Film f1 = createFilm("Film1");
        Film f2 = createFilm("Film2");
        Film f3 = createFilm("Film3");

        likesDbStorage.addLike(f1.getId(), u1.getId());
        likesDbStorage.addLike(f1.getId(), u2.getId());
        likesDbStorage.addLike(f2.getId(), u2.getId());
        likesDbStorage.addLike(f1.getId(), u3.getId());
        likesDbStorage.addLike(f3.getId(), u3.getId());
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

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        film.setMpaRating(null);

        film.setFilmGenres(null);

        return filmDbStorage.createFilm(film);
    }
}