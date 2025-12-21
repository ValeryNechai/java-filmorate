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
import ru.yandex.practicum.filmorate.storage.db.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    private final FeedStorage feedStorage;
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate DECEMBER_1895 = LocalDate.of(1895, 12, 28);
    private static final int MIN_LENGTH_FOR_SEARCH = 2;
    private static final int MIN_MPA_ID = 1;
    private static final int MAX_MPA_ID = 5;
    private static final int MIN_GENRE_ID = 1;
    private static final int MAX_GENRE_ID = 6;

    @Autowired
    public FilmDbService(FilmStorage filmStorage, UserStorage userStorage, FeedStorage feedStorage,
                         LikesStorage likesStorage, ReviewStorage reviewStorage, GenreStorage genreStorage,
                         ReviewRatingsStorage reviewRatingsStorage, DirectorStorage directorStorage,
                         MpaRatingStorage mpaRatingStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.feedStorage = feedStorage;
        this.likesStorage = likesStorage;
        this.reviewStorage = reviewStorage;
        this.genreStorage = genreStorage;
        this.reviewRatingsStorage = reviewRatingsStorage;
        this.directorStorage = directorStorage;
        this.mpaRatingStorage = mpaRatingStorage;
    }

    @Override
    public Film createFilm(Film film) {
        validateFilm(film);
        Integer mpaId = validateMpa(film);
        film.setMpaRating(mpaRatingStorage.getMpaById(mpaId));
        Set<Genre> preparedGenres = validateGenres(film);
        film.setFilmGenres(new HashSet<>(preparedGenres));

        return filmStorage.createFilm(film);
    }

    @Override
    public Film updateFilm(Film newFilm) {
        if (!filmStorage.existsById(newFilm.getId())) {
            throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден.");
        }
        validateFilm(newFilm);
        Integer mpaId = validateMpa(newFilm);
        newFilm.setMpaRating(mpaRatingStorage.getMpaById(mpaId));
        Set<Genre> preparedGenres = validateGenres(newFilm);
        newFilm.setFilmGenres(new HashSet<>(preparedGenres));

        return filmStorage.updateFilm(newFilm);
    }

    @Override
    public void deleteFilm(Long filmId) {
        if (!filmStorage.existsById(filmId)) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден.");
        }
        filmStorage.deleteFilmById(filmId);
    }

    @Override
    public Collection<Film> getAllFilms() {
        Collection<Film> films = filmStorage.getAllFilms();
        enrichFilms(films);
        return films;
    }

    @Override
    public Film getFilm(Long id) {
        Film film = filmStorage.getFilm(id);
        if (film.getMpaRating() != null && film.getMpaRating().getId() != null) {
            MpaRating fullMpa = mpaRatingStorage.getMpaById(film.getMpaRating().getId());
            film.setMpaRating(fullMpa);
        }

        film.setFilmGenres(new LinkedHashSet<>(genreStorage.getGenresByFilmId(id)));
        film.setLikes(likesStorage.getLikesByFilmId(id));
        film.setReviews(reviewStorage.getReviewsByFilmId(id));
        film.setDirectors(filmStorage.getDirectorsByFilmId(id));
        return film;
    }

    @Override
    public void addLike(Long id, Long userId) {
        validateLike(id, userId);
        likesStorage.addLike(id, userId);
        feedStorage.createFeed(userId, EventType.LIKE, Operation.ADD, id);
    }

    @Override
    public void deleteLike(Long id, Long userId) {
        validateLike(id, userId);
        likesStorage.deleteLike(id, userId);
        feedStorage.createFeed(userId, EventType.LIKE, Operation.REMOVE, id);
    }

    @Override
    public Collection<Film> getPopularFilms(int count, Integer genreId, Integer year) {
        Collection<Film> popularFilms = filmStorage.getPopularFilms(count, genreId, year);
        enrichFilms(popularFilms);
        return popularFilms;
    }

    @Override
    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        Collection<Film> commonFilms = filmStorage.getCommonFilms(userId, friendId);
        enrichFilms(commonFilms);
        return commonFilms;
    }

    @Override
    public Collection<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }

    @Override
    public Genre getGenreById(int id) {
        return genreStorage.getGenreById(id);
    }

    @Override
    public Collection<MpaRating> getAllMpa() {
        return mpaRatingStorage.getAllMpa();
    }

    @Override
    public MpaRating getMpaById(int id) {
        return mpaRatingStorage.getMpaById(id);
    }

    @Override
    public Review createReview(Review review) {
        validateReview(review);
        Review newReview = reviewStorage.createReview(review);
        feedStorage.createFeed(review.getUserId(), EventType.REVIEW, Operation.ADD, review.getReviewId());
        return newReview;
    }

    @Override
    public Review updateReview(Review newReview) {
        if (!reviewStorage.existsById(newReview.getReviewId())) {
            throw new NotFoundException("Отзыв с id = " + newReview.getReviewId() + " не найден.");
        }
        validateReview(newReview);
        Review updateReview = reviewStorage.updateReview(newReview);
        feedStorage.createFeed(
                getReviewById(newReview.getReviewId()).getUserId(),
                EventType.REVIEW,
                Operation.UPDATE,
                newReview.getReviewId()
        );
        return updateReview;
    }

    @Override
    public void deleteReview(Long reviewId) {
        if (!reviewStorage.existsById(reviewId)) {
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден.");
        }
        Review review = getReviewById(reviewId);
        feedStorage.createFeed(review.getUserId(), EventType.REVIEW, Operation.REMOVE, review.getReviewId());

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
        try {
            filmStorage.getFilm(id);
        } catch (NotFoundException e) {
            log.warn("Фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }

        try {
            userStorage.getUser(userId);
        } catch (NotFoundException e) {
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
        if (query.length() < MIN_LENGTH_FOR_SEARCH) {
            log.warn("Слишком короткий запрос для поиска: '{}'", query);
            throw new ValidationException("Запрос для поиска должен содержать не менее 2 символов");
        }

        String trimmedBy = (by == null || by.trim().isEmpty()) ? "title" : by.trim().toLowerCase();

        if (!trimmedBy.equals("title") &&
                !trimmedBy.equals("director") &&
                !trimmedBy.equals("title,director") &&
                !trimmedBy.equals("director,title")) {
            throw new ValidationException("Параметр 'by' может принимать значения: title, director, title,director");
        }

        log.info("Выполнение поиска фильмов по запросу: '{}'", query);
        List<Film> result = filmStorage.searchFilms(query, by);
        enrichFilms(result);
        log.info("Найдено {} фильмов по запросу '{}'", result.size(), query);

        return result;
    }

    public Collection<Film> getFilmsByDirector(Long directorId, String sortBy) {

        directorStorage.getDirectorById(directorId);

        if (sortBy == null || (!sortBy.equals("year") && !sortBy.equals("likes"))) {
            throw new ValidationException("sortBy должен быть 'year' или 'likes'");
        }
        Collection<Film> films = filmStorage.getFilmsByDirector(directorId, sortBy);
        enrichFilms(films);

        return films;
    }

    private void enrichFilms(Collection<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Genre>> genres = genreStorage.getGenresByAllFilms();
        Map<Long, Set<Long>> likes = likesStorage.getLikesByAllFilms();
        Map<Long, Set<Long>> reviews = reviewStorage.getReviewsByAllFilms();
        Map<Long, Set<Director>> directors = filmStorage.getDirectorsByFilmIds(filmIds);
        films.stream()
                .peek(film -> film.setFilmGenres(genres.getOrDefault(film.getId(), Set.of())))
                .peek(film -> film.setLikes(likes.getOrDefault(film.getId(), Set.of())))
                .peek(film -> film.setReviews(reviews.getOrDefault(film.getId(), Set.of())))
                .peek(film -> film.setDirectors(directors.getOrDefault(film.getId(), Set.of())))
                .collect(Collectors.toList());
    }

    private Integer validateMpa(Film film) {
        if (film.getMpaRating() == null || film.getMpaRating().getId() == null) {
            log.debug("MPA не указан, используется значение по умолчанию: 1");
            film.setMpaRating(new MpaRating(1, null));
            return 1;
        }

        Integer mpaId = film.getMpaRating().getId();

        if (mpaId < MIN_MPA_ID || mpaId > MAX_MPA_ID) {
            throw new NotFoundException(
                    String.format("ID MPA рейтинга должен быть от %d до %d. Получено: %d",
                            MIN_MPA_ID, MAX_MPA_ID, mpaId)
            );
        }
        // Проверка существования в БД
        try {
            MpaRating mpa = mpaRatingStorage.getMpaById(mpaId);
            film.setMpaRating(mpa);
            log.debug("MPA рейтинг ID {} валиден: {}", mpaId, mpa.getName());
            return mpaId;
        } catch (NotFoundException e) {
            throw new NotFoundException(
                    String.format("MPA рейтинг с id = %d не найден", mpaId)
            );
        }
    }

    private Set<Genre> validateGenres(Film film) {
        Set<Genre> result = new LinkedHashSet<>();

        if (film.getFilmGenres() == null || film.getFilmGenres().isEmpty()) {
            log.debug("Фильм не имеет жанров");
            return result;
        }

        for (Genre genre : film.getFilmGenres()) {
            if (genre == null || genre.getId() == null) {
                continue;
            }

            Integer genreId = genre.getId();

            // Проверка диапазона
            if (genreId < MIN_GENRE_ID || genreId > MAX_GENRE_ID) {
                throw new NotFoundException(
                        String.format("ID жанра должен быть от %d до %d. Получено: %d",
                                MIN_GENRE_ID, MAX_GENRE_ID, genreId)
                );
            }

            // Проверка существования в БД
            Genre dbGenre = genreStorage.getGenreById(genreId);
            // Добавляем только если еще нет (удаляем дубликаты)
            if (!result.contains(dbGenre)) {
                result.add(dbGenre);
                log.debug("Жанр ID {} валиден: {}", genreId, dbGenre.getName());
            }
        }

        return result.stream()
                .sorted(Comparator.comparing(Genre::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}