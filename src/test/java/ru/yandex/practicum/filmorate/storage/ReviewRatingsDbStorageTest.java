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
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.*;
import ru.yandex.practicum.filmorate.storage.db.mapper.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GenreDbStorage.class, GenreRowMapper.class, FilmDbStorage.class, FilmRowMapper.class,
        MpaRatingRowMapper.class, MpaRatingDbStorage.class, LikesDbStorage.class, UserRowMapper.class,
        UserDbStorage.class, FriendDbStorage.class, ReviewDbStorage.class, ReviewRowMapper.class,
        ReviewRatingsDbStorage.class, FeedDbStorage.class, FeedRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ReviewRatingsDbStorageTest {
    private final JdbcTemplate jdbcTemplate;
    private final LikesDbStorage likesDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final UserDbStorage userDbStorage;
    private final ReviewDbStorage reviewDbStorage;
    private final ReviewRatingsDbStorage reviewRatingsDbStorage;

    private Film createdFilm1;
    private User createdUser1;
    private Review createdReview1;

    @BeforeEach
    public void createReviews() {
        jdbcTemplate.execute("DELETE FROM REVIEW_RATINGS");
        jdbcTemplate.execute("DELETE FROM REVIEWS");
        jdbcTemplate.execute("DELETE FROM LIKES");
        jdbcTemplate.execute("DELETE FROM FILM_GENRES");
        jdbcTemplate.execute("DELETE FROM FRIENDSHIPS");
        jdbcTemplate.execute("DELETE FROM FEEDS");
        jdbcTemplate.execute("DELETE FROM FILMS");
        jdbcTemplate.execute("DELETE FROM USERS");

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

        Review testReview1 = new Review();
        testReview1.setContent("Отличный фильм! Особенно понравились спецэффекты и философская составляющая.");
        testReview1.setIsPositive(true);
        testReview1.setUserId(createdUser1.getId());
        testReview1.setFilmId(createdFilm1.getId());
        testReview1.setUseful(10);

        createdReview1 = reviewDbStorage.createReview(testReview1);
    }

    @Test
    public void shouldAddLikeToReview() {
        reviewRatingsDbStorage.addLikeToReview(createdReview1.getReviewId(), createdUser1.getId());

        assertThat(reviewDbStorage.getReviewById(createdReview1.getReviewId()))
                .isNotNull()
                .hasFieldOrPropertyWithValue("useful", 1);
    }

    @Test
    public void shouldDeleteLikeFromReview() {
        reviewRatingsDbStorage.addLikeToReview(createdReview1.getReviewId(), createdUser1.getId());
        reviewRatingsDbStorage.deleteLikeFromReview(createdReview1.getReviewId(), createdUser1.getId());

        assertThat(reviewDbStorage.getReviewById(createdReview1.getReviewId()))
                .isNotNull()
                .hasFieldOrPropertyWithValue("useful", 0);
    }

    @Test
    public void shouldAddDislikeToReview() {
        reviewRatingsDbStorage.addDislikeToReview(createdReview1.getReviewId(), createdUser1.getId());

        assertThat(reviewDbStorage.getReviewById(createdReview1.getReviewId()))
                .isNotNull()
                .hasFieldOrPropertyWithValue("useful", -1);
    }

    @Test
    public void shouldDeleteDislikeFromReview() {
        reviewRatingsDbStorage.addDislikeToReview(createdReview1.getReviewId(), createdUser1.getId());
        reviewRatingsDbStorage.deleteDislikeFromReview(createdReview1.getReviewId(), createdUser1.getId());

        assertThat(reviewDbStorage.getReviewById(createdReview1.getReviewId()))
                .isNotNull()
                .hasFieldOrPropertyWithValue("useful", 0);
    }

    @AfterEach
    public void clean() {
        jdbcTemplate.execute("DELETE FROM REVIEW_RATINGS");
        jdbcTemplate.execute("DELETE FROM REVIEWS");
        jdbcTemplate.execute("DELETE FROM LIKES");
        jdbcTemplate.execute("DELETE FROM FILM_GENRES");
        jdbcTemplate.execute("DELETE FROM FRIENDSHIPS");
        jdbcTemplate.execute("DELETE FROM FEEDS");
        jdbcTemplate.execute("DELETE FROM FILMS");
        jdbcTemplate.execute("DELETE FROM USERS");
    }
}