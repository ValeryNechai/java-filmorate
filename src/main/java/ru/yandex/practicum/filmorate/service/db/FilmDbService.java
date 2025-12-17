package ru.yandex.practicum.filmorate.service.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.db.LikesStorage;
import ru.yandex.practicum.filmorate.storage.db.ReviewRatingsStorage;
import ru.yandex.practicum.filmorate.storage.db.ReviewStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Service
@Primary
@Slf4j
public class FilmDbService implements FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final LikesStorage likesStorage;
    private final ReviewStorage reviewStorage;
    private final ReviewRatingsStorage reviewRatingsStorage;
    private final DirectorStorage directorStorage;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate DECEMBER_1895 = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmDbService(FilmStorage filmStorage, UserStorage userStorage,
                         LikesStorage likesStorage, ReviewStorage reviewStorage,
                         ReviewRatingsStorage reviewRatingsStorage, DirectorStorage directorStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.likesStorage = likesStorage;
        this.reviewStorage = reviewStorage;
        this.reviewRatingsStorage = reviewRatingsStorage;
        this.directorStorage = directorStorage;
    }

    @Override
    public Film createFilm(Film film) {
        validateFilm(film);
        return filmStorage.createFilm(film);
    }

    @Override
    public Film updateFilm(Film newFilm) {
        if (!filmStorage.existsById(newFilm.getId())) {
            throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден.");
        }
        validateFilm(newFilm);

        return filmStorage.updateFilm(newFilm);
    }

    @Override
    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    @Override
    public Film getFilm(Long id) {
        return filmStorage.getFilm(id);
    }

    @Override
    public void addLike(Long id, Long userId) {
        Film film = filmStorage.getFilm(id);
        validateLike(id, userId);
        if (film.getLikes().contains(userId)) {
            log.warn("Попытка повторно поставить лайк фильму");
            throw new ValidationException("Фильм можно лайкнуть только один раз!");
        }

        likesStorage.addLike(id, userId);
    }

    @Override
    public void deleteLike(Long id, Long userId) {
        Film film = filmStorage.getFilm(id);
        validateLike(id, userId);
        if (!film.getLikes().contains(userId)) {
            log.warn("Попытка удалить несуществующий лайк. Ранее лайк этому фильму не был поставлен");
            throw new ValidationException("Нельзя удалить несуществующий лайк. " +
                    "Ранее лайк этому фильму не был поставлен");
        }

        likesStorage.deleteLike(id, userId);
    }

    @Override
    public Collection<Film> getPopularFilms(int count, Integer genreId, Integer year) {
        return filmStorage.getPopularFilms(count, genreId, year);
    }

    @Override
    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        return filmStorage.getCommonFilms(userId, friendId);
    }

    @Override
    public Collection<Genre> getAllGenres() {
        return filmStorage.getAllGenres();
    }

    @Override
    public Genre getGenreById(int id) {
        return filmStorage.getGenreById(id);
    }

    @Override
    public Collection<MpaRating> getAllMpa() {
        return filmStorage.getAllMpa();
    }

    @Override
    public MpaRating getMpaById(int id) {
        return filmStorage.getMpaById(id);
    }

    @Override
    public Review createReview(Review review) {
        validateReview(review);
        return reviewStorage.createReview(review);
    }

    @Override
    public Review updateReview(Review newReview) {
        if (!reviewStorage.existsById(newReview.getReviewId())) {
            throw new NotFoundException("Отзыв с id = " + newReview.getReviewId() + " не найден.");
        }
        validateReview(newReview);
        return reviewStorage.updateReview(newReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        if (!reviewStorage.existsById(reviewId)) {
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден.");
        }
        validateReview(reviewStorage.getReviewById(reviewId));
        reviewStorage.deleteReview(reviewId);
    }

    @Override
    public Review getReviewById(Long reviewId) {
        return reviewStorage.getReviewById(reviewId);
    }

    @Override
    public Collection<Review> getReviewsByFilmIdAndCount(Long filmId, int count) {
        if (filmId != null) {
            if (!filmStorage.existsById(filmId)) {
                throw new NotFoundException("Фильм с id = " + filmId + " не найден.");
            }
            return reviewStorage.getReviewsByFilmIdAndCount(filmId, count);
        }
        return reviewStorage.getReviewsByAllFilmsAndCount(count);
    }

    @Override
    public void addLikeToReview(Long reviewId, Long userId) {
        validateReviewReaction(reviewId, userId);
        reviewRatingsStorage.addLikeToReview(reviewId, userId);
    }

    @Override
    public void addDislikeToReview(Long reviewId, Long userId) {
        validateReviewReaction(reviewId, userId);
        reviewRatingsStorage.addDislikeToReview(reviewId, userId);
    }

    @Override
    public void deleteLikeFromReview(Long reviewId, Long userId) {
        validateReviewReaction(reviewId, userId);
        reviewRatingsStorage.deleteLikeFromReview(reviewId, userId);
    }

    @Override
    public void deleteDislikeFromReview(Long reviewId, Long userId) {
        validateReviewReaction(reviewId, userId);
        reviewRatingsStorage.deleteDislikeFromReview(reviewId, userId);
    }

    private void validateFilm(Film film) {
        log.debug("Начало проверки соответствия данных фильма {} всем критериям.", film.getName());

        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Название фильма {} не соответствует требованиям.", film.getName());
            throw new ValidationException("Название не может быть пустым.");
        }
        if (film.getDescription() == null || film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("Описание фильма {} не соответствует требованиям.", film.getDescription());
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(DECEMBER_1895)) {
            log.warn("Дата выпуска фильма {} не соответствует требованиям.", film.getReleaseDate());
            throw new ValidationException("Дата релиза должна быть не раньше 28 декабря 1895 года.");
        }
        if (film.getDuration() == null || film.getDuration() < 0) {
            log.warn("Продолжительность фильма {} не соответствует требованиям.", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }

        log.debug("Проверка данных фильма {} прошла успешно.", film.getName());
    }

    private void validateLike(Long id, Long userId) {
        Film film = filmStorage.getFilm(id);
        User user = userStorage.getUser(userId);
        if (film == null) {
            log.warn("Фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        } else if (user == null) {
            log.warn("Пользователь с userId = {} не найден", userId);
            throw new NotFoundException("Пользователь с userId = " + userId + " не найден");
        }
    }

    private void validateReview(Review review) {
        if (review == null) {
            log.warn("Отзыв не найден");
            throw new NotFoundException("Отзыв не найден");
        }
        if (review.getFilmId() == null) {
            throw new ValidationException("ID фильма не может быть null");
        }
        if (review.getUserId() == null) {
            throw new ValidationException("ID пользователя не может быть null");
        }
        try {
            filmStorage.getFilm(review.getFilmId());
        } catch (NotFoundException e) {
            log.warn("Фильм с id = {} не найден", review.getFilmId());
            throw new NotFoundException("Фильм с id = " + review.getFilmId() + " не найден");
        }
        try {
            userStorage.getUser(review.getUserId());
        } catch (NotFoundException e) {
            log.warn("Пользователь с id = {} не найден", review.getUserId());
            throw new NotFoundException("Пользователь с id = " + review.getUserId() + " не найден");
        }
        if (review.getContent() == null) {
            log.warn("Содержание отзыва не может быть пустым.");
            throw new ValidationException("Содержание отзыва не может быть пустым.");
        }
        if (review.getIsPositive() == null) {
            log.warn("Тип отзыва не может быть пустым.");
            throw new ValidationException("Тип отзыва не может быть пустым.");
        }
    }

    private void validateReviewReaction(Long reviewId, Long userId) {
        Review review = reviewStorage.getReviewById(reviewId);
        User user = userStorage.getUser(userId);
        if (review == null) {
            log.warn("Отзыв с id = {} не найден", reviewId);
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        } else if (user == null) {
            log.warn("Пользователь с userId = {} не найден", userId);
            throw new NotFoundException("Пользователь с userId = " + userId + " не найден");
        }
    }

    @Override
    public List<Film> searchFilms(String query, String by) {
        log.debug("Поиск фильмов по запросу: '{}'", query);

        if (query == null || query.trim().isEmpty()) {
            log.warn("Попытка поиска с пустым запросом");
            throw new ValidationException("Параметр поиска 'query' не может быть пустым");
        }
        query = query.trim();
        if (query.length() < 2) {
            log.warn("Слишком короткий запрос для поиска: '{}'", query);
            throw new ValidationException("Запрос для поиска должен содержать не менее 2 символов");
        }

        log.info("Выполнение поиска фильмов по запросу: '{}'", query);
        List<Film> result = filmStorage.searchFilms(query, by);
        log.info("Найдено {} фильмов по запросу '{}'", result.size(), query);
        return result;
      }
  
    public Collection<Film> getFilmsByDirector(Long directorId, String sortBy) {

        directorStorage.getDirectorById(directorId);

        if (sortBy == null || (!sortBy.equals("year") && !sortBy.equals("likes"))) {
            throw new ValidationException("sortBy должен быть 'year' или 'likes'");
        }
        return filmStorage.getFilmsByDirector(directorId, sortBy);
    }
}

