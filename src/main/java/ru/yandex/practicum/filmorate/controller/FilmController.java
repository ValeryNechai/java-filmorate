package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();
    private static long id = 0;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate DEСEMBER_1895 = LocalDate.of(1895,12,28);
    //ахах, вот блин, тест на внимательность пройден))

    @PostMapping
    public Film createFilm(@RequestBody Film film) {
        validateFilm(film);
        film.setId(++id);
        films.put(film.getId(), film);
        log.debug("Фильм {} успешно добавлен", film.getName());
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film newFilm) {
        if (films.containsKey(newFilm.getId())) {
            validateFilm(newFilm);
            films.put(newFilm.getId(), newFilm);
            log.debug("Фильм с id {} успешно обновлен.", newFilm.getId());
            return newFilm;
        }

        log.warn("Фильм с id = {} не найден", newFilm.getId());
        throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
    }

    @GetMapping
    public Collection<Film> getAllFilms() {
        return films.values();
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
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(DEСEMBER_1895)) {
            log.warn("Дата выпуска фильма {} не соответствует требованиям.", film.getReleaseDate());
            throw new ValidationException("Дата релиза должна быть не раньше 28 декабря 1895 года.");
        }
        if (film.getDuration() == null || !film.getDuration().isPositive()) {
            log.warn("Продолжительность фильма {} не соответствует требованиям.", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }

        log.debug("Проверка данных фильма {} прошла успешно.", film.getName());
    }
}
