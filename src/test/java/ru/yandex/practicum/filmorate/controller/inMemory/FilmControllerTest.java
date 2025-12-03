package ru.yandex.practicum.filmorate.controller.inMemory;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.inMemory.InMemoryFilmService;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.inMemory.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.inMemory.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
class FilmControllerTest {
    FilmStorage filmStorage = new InMemoryFilmStorage();
    UserStorage userStorage = new InMemoryUserStorage();
    InMemoryFilmService filmService = new InMemoryFilmService(filmStorage, userStorage);
    FilmController fc = new FilmController(filmService);

    @Test
    void shouldThrowException_WhenNameIsEmpty() {
        Film film1 = new Film(1L, "", "Description", LocalDate.of(2025, 03, 03), 136, null,
                null, null);
        assertThrows(ValidationException.class, () -> fc.createFilm(film1), "Название не может быть пустым.");
    }

    @Test
    void shouldThrowException_WhenDescriptionIsLongerThenMaxLength() {
        Film film2 = new Film(1L, "Film",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", LocalDate.of(2025, 03, 03), 136, null,
                null, null);
        assertThrows(ValidationException.class, () -> fc.createFilm(film2), "Превышена максимальная длина описания.");
    }

    @Test
    void shouldThrowException_WhenReleaseDateIsEarlierThanMoviesBirthday() {
        Film film3 = new Film(1L, "Film", "Description", LocalDate.of(1875, 03, 03),
                136, null, null, null);
        assertThrows(ValidationException.class, () -> fc.createFilm(film3),
                "Дата релиза должна быть не раньше 28 декабря 1895 года.");
    }

    @Test
    void shouldThrowException_WhenDurationIsNegative() {
        Film film4 = new Film(1L, "Film", "Description", LocalDate.of(1995, 03, 03),
                -5, null, null, null);
        assertThrows(ValidationException.class, () -> fc.createFilm(film4),
                "Продолжительность фильма должна быть положительным числом.");
    }

    @Test
    void shouldThrowException_WhenDurationIs0() {
        Film film5 = new Film(1L, "Film", "Description", LocalDate.of(1995,03,03),
                0, null, null, null);
        assertThrows(ValidationException.class, () -> fc.createFilm(film5),
                "Продолжительность фильма должна быть положительным числом.");
    }

    @Test
    void shouldGetAllFilms() {
        Film film = new Film(1L, "Film", "Description", LocalDate.of(2025,03,03),
                136, null, null, null);
        fc.createFilm(film);
        assertEquals(1L, fc.getAllFilms().size(), "Должен быть добавлен один фильм.");
    }
}