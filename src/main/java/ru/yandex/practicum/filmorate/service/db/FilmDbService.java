package ru.yandex.practicum.filmorate.service.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.db.LikesStorage;

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
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate DECEMBER_1895 = LocalDate.of(1895,12,28);

    @Autowired
    public FilmDbService(FilmStorage filmStorage, UserStorage userStorage, LikesStorage likesStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.likesStorage = likesStorage;
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
    public Collection<Film> getPopularFilms(int count) {
        return filmStorage.getPopularFilms(count);
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
        List<Film> result = filmStorage.searchFilms(query,by);
        log.info("Найдено {} фильмов по запросу '{}'", result.size(), query);
        return result;
    }
}
