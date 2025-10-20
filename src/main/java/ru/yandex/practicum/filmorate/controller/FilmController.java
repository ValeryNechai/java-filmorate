package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
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
    private static final LocalDate DESEMBER_1895 = LocalDate.of(1895,12,28);

    @PostMapping
    public Film createFilm(@RequestBody Film film) {

        verificationNewFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.debug("Фильм {} успешно добавлен", film.getName());
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film newFilm) {

        if (films.containsKey(newFilm.getId())) {
            verificationUpdateFilm(newFilm);
            Film oldFilm = films.get(newFilm.getId());
            if (newFilm.getDescription() != null) {
                oldFilm.setDescription(newFilm.getDescription());
                log.debug("Описание фильма {} успешно обновлено.", oldFilm.getName());
            }
            if (newFilm.getName() != null) {
                oldFilm.setName(newFilm.getName());
                log.debug("Наименование фильма {} успешно обновлено.", oldFilm.getName());
            }
            if (newFilm.getReleaseDate() != null) {
                oldFilm.setReleaseDate(newFilm.getReleaseDate());
                log.debug("Дата выхода фильма {} успешно обновлена.", oldFilm.getName());
            }
            if (newFilm.getDuration() != null) {
                oldFilm.setDuration(newFilm.getDuration());
                log.debug("Продолжительность фильма {} успешно обновлена.", oldFilm.getName());
            }
            return oldFilm;
        }

        log.warn("Фильм с id = {} не найден", newFilm.getId());
        throw new ValidationException("Фильм с id = " + newFilm.getId() + " не найден");
    }

    @GetMapping
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    public void verificationNewFilm (Film film) {

        log.debug("Начало проверки соответствия данных фильма {} всем критериям.", film.getName());

        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Название фильма {} не соответствует требованиям.", film.getName());
            throw new ValidationException("Название не может быть пустым.");
        }

        if (film.getDescription() == null || film.getDescription().length() > 200) {
            log.warn("Описание фильма {} не соответствует требованиям.", film.getDescription());
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(DESEMBER_1895)) {
            log.warn("Дата выпуска фильма {} не соответствует требованиям.", film.getReleaseDate());
            throw new ValidationException("Дата релиза должна быть не раньше 28 декабря 1895 года.");
        }

        if (film.getDuration() == null || !film.getDuration().isPositive()) {
            log.warn("Продолжительность фильма {} не соответствует требованиям.", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }

        log.debug("Проверка данных фильма {} прошла успешно.", film.getName());
    }

    public void verificationUpdateFilm (Film film) {

        log.debug("Начало проверки соответствия данных фильма {} всем критериям.", film.getName());
        if (film.getName() != null && film.getName().isBlank()) {
            log.warn("Название фильма {} не соответствует требованиям.", film.getName());
            throw new ValidationException("Название не может быть пустым.");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Описание фильма {} не соответствует требованиям.", film.getDescription());
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        }

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(DESEMBER_1895)) {
            log.warn("Дата выпуска фильма {} не соответствует требованиям.", film.getReleaseDate());
            throw new ValidationException("Дата релиза должна быть не раньше 28 декабря 1895 года.");
        }

        if (film.getDuration() != null && !film.getDuration().isPositive()) {
            log.warn("Продолжительность фильма {} не соответствует требованиям.", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }

        log.debug("Проверка данных фильма {} прошла успешно.", film.getName());
    }

    public Long getNextId() {
        long maxId = films.keySet().stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++maxId;
    }
}
