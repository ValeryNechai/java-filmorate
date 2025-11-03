package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate DECEMBER_1895 = LocalDate.of(1895,12,28);

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film createFilm(Film film) {
        validateFilm(film);
        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film newFilm) {
        validateFilm(newFilm);
        return filmStorage.updateFilm(newFilm);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film getFilm(Long id) {
        return filmStorage.getFilm(id);
    }

    public void addLike(Long id, Long userId) {
        Film film = filmStorage.getFilm(id);
        validateLike(id, userId);
        if (film.getLikes().contains(userId)) {
            log.warn("Попытка повторно поставить лайк фильму");
            throw new ValidationException("Фильм можно лайкнуть только один раз!");
        }

        film.getLikes().add(userId);
    }

    public void deleteLike(Long id, Long userId) {
        Film film = filmStorage.getFilm(id);
        validateLike(id, userId);
        if (!film.getLikes().contains(userId)) {
            log.warn("Попытка удалить несуществующий лайк. Ранее лайк этому фильму не был поставлен");
            throw new ValidationException("Нельзя удалить несуществующий лайк. " +
                    "Ранее лайк этому фильму не был поставлен");
            }

        film.getLikes().remove(userId);
    }

    public Collection<Film> getPopularFilms(int count) {
        return filmStorage.getAllFilms()
                .stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public void validateFilm(Film film) {
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
        if (film.getDuration() == null || !film.getDuration().isPositive()) {
            log.warn("Продолжительность фильма {} не соответствует требованиям.", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }

        log.debug("Проверка данных фильма {} прошла успешно.", film.getName());
    }

    public void validateLike(Long id, Long userId) {
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
}
