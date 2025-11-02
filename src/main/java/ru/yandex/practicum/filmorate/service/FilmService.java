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

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void addLike(Long id, Long userId) {
        Film film = filmStorage.getFilm(id);
        User user = userStorage.getUser(userId);
        if (film != null && user != null) {
            if (film.getLikes().contains(userId)) {
                log.warn("Попытка повторно поставить лайк фильму");
                throw new ValidationException("Фильм можно лайкнуть только один раз!");
            }

            film.getLikes().add(userId);
        } else if (film == null) {
            log.warn("Фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        } else {
            log.warn("Пользователь с userId = {} не найден", userId);
            throw new NotFoundException("Пользователь с userId = " + userId + " не найден");
        }
    }

    public void deleteLike(Long id, Long userId) {
        Film film = filmStorage.getFilm(id);
        User user = userStorage.getUser(userId);
        if (film != null && user != null) {
            if (!film.getLikes().contains(userId)) {
                log.warn("Попытка удалить несуществующий лайк. Ранее лайк этому фильму не был поставлен");
                throw new ValidationException("Нельзя удалить несуществующий лайк. " +
                        "Ранее лайк этому фильму не был поставлен");
            }

            film.getLikes().remove(userId);
        } else if (film == null) {
            log.warn("Фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        } else {
            log.warn("Пользователь с userId = {} не найден", userId);
            throw new NotFoundException("Пользователь с userId = " + userId + " не найден");
        }
    }

    public Collection<Film> getPopularFilms(int count) {
        return filmStorage.getAllFilms()
                .stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }
}
