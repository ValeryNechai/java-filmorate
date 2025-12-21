package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.*;
import ru.yandex.practicum.filmorate.storage.db.mapper.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FilmDbStorage.class, FilmRowMapper.class, DirectorDbStorage.class, DirectorRowMapper.class,
        MpaRatingDbStorage.class, MpaRatingRowMapper.class, GenreDbStorage.class, GenreRowMapper.class,
        LikesDbStorage.class, FeedDbStorage.class, FeedRowMapper.class, ReviewDbStorage.class, ReviewRowMapper.class,
        UserDbStorage.class, UserRowMapper.class, FriendDbStorage.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilmByDirectorDbStorageTest {

    private final FilmDbStorage filmDbStorage;
    private final DirectorDbStorage directorDbStorage;
    private final UserDbStorage userDbStorage;
    private final LikesDbStorage likesDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;

    private Director director;
    private Film film1999;
    private Film film2003;

    @BeforeEach
    void setUp() {
        director = directorDbStorage.createDirector(
                new Director(null, "Лана Вачовски")
        );

        film1999 = new Film();
        film1999.setName("Матрица");
        film1999.setDescription("Sci-fi");
        film1999.setReleaseDate(LocalDate.of(1999, 3, 31));
        film1999.setDuration(136);
        film1999.setMpaRating(mpaRatingDbStorage.getMpaById(2));
        film1999.setDirectors(Set.of(director));

        film1999 = filmDbStorage.createFilm(film1999);

        film2003 = new Film();
        film2003.setName("Матрица: Перезагрузка");
        film2003.setDescription("Sci-fi");
        film2003.setReleaseDate(LocalDate.of(2003, 5, 15));
        film2003.setDuration(138);
        film2003.setMpaRating(mpaRatingDbStorage.getMpaById(2));
        film2003.setDirectors(Set.of(director));

        film2003 = filmDbStorage.createFilm(film2003);
    }

    @Test
    void shouldReturnFilmsSortedByYear() {
        Collection<Film> films =
                filmDbStorage.getFilmsByDirector(director.getId(), "year");

        assertThat(films)
                .hasSize(2)
                .extracting(Film::getName)
                .containsExactly(
                        "Матрица",
                        "Матрица: Перезагрузка"
                );
    }

    @Test
    void shouldReturnFilmsSortedByLikes() {
        // пользователь для лайков
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        user = userDbStorage.createUser(user);

        likesDbStorage.addLike(film2003.getId(), user.getId());

        Collection<Film> films =
                filmDbStorage.getFilmsByDirector(director.getId(), "likes");

        assertThat(films)
                .hasSize(2)
                .extracting(Film::getName)
                .containsExactly(
                        "Матрица: Перезагрузка",
                        "Матрица"
                );
    }
}
