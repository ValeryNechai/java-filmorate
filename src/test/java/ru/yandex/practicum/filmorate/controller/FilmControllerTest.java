package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
class FilmControllerTest {

    FilmController fc = new FilmController();

    @Test
    void createFilm() {
        Film film1 = new Film(1L, "", "Description", LocalDate.of(2025,03,03),
                Duration.ofMinutes(136));
        assertThrows(ValidationException.class, () -> fc.createFilm(film1), "Название не может быть пустым.");

        Film film2 = new Film(1L, "Film",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", LocalDate.of(2025,03,03),
                Duration.ofMinutes(136));
        assertThrows(ValidationException.class, () -> fc.createFilm(film2), "Превышена максимальная длина описания.");

        Film film3 = new Film(1L, "Film", "Description", LocalDate.of(1875,03,03),
                Duration.ofMinutes(136));
        assertThrows(ValidationException.class, () -> fc.createFilm(film3),
                "Дата релиза должна быть не раньше 28 декабря 1895 года.");

        Film film4 = new Film(1L, "Film", "Description", LocalDate.of(1995,03,03),
                Duration.ofMinutes(-5));
        assertThrows(ValidationException.class, () -> fc.createFilm(film4),
                "Продолжительность фильма должна быть положительным числом.");

        Film film5 = new Film(1L, "Film", "Description", LocalDate.of(1995,03,03),
                Duration.ofMinutes(0));
        assertThrows(ValidationException.class, () -> fc.createFilm(film5),
                "Продолжительность фильма должна быть положительным числом.");
    }

    @Test
    void updateFilm() {
        Film film = new Film(1L, "Film", "Description", LocalDate.of(2025,03,03),
                Duration.ofMinutes(136));
        fc.createFilm(film);

        Film film1 = new Film(film.getId(), "", "Description", LocalDate.of(2025,03,03),
                Duration.ofMinutes(136));
        assertThrows(ValidationException.class, () -> fc.updateFilm(film1), "Название не может быть пустым.");

        Film film2 = new Film(film.getId(), "Film",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", LocalDate.of(2025,03,03),
                Duration.ofMinutes(136));
        assertThrows(ValidationException.class, () -> fc.updateFilm(film2), "Превышена максимальная длина описания.");

        Film film3 = new Film(film.getId(), "Film", "Description", LocalDate.of(1875,03,03),
                Duration.ofMinutes(136));
        assertThrows(ValidationException.class, () -> fc.updateFilm(film3),
                "Дата релиза должна быть не раньше 28 декабря 1895 года.");

        Film film4 = new Film(film.getId(), "Film", "Description", LocalDate.of(1995,03,03),
                Duration.ofMinutes(-5));
        assertThrows(ValidationException.class, () -> fc.updateFilm(film4),
                "Продолжительность фильма должна быть положительным числом.");

        Film film5 = new Film(film.getId(), "Film", "Description", LocalDate.of(1995,03,03),
                Duration.ofMinutes(0));
        assertThrows(ValidationException.class, () -> fc.updateFilm(film5),
                "Продолжительность фильма должна быть положительным числом.");

    }

    @Test
    void getAllFilms() {
        Film film = new Film(1L, "Film", "Description", LocalDate.of(2025,03,03),
                Duration.ofMinutes(136));
        fc.createFilm(film);
        assertEquals(1L, fc.getAllFilms().size(), "Должен быть добавлен один фильм.");
    }
}