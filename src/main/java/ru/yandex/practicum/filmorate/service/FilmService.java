package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.Collection;
import java.util.List;

public interface FilmService {

    Film createFilm(Film film);

    Film updateFilm(Film newFilm);

    Collection<Film> getAllFilms();

    Film getFilm(Long id);

    void addLike(Long id, Long userId);

    void deleteLike(Long id, Long userId);

    Collection<Film> getPopularFilms(int count, Integer genreId, Integer year);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    Collection<Genre> getAllGenres();

    Genre getGenreById(int id);

    Collection<MpaRating> getAllMpa();

    MpaRating getMpaById(int id);

    Review createReview(Review review);

    Review updateReview(Review review);

    void deleteReview(Long reviewId);

    Review getReviewById(Long reviewId);

    Collection<Review> getReviewsByFilmIdAndCount(Long filmId, int count);

    void addLikeToReview(Long reviewId, Long userId);

    void addDislikeToReview(Long reviewId, Long userId);

    void deleteLikeFromReview(Long reviewId, Long userId);

    void deleteDislikeFromReview(Long reviewId, Long userId);

    List<Film> searchFilms(String query, String by);
}
